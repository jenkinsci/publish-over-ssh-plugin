/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over_ssh;

import com.jcraft.jsch.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.Util;
import jenkins.plugins.publish_over.BPBuildEnv;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPDefaultClient;
import jenkins.plugins.publish_over.BapPublisherException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

@SuppressWarnings("PMD.TooManyMethods")
public class BapSshClient extends BPDefaultClient<BapSshTransfer> {

    private static final transient Log LOG = LogFactory.getLog(BapSshClient.class);

    private final BPBuildInfo buildInfo;
    private final Stack<Session> sessions = new Stack<>();
    private final boolean disableExec;
    private ChannelSftp sftp;
    private final boolean avoidSameFileUpload;

    private BapSshTransferCache remoteResourceCache;

    public BapSshClient(final BPBuildInfo buildInfo, final Session session) {
        this(buildInfo, session, false, false);
    }

    public BapSshClient(final BPBuildInfo buildInfo, final Session session, final boolean disableExec, final boolean avoidSameFileUpload) {
        this.buildInfo = buildInfo;
        this.disableExec = disableExec;
        this.avoidSameFileUpload = avoidSameFileUpload;
        addSession(session);
    }

    /** Add a new session to the already known session chain (forwarding)
     * The new session will become the current session.
     * @param session new session to add
     */
    public void addSession(final Session session) {
        sessions.push(session);
    }

    public boolean isDisableExec() {
        return disableExec;
    }

    public boolean isAvoidSameFileUpload() {
      return avoidSameFileUpload;
    }

    public BPBuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void setSftp(final ChannelSftp sftp) {
        this.sftp = sftp;
    }

    public Session getSession() {
        return sessions.peek();
    }

    public void beginTransfers(final BapSshTransfer transfer) {
        if (disableExec) {
            if (!transfer.hasConfiguredSourceFiles())
                throw new BapPublisherException(Messages.exception_badTransferConfig_noExec());
        } else {
            if (!transfer.hasConfiguredSourceFiles() && !transfer.hasExecCommand())
                throw new BapPublisherException(Messages.exception_badTransferConfig());
        }

        BPBuildEnv buildEnv = getBuildInfo().getCurrentBuildEnv();
        String jobName = buildEnv.getEnvVars().get(BPBuildEnv.ENV_JOB_NAME);

        FilePath jobConfigPath = getBuildInfo().getConfigDir()
          .child("jobs")
          .child(jobName);

        if( isAvoidSameFileUpload() )
          this.remoteResourceCache = new BapSshTransferCache(jobConfigPath);
    }

    public boolean changeDirectory(final String directory) {
        try {
            if (!sftp.stat(directory).isDir()) return false;
        } catch (SftpException sftpe) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.log_sftp_stat(directory, sftpe.getLocalizedMessage()));
            }
            return false;
        }
        try {
            buildInfo.printIfVerbose(Messages.console_cd(directory));
            sftp.cd(directory);
            success();
            return true;
        } catch (SftpException sftpe) {
            throw new BapPublisherException(Messages.exception_cwdException(directory, sftpe.getLocalizedMessage()), sftpe);
        }
    }

    @Override
    public void deleteTree() throws SftpException {
        delete();
    }

    private void delete() throws SftpException {
        // List source directory structure.
        Vector<ChannelSftp.LsEntry> fileAndFolderList = sftp.ls(sftp.pwd());

        // Iterate objects in the list to get file/folder names.
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            delete(item);
        }
    }

    private void delete(ChannelSftp.LsEntry entry) throws SftpException {
        if (entry == null)
            throw new BapPublisherException(Messages.exception_client_entryIsNull());
        final String entryName = entry.getFilename();
        if (".".equals(entryName) || "..".equals(entryName))
            return;
        if (entry.getAttrs().isDir()) {
            if (!changeDirectory(entryName))
                throw new BapPublisherException(Messages.exception_cwdException(entryName, "Error occurred changing directory"));
            delete();
            if (!changeDirectory(".."))
                throw new BapPublisherException(Messages.exception_client_cdup());
            try {
                sftp.rmdir(entryName);
            } catch(SftpException e) {
                throw new BapPublisherException(Messages.exception_client_rmdir(entryName));
            }
        } else {
            try {
                sftp.rm(entryName);
            } catch (SftpException e) {
                throw new BapPublisherException(Messages.exception_client_dele(entryName));
            }
        }
    }

    public boolean makeDirectory(final String directory) {
        if (hasSubDirs(directory)) return false;
        try {
            buildInfo.printIfVerbose(Messages.console_mkdir(directory));
            sftp.mkdir(directory);
            success();
            return true;
        } catch (SftpException sftpe) {
            buildInfo.printIfVerbose(Messages.console_failure(sftpe.getLocalizedMessage()));
            return false;
        }
    }

    public void transferFile(final BapSshTransfer bapSshTransfer, final FilePath filePath,
                             final InputStream inputStream) throws SftpException, IOException, InterruptedException {
        final String fileName = filePath.getName();
        buildInfo.printIfVerbose(Messages.console_put(fileName));
        sftp.put(inputStream, fileName);

        if (bapSshTransfer.isKeepFilePermissions()) {
          final FilePath parentFle = filePath.getParent();
          if (parentFle != null) {
            final FilePath directory = parentFle.absolutize();
            final String remoteDir = sftp.pwd();
            int directoryMode = directory.mode();
            if (directoryMode >= 0) {
              buildInfo.printIfVerbose(Messages.console_chmod(Integer.toString(directoryMode, 8), fileName));
              sftp.chmod(directoryMode, remoteDir);
              success();
            }

            int fileMode = filePath.mode();
            if (fileMode >= 0) {
              buildInfo.printIfVerbose(Messages.console_chmod(Integer.toString(fileMode, 8), fileName));
              sftp.chmod(fileMode, fileName);
              success();
            }
          }
        }
    }

    private void success() {
        buildInfo.printIfVerbose(Messages.console_success());
    }

    private boolean hasSubDirs(final String directory) {
        return directory.contains("/") || directory.contains("\\");
    }

    public void endTransfers(final BapSshTransfer transfer) {
        if( isAvoidSameFileUpload() ) {
          remoteResourceCache.save();
          remoteResourceCache = null;
        }

        if (!disableExec && transfer.hasExecCommand()) {
            if (transfer.isUseSftpForExec())
                sftpExec(transfer);
            else
                exec(transfer);
        }
    }

    private void makeSymlink(final String oldPath, final String newPath) {
        try {
            buildInfo.printIfVerbose(Messages.sftpExec_symlink(oldPath, newPath));
            sftp.symlink(oldPath, newPath);
            success();
        } catch (SftpException sftpe) {
            buildInfo.println(Messages.console_failure(sftpe.getLocalizedMessage()));
        }
    }

    private void makeHardlink(final String oldPath, final String newPath) {
        try {
            buildInfo.printIfVerbose(Messages.sftpExec_hardlink(oldPath, newPath));
            sftp.hardlink(oldPath, newPath);
            success();
        } catch (SftpException sftpe) {
            buildInfo.println(Messages.console_failure(sftpe.getLocalizedMessage()));
        }
    }

    private void deleteDirectory(final String pathName) {
        try {
            buildInfo.printIfVerbose(Messages.sftpExec_deleteDirectory(pathName));
            sftp.rmdir(pathName);
            success();
        } catch (SftpException sftpe) {
            buildInfo.println(Messages.console_failure(sftpe.getLocalizedMessage()));
        }
    }

    private void deleteFile(final String pathName) {
        try {
            buildInfo.printIfVerbose(Messages.sftpExec_deleteFile(pathName));
            sftp.rm(pathName);
            success();
        } catch (SftpException sftpe) {
            buildInfo.println(Messages.console_failure(sftpe.getLocalizedMessage()));
        }
    }

    private void getDirContent(final List<String> commandArguments) {
        boolean withAttrs = false;
        Vector<ChannelSftp.LsEntry> fileAndDirectoryList;
        try {
            buildInfo.println(Messages.sftpExec_ls(String.join(" ", commandArguments)));
            String currentDir = sftp.pwd();
            if (!commandArguments.isEmpty()) {
                withAttrs = commandArguments.get(0).equals("-l");
                if (withAttrs)
                    commandArguments.remove(0);
                if (!commandArguments.isEmpty())
                    currentDir = commandArguments.get(0);
            }
            fileAndDirectoryList = sftp.ls(currentDir);
            if (fileAndDirectoryList.isEmpty())
                buildInfo.println(Messages.sftpExec_lsEmpty(currentDir));
            else
                showDirContent(sftp.ls(currentDir), withAttrs);
        } catch (SftpException sftpe) {
            buildInfo.println(Messages.sftpExec_failedCommand("ls"));
            buildInfo.println(Messages.console_failure(sftpe.getLocalizedMessage()));
        }
    }

    private void showDirContent(final Vector<ChannelSftp.LsEntry> entry, final boolean withAttrs) {
        for (ChannelSftp.LsEntry item : entry) {
            final String getName;
            if (!item.getFilename().equals(".") && !item.getFilename().equals("..")) {
                if (withAttrs)
                    getName = item.getLongname();
                else
                    getName = item.getFilename();
                buildInfo.println(Messages.sftpExec_display(getName));
            }
        }
        success();
    }

    private String parsePathName(String pathName, final boolean isDir) {
        if (pathName.contains("*") || pathName.contains("?")) {
            if (pathName.length() > 1 && pathName.contains("/"))
                pathName = pathName.substring(0, pathName.lastIndexOf('/') + 1);
            else
                pathName = "./";
        }
        if (isDir && pathName.charAt(pathName.length() - 1) != '/')
            pathName += "/";
        return pathName;
    }

    private String parsePathName(String pathName) {
        return parsePathName(pathName, true);
    }

    private boolean changeLocalBaseDirectory(final String newBaseDir) {
        try {
            buildInfo.printIfVerbose(Messages.sftpExec_lcd(newBaseDir));
            sftp.lcd(newBaseDir);
            success();
            return true;
        } catch (SftpException sftpe) {
            buildInfo.println(Messages.console_failure(sftpe.getLocalizedMessage()));
            return false;
        }
    }

    private void getFiles(final List<String> commandArguments) {
        String workspace = buildInfo.getBaseDirectory().getRemote();
        buildInfo.println(Messages.sftpExec_get(String.join(" ", commandArguments)));
        if (commandArguments.isEmpty() || commandArguments.size() == 1 && commandArguments.get(0).equals("-r")) {
            buildInfo.println(Messages.sftpExec_getArgumentsEmpty());
            return;
        }
        final boolean isRecursive = commandArguments.get(0).equals("-r");
        if (isRecursive)
            commandArguments.remove(0);
        if (commandArguments.size() > 1 && changeLocalBaseDirectory(workspace) &&
                changeLocalBaseDirectory(commandArguments.get(1))) {
                workspace = sftp.lpwd();
        }
        getFileList(commandArguments.get(0), workspace, isRecursive);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void getFileList(final String remotePathName, final String localBaseDir, final boolean isRecursive) {
        try {
            buildInfo.printIfVerbose(Messages.sftpExec_baseDir(localBaseDir));
            buildInfo.printIfVerbose(Messages.sftpExec_showRemotePath(remotePathName));
            Vector<ChannelSftp.LsEntry> fileAndDirectoryList = sftp.ls(remotePathName);
            if (fileAndDirectoryList.isEmpty()) {
                buildInfo.println(Messages.sftpExec_getEmpty(remotePathName));
                return;
            }
            for (ChannelSftp.LsEntry item : fileAndDirectoryList) {
                final String itemFileName = item.getFilename();
                if (!itemFileName.equals(".") && !itemFileName.equals("..")) {
                    if (!item.getAttrs().isDir())
                        getFile(itemFileName, parsePathName(remotePathName, false), localBaseDir);
                    else if (isRecursive) {
                        File newFile = new File(localBaseDir, itemFileName);
                        newFile.mkdir();
                        getFileList(parsePathName(remotePathName, true) + itemFileName + "/",
                                localBaseDir + "/" + itemFileName, true);
                    } else
                        buildInfo.printIfVerbose(Messages.sftpExec_isDirectory(itemFileName));
                }
            }
        } catch (SftpException | IOException  e) {
            buildInfo.println(Messages.sftpExec_failedCommand("get"));
            buildInfo.println(Messages.console_failure(e.getLocalizedMessage()));
        }
    }

    private void getFile(final String fileName, final String remotePathName, final String localBaseDir)
            throws SftpException, IOException {
        buildInfo.println(Messages.sftpExec_getFile(fileName));
        try(OutputStream outputStream =
                    new FileOutputStream(new File(localBaseDir, fileName))) {
            if (!fileName.equals(remotePathName.substring(remotePathName.lastIndexOf('/') + 1)))
                sftp.get(parsePathName(remotePathName) + fileName, outputStream);
            else
                sftp.get(remotePathName, outputStream);
            success();
        }
    }

    public String[] parseAllCommands(final BapSshTransfer transfer) {
        return Util.replaceMacro(transfer.getExecCommand(), buildInfo.getEnvVars()).split("\n\\s*");
    }

    public String[] parseCommand(final String command) {
        return command.trim().split("\\s+");
    }

    private void sftpExec(final BapSshTransfer transfer) {

        int maxSize;

        changeDirectory(getAbsoluteRemoteRoot());

        for (String commandToken:parseAllCommands(transfer)) {
            String[] command = parseCommand(commandToken);
            try {
                switch (command[0]) {
                    case "cd" :
                        changeDirectory(command[1]);
                        break;
                    case "symlink" :
                        makeSymlink(command[1], command[2]);
                        break;
                    case "mkdir" :
                        makeDirectory(command[1]);
                        break;
                    case "rm" :
                        deleteFile(command[1]);
                        break;
                    case "rmdir" :
                        deleteDirectory(command[1]);
                        break;
                    case "ln" :
                        if (command[1].equals("-s"))
                            makeSymlink(command[2], command[3]);
                        else
                            makeHardlink(command[1], command[2]);
                        break;
                    case "ls" :
                        maxSize = command.length > 3 ? 3 : command.length;
                        getDirContent(new ArrayList<>(Arrays.asList(command).subList(1, maxSize)));
                        break;
                    case "get" :
                        maxSize = command.length > 4 ? 4 : command.length;
                        getFiles(new ArrayList<>(Arrays.asList(command).subList(1, maxSize)));
                        break;
                    default :
                        buildInfo.println(Messages.sftpExec_unsupportedCommand(command[0]));
                        break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                buildInfo.println(Messages.sftpExec_tooFewArguments(command[0]));
            }
        }
    }

    private void exec(final BapSshTransfer transfer) {
        ChannelExec exec = null;
        try {
            exec = openExecChannel();
            exec.setPty(transfer.isUsePty());
            exec.setAgentForwarding(transfer.isUseAgentForwarding());
            exec.setInputStream(null);
            if(buildInfo.isVerbose()) {
                exec.setOutputStream(buildInfo.getListener().getLogger(), true);
                exec.setErrStream(buildInfo.getListener().getLogger(), true);
            }
            connectExecChannel(exec, Util.replaceMacro(transfer.getExecCommand(), buildInfo.getEnvVars()));
            waitForExec(exec, transfer.getExecTimeout());
            final int status = exec.getExitStatus();
            if (status != 0)
                throw new BapPublisherException(Messages.exception_exec_exitStatus(status));
        } finally {
            disconnectExecQuietly(exec);
        }
    }

    private void connectExecChannel(final ChannelExec exec, final String command) {
        exec.setCommand(command);
        buildInfo.printIfVerbose(Messages.console_exec_connecting(command));
        try {
            exec.connect(getSession().getTimeout());
        } catch (JSchException jse) {
            final String message = Messages.exception_exec_connect(jse.getLocalizedMessage());
            LOG.warn(message, jse);
            throw new BapPublisherException(message); // NOPMD - it's in the log!
        }
        buildInfo.printIfVerbose(Messages.console_exec_connected());
    }

    private ChannelExec openExecChannel() {
        buildInfo.printIfVerbose(Messages.console_exec_opening());
        try {
            final ChannelExec exec = (ChannelExec) getSession().openChannel("exec");
            buildInfo.printIfVerbose(Messages.console_exec_opened());
            return exec;
        } catch (JSchException jse) {
            final String message = Messages.exception_exec_open(jse.getLocalizedMessage());
            LOG.warn(message, jse);
            throw new BapPublisherException(message); // NOPMD - it's in the log!
        }
    }

    public void disconnectExecQuietly(final ChannelExec exec) {
        try {
            disconnectExec(exec);
        } catch (Exception e) {
            LOG.warn(Messages.exception_disconnect_exec(e.getLocalizedMessage()));
        }
    }

    private void disconnectExec(final ChannelExec exec) {
        if (exec == null) return;
        if (exec.isConnected())
            exec.disconnect();
    }

    public void disconnect() {
        disconnectSftp();
        disconnectSession();
    }

    private void disconnectSftp() {
        if (sftp == null) return;
        if (sftp.isConnected())
            sftp.disconnect();
    }

    private void disconnectSession() {
        while (!sessions.empty()) {
            Session session = sessions.pop();
            if (session.isConnected())
                session.disconnect();
        }
    }

    public void disconnectQuietly() {
        try {
            disconnectSftp();
        } catch (Exception e) {
            LOG.warn(Messages.exception_disconnect_sftp(e.getLocalizedMessage()));
        }
        try {
            disconnectSession();
        } catch (Exception e) {
            LOG.warn(Messages.exception_disconnect_session(e.getLocalizedMessage()));
        }
    }

    private void waitForExec(final ChannelExec exec, final long timeout) {
        final long start = System.currentTimeMillis();
        final Thread waiter = new ExecCheckThread(exec);
        waiter.start();
        try {
            waiter.join(timeout);
        } catch (InterruptedException ie) {
            waiter.interrupt();
            LOG.warn(ie.getMessage(), ie);
        }
        final long duration = System.currentTimeMillis() - start;
        if (waiter.isAlive()) {
            waiter.interrupt();
        }
        if (!exec.isClosed())
            throw new BapPublisherException(Messages.exception_exec_timeout(duration));
        buildInfo.println(Messages.console_exec_completed(duration));
    }

    private static final class ExecCheckThread extends Thread {
        private static final int POLL_TIME = 200;
        private final ChannelExec exec;

        ExecCheckThread(final ChannelExec exec) {
            this.exec = exec;
        }
        @Override
        public void run() {
            try {
                while (!exec.isClosed()) {
                    Thread.sleep(POLL_TIME);
                }
            } catch (InterruptedException ie) {
                interrupt();
                LOG.warn(ie.getMessage(), ie);
            }
        }
    }

}
