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
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisherException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Properties;

public class BapSshHostConfiguration extends BPHostConfiguration<BapSshClient> {
    
    static final long serialVersionUID = 1L;
    
    public static final int DEFAULT_PORT = 22;
    public static final int DEFAULT_TIMEOUT = 300000;
    private int timeout;

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }
    public static int getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }
    
    @DataBoundConstructor
	public BapSshHostConfiguration(String name, String hostname, String username, String password, String remoteRootDir, int port, int timeout) {
        super(name, hostname, username, password, remoteRootDir, port);
        this.timeout = timeout;
    }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    @Override
    public BapSshClient createClient(BPBuildInfo buildInfo) {
        JSch ssh = createJSch();
        Session session = createSession(buildInfo, ssh);
        BapSshClient bapClient = new BapSshClient(ssh, session);
        try {
            session.setUserInfo(new BapSshUserInfo(buildInfo, getPassword()));
            session.setConfig(getSessionProperties());
            connect(buildInfo, session);
            ChannelSftp sftp = openSftpChannel(buildInfo, session);
            bapClient.setSftp(sftp);
            connectSftpChannel(buildInfo, sftp);
    //        TODO the exec
    //        ChannelExec exec;
            return bapClient;
        } catch (RuntimeException re) {
            bapClient.disconnectQuietly();
            throw re;
        }
    }
    
    private void connectSftpChannel(BPBuildInfo buildInfo, ChannelSftp channel) {
        buildInfo.printIfVerbose(Messages.console_sftp_connecting());
        try {
            channel.connect(getTimeout());
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_sftp_connect(jse.getLocalizedMessage()));
        }
        buildInfo.printIfVerbose(Messages.console_sftp_connected());
        
    }
    
    private ChannelSftp openSftpChannel(BPBuildInfo buildInfo, Session session) {
        buildInfo.printIfVerbose(Messages.console_sftp_opening());
        ChannelSftp sftp;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_sftp_open(jse.getLocalizedMessage()));
        }
        buildInfo.printIfVerbose(Messages.console_sftp_opened());
        return sftp;
    }
    
    private Properties getSessionProperties() {
        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no");
        return props;
    }
    
    private void connect(BPBuildInfo buildInfo, Session session) {
        buildInfo.printIfVerbose(Messages.console_session_connecting());
        try {
            session.connect(getTimeout());
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_session_connect(getName(), jse.getLocalizedMessage()));
        }
        buildInfo.printIfVerbose(Messages.console_session_connected());
    }

    private Session createSession(BPBuildInfo buildInfo, JSch ssh) {
        try {
            buildInfo.printIfVerbose(Messages.console_session_creating(getUsername(), getHostname(), getPort()));
            return ssh.getSession(getUsername(), getHostname(), getPort());
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_session_create(getUsername(), getHostname(), getPort(), jse.getLocalizedMessage()), jse);
        }
    }

    protected JSch createJSch() {
        return new JSch();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BapSshHostConfiguration that = (BapSshHostConfiguration) o;
        
        return createEqualsBuilder(that)
            .append(timeout, that.timeout)
            .isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder()
            .append(timeout)
            .toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE))
            .append("timeout", timeout)
            .toString();
    }
    
    public static class BapSshUserInfo implements UserInfo {

        private final BPBuildInfo buildInfo;
        private final String password;

        public BapSshUserInfo(BPBuildInfo buildInfo, String password) {
            this.buildInfo = buildInfo;
            this.password = password;
        }

        public String getPassphrase() {
            return password;
        }
        
        public String getPassword() {
            return password;
        }
        
        public boolean promptPassword(String s) {
            return printAndReturn(s, true);
        }
        
        public boolean promptPassphrase(String s) {
            return printAndReturn(s, true);
        }
        
        public boolean promptYesNo(String s) {
            return printAndReturn(s, true);
        }
        
        private boolean printAndReturn(String message, boolean returning) {
            buildInfo.printIfVerbose(message);
            buildInfo.printIfVerbose(Messages.console_userInfo_returning(returning));
            return returning;
        }
        
        public void showMessage(String s) {
            buildInfo.println(s);
        }
    }
    
}
