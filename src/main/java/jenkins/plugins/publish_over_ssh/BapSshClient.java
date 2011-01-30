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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import hudson.FilePath;
import jenkins.plugins.publish_over.BPDefaultClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

public class BapSshClient extends BPDefaultClient<BapSshTransfer> {
    
    private static Log LOG = LogFactory.getLog(BapSshClient.class);
    
    private JSch ssh;
    private Session session;
    private ChannelSftp sftp;

    public BapSshClient(JSch ssh, Session session) {
        this.ssh = ssh;
        this.session = session;
    }
    
    public void setSftp(ChannelSftp sftp) {
        this.sftp = sftp;
    }

    public boolean changeToInitialDirectory() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean changeDirectory(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean makeDirectory(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void transferFile(BapSshTransfer bapSshTransfer, FilePath filePath, InputStream inputStream) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
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
}
