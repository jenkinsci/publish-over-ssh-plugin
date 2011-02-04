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
import com.jcraft.jsch.SftpException;
import hudson.Util;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisherException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Properties;

public class BapSshHostConfiguration extends BPHostConfiguration<BapSshClient, BapSshCommonConfiguration> {
    
    static final long serialVersionUID = 1L;
    
    public static final int DEFAULT_PORT = 22;
    public static final int DEFAULT_TIMEOUT = 300000;
    private int timeout;
    private boolean overrideKey;
    private BapSshConcreteKeyInfo keyInfo= new BapSshConcreteKeyInfo();

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }
    public static int getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }
    
    @DataBoundConstructor
	public BapSshHostConfiguration(String name, String hostname, String username, String password, String remoteRootDir, int port, int timeout, boolean overrideKey, String keyPath, String key) {
        super(name, hostname, username, null, remoteRootDir, port);
        this.timeout = timeout;
        this.overrideKey = overrideKey;
        setPassword(password);
        setKey(key);
        setKeyPath(keyPath);
    }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public String getPassword() { return keyInfo.getPassphrase(); }
    public void setPassword(String password) { keyInfo.setPassphrase(password); }
    
    public String getKeyPath() {return keyInfo.getKeyPath(); }
    public void setKeyPath(String keyPath) { keyInfo.setKeyPath(keyPath); }

    public String getKey() { return keyInfo.getKey(); }
    public void setKey(String key) { keyInfo.setKey(key); }

    public boolean isOverrideKey() { return overrideKey; }
    public void setOverrideKey(boolean overrideKey) { this.overrideKey = overrideKey; }

    BapSshKeyInfo getEffectiveKeyInfo() {
        return overrideKey ? keyInfo : getCommonConfig();
    }

    @Override
    public BapSshClient createClient(BPBuildInfo buildInfo) {
        JSch ssh = createJSch();
        Session session = createSession(buildInfo, ssh);
        BapSshClient bapClient = new BapSshClient(buildInfo, ssh, session);
        try {
            BapSshKeyInfo keyInfo = getEffectiveKeyInfo();
            Properties sessionProperties = getSessionProperties();
            if (getEffectiveKeyInfo().useKey()) {
                setKey(buildInfo, ssh, keyInfo);
                sessionProperties.put("PreferredAuthentications", "publickey");
            } else {
                session.setPassword(Util.fixNull(keyInfo.getPassphrase()));
            }        
            session.setConfig(sessionProperties);
            connect(buildInfo, session);
            ChannelSftp sftp = openSftpChannel(buildInfo, session);
            bapClient.setSftp(sftp);
            connectSftpChannel(buildInfo, sftp);
            changeToRootDirectory(bapClient);
            setRootDirectoryInClient(bapClient, sftp);            
            return bapClient;
        } catch (IOException ioe) {
            bapClient.disconnectQuietly();
            throw new BapPublisherException(Messages.exception_failedToCreateClient(ioe.getLocalizedMessage()), ioe);
        } catch (RuntimeException re) {
            bapClient.disconnectQuietly();
            throw re;
        }
    }
    
    private void setKey(BPBuildInfo buildInfo, JSch ssh, BapSshKeyInfo keyInfo) {
        try {
            ssh.addIdentity("TheKey", keyInfo.getEffectiveKey(buildInfo), null, BapSshUtil.toBytes(keyInfo.getPassphrase()));
        } catch (JSchException jsche) {
            throw new BapPublisherException(Messages.exception_addIdentity(jsche.getLocalizedMessage()), jsche);
        }
    }
    
    private void setRootDirectoryInClient(BapSshClient client, ChannelSftp sftp) throws IOException {
        if (isDirectoryAbsolute(getRemoteRootDir())) {
            client.setAbsoluteRemoteRoot(getRemoteRootDir());
        } else {
            client.setAbsoluteRemoteRoot(getRootDirectoryFromPwd(client, sftp));
        }
    }
    
    private String getRootDirectoryFromPwd(BapSshClient client, ChannelSftp sftp) {
        BPBuildInfo buildInfo = client.getBuildInfo();
        buildInfo.printIfVerbose(Messages.console_usingPwd());
        try {
            String pwd = sftp.pwd();
            if (!isDirectoryAbsolute(pwd))
                throw new BapPublisherException(Messages.exception_pwdNotAbsolute(pwd));
            return pwd;
        } catch (SftpException sftpe) {
            throw new BapPublisherException(Messages.exception_pwd(sftpe.getLocalizedMessage()));
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
            .append(keyInfo, that.keyInfo)
            .append(timeout, that.timeout)
            .append(overrideKey, that.overrideKey)
            .isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder()
            .append(keyInfo)
            .append(timeout)
            .append(overrideKey)
            .toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE))
            .append("keyInfo", keyInfo)
            .append("timeout", timeout)
            .append("overrideKey", overrideKey)
            .toString();
    }
    
}
