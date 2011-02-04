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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import hudson.Util;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPDefaultClient;
import jenkins.plugins.publish_over.BapPublisherException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

public class BapSshClient extends BPDefaultClient<BapSshTransfer> {
    
    private static Log LOG = LogFactory.getLog(BapSshClient.class);
    
    private BPBuildInfo buildInfo;
    private JSch ssh;
    private Session session;
    private ChannelSftp sftp;

    public BapSshClient(BPBuildInfo buildInfo, JSch ssh, Session session) {
        this.buildInfo = buildInfo;
        this.ssh = ssh;
        this.session = session;
    }
    
    public BPBuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void setSftp(ChannelSftp sftp) {
        this.sftp = sftp;
    }

    public void beginTransfers(BapSshTransfer transfer) {
        if (!transfer.hasConfiguredSourceFiles() && !transfer.hasExecCommand())
            throw new BapPublisherException(Messages.exception_badTransferConfig());
    }

    public boolean changeDirectory(String directory) {    
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

    public boolean makeDirectory(String directory) {
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

    public void transferFile(BapSshTransfer bapSshTransfer, FilePath filePath, InputStream inputStream) throws SftpException {
        buildInfo.printIfVerbose(Messages.console_put(filePath.getName()));
        sftp.put(inputStream, filePath.getName());
        success();
    }
    
    private void success() {
        buildInfo.printIfVerbose(Messages.console_success());
    }
    
    private boolean hasSubDirs(String directory) {
        return directory.contains("/") || directory.contains("\\");
    }

    public void endTransfers(BapSshTransfer transfer) {
        if (transfer.hasExecCommand())
            exec(transfer);
    }
    
    private void exec(BapSshTransfer transfer) {
        ChannelExec exec = null;
        try {
            exec = openExecChannel();
            exec.setInputStream(null);
            exec.setOutputStream(buildInfo.getListener().getLogger(), true);
            exec.setErrStream(buildInfo.getListener().getLogger(), true);
            connectExecChannel(exec, Util.replaceMacro(transfer.getExecCommand(), buildInfo.getEnvVars()));
            waitForExec(exec, transfer.getExecTimeout());
            int status = exec.getExitStatus();
            if (status != 0)
                throw new BapPublisherException(Messages.exception_exec_exitStatus(status));
        } finally {
            disconnectExecQuietly(exec);
        }
    }
    
    private void connectExecChannel(ChannelExec exec, String command) {
        exec.setCommand(command);
        buildInfo.printIfVerbose(Messages.console_exec_connecting(command));
        try {
            exec.connect(session.getTimeout());
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_exec_connect(jse.getLocalizedMessage()));
        }
        buildInfo.printIfVerbose(Messages.console_exec_connected());
        
    }
    
    private ChannelExec openExecChannel() {
        buildInfo.printIfVerbose(Messages.console_exec_opening());
        try {
            ChannelExec exec = (ChannelExec) session.openChannel("exec");
            buildInfo.printIfVerbose(Messages.console_exec_opened());
            return exec;
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_exec_open(jse.getLocalizedMessage()));
        }
    }

    public void disconnectExecQuietly(ChannelExec exec) {
        try {
            disconnectExec(exec);
        } catch (Exception e) {
            LOG.warn(Messages.exception_disconnect_exec(e.getLocalizedMessage()));
        }
    }
    
    private void disconnectExec(ChannelExec exec) {
        if (exec == null) return; 
        if (exec.isConnected())
            exec.disconnect();
    }
    
    public void disconnect() throws Exception {
        disconnectSftp();
        disconnectSession();
    }
    
    private void disconnectSftp() {
        if (sftp == null) return;
        if (sftp.isConnected())
            sftp.disconnect();
    }
    
    private void disconnectSession() {
        if (session == null) return;
        if (session.isConnected())
                session.disconnect();
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
    
    private void waitForExec(final ChannelExec exec, long timeout) {
        long start = System.currentTimeMillis();
        Thread waiter = new Thread() { public void run() {
                try {
                    while (!exec.isClosed()) {
                        Thread.sleep(200);
                    }
                } catch (InterruptedException ie) { }
        }};
        waiter.start();
        try {
            waiter.join(timeout);
        } catch (InterruptedException ie) { }
        long duration = System.currentTimeMillis() - start;
        if (waiter.isAlive()) {
            waiter.interrupt();
        }
        if (!exec.isClosed())
            throw new BapPublisherException(Messages.exception_exec_timeout(duration));
        buildInfo.printIfVerbose(Messages.console_exec_completed(duration));
    }
    
}
