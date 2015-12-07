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

import hudson.Util;
import hudson.model.Describable;
import hudson.model.Hudson;
import java.io.IOException;
import java.util.Properties;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisher;
import jenkins.plugins.publish_over.BapPublisherException;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshHostConfigurationDescriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@SuppressWarnings("PMD.TooManyMethods")
public class BapSshHostConfiguration extends BPHostConfiguration<BapSshClient, BapSshCommonConfiguration>
                                                                                        implements Describable<BapSshHostConfiguration> {

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PORT = 22;
    public static final int DEFAULT_TIMEOUT = 300000;
    public static final String CONFIG_KEY_PREFERRED_AUTHENTICATIONS = "PreferredAuthentications";
    private static final Log LOG = LogFactory.getLog(BapSshHostConfiguration.class);

    private int timeout;
    private boolean overrideKey;
    private boolean disableExec;
    private final BapSshKeyInfo keyInfo;

    // CSOFF: ParameterNumberCheck
    @SuppressWarnings("PMD.ExcessiveParameterList") // DBC for you!
    @DataBoundConstructor
    public BapSshHostConfiguration(final String name, final String hostname, final String username, final String encryptedPassword,
                                   final String remoteRootDir, final int port, final int timeout, final boolean overrideKey,
                                   final String keyPath, final String key, final boolean disableExec) {
        // CSON: ParameterNumberCheck
        super(name, hostname, username, null, remoteRootDir, port);
        this.timeout = timeout;
        this.overrideKey = overrideKey;
        keyInfo = new BapSshKeyInfo(encryptedPassword, key, keyPath);
        this.disableExec = disableExec;
    }

    public int getTimeout() { return timeout; }
    public void setTimeout(final int timeout) { this.timeout = timeout; }

    @Override
    protected final String getPassword() { return keyInfo.getPassphrase(); }
    @Override
    public final void setPassword(final String password) { keyInfo.setPassphrase(password); }

    @Override
    public final String getEncryptedPassword() { return keyInfo.getEncryptedPassphrase(); }

    public String getKeyPath() { return keyInfo.getKeyPath(); }
    public void setKeyPath(final String keyPath) { keyInfo.setKeyPath(keyPath); }

    public String getKey() { return keyInfo.getKey(); }
    public void setKey(final String key) { keyInfo.setKey(key); }

    public boolean isOverrideKey() { return overrideKey; }
    public void setOverrideKey(final boolean overrideKey) { this.overrideKey = overrideKey; }

    public boolean isDisableExec() { return disableExec; }
    public void setDisableExec(final boolean disableExec) { this.disableExec = disableExec; }

    public boolean isEffectiveDisableExec() {
        return getCommonConfig().isDisableAllExec() || disableExec;
    }

    private BapSshKeyInfo getEffectiveKeyInfo(final BPBuildInfo buildInfo) {
        final BapSshCredentials publisherCredentials = getPublisherOverrideCredentials(buildInfo);
        if (publisherCredentials != null) return publisherCredentials;
        return overrideKey ? keyInfo : getCommonConfig();
    }

    @Override
    public BapSshClient createClient(final BPBuildInfo buildInfo, final BapPublisher publisher) {
        return createClient(buildInfo, ((BapSshPublisher) publisher).isSftpRequired());
    }

    @Override
    public BapSshClient createClient(final BPBuildInfo buildInfo) {
        return createClient(buildInfo, true);
    }

    public BapSshClient createClient(final BPBuildInfo buildInfo, final boolean connectSftp) {
        final JSch ssh = createJSch();
        final Session session = createSession(buildInfo, ssh);
        final BapSshClient bapClient = new BapSshClient(buildInfo, session, isEffectiveDisableExec());
        try {
            final BapSshKeyInfo keyInfo = getEffectiveKeyInfo(buildInfo);
            final Properties sessionProperties = getSessionProperties();
            if (keyInfo.useKey()) {
                setKey(buildInfo, ssh, keyInfo);
                sessionProperties.put(CONFIG_KEY_PREFERRED_AUTHENTICATIONS, "publickey");
            } else {
                session.setPassword(getPassphrase(keyInfo,buildInfo));
                sessionProperties.put(CONFIG_KEY_PREFERRED_AUTHENTICATIONS, "keyboard-interactive,password");
            }
            session.setConfig(sessionProperties);
            connect(buildInfo, session);
            if (connectSftp) setupSftp(buildInfo, bapClient);
            return bapClient;
        } catch (IOException ioe) {
            bapClient.disconnectQuietly();
            throw new BapPublisherException(Messages.exception_failedToCreateClient(ioe.getLocalizedMessage()), ioe);
        } catch (RuntimeException re) {
            bapClient.disconnectQuietly();
            throw re;
        }
    }

    private static String getPassphrase(final BapSshKeyInfo keyInfo, final BPBuildInfo buildInfo) {
        String passphrase = Util.fixNull(keyInfo.getPassphrase());
        if ( keyInfo instanceof BapSshCredentials && ((BapSshCredentials)keyInfo).getInjectCredentials() ) {
            passphrase = getEnvironmentVariable(passphrase,buildInfo);
        }
        return passphrase;
    }

    private void setupSftp(final BPBuildInfo buildInfo, final BapSshClient bapClient) throws IOException {
        final ChannelSftp sftp = openSftpChannel(buildInfo, bapClient.getSession());
        bapClient.setSftp(sftp);
        connectSftpChannel(buildInfo, sftp);
        changeToRootDirectory(bapClient);
        setRootDirectoryInClient(bapClient, sftp);
    }

    private void setKey(final BPBuildInfo buildInfo, final JSch ssh, final BapSshKeyInfo keyInfo) {
        try {
            ssh.addIdentity("TheKey", keyInfo.getEffectiveKey(buildInfo), null, BapSshUtil.toBytes(keyInfo.getPassphrase()));
        } catch (JSchException jsche) {
            throw new BapPublisherException(Messages.exception_addIdentity(jsche.getLocalizedMessage()), jsche);
        }
    }

    private void setRootDirectoryInClient(final BapSshClient client, final ChannelSftp sftp) throws IOException {
        if (isDirectoryAbsolute(getRemoteRootDir())) {
            client.setAbsoluteRemoteRoot(getRemoteRootDir());
        } else {
            client.setAbsoluteRemoteRoot(getRootDirectoryFromPwd(client, sftp));
        }
    }

    private String getRootDirectoryFromPwd(final BapSshClient client, final ChannelSftp sftp) {
        final BPBuildInfo buildInfo = client.getBuildInfo();
        buildInfo.printIfVerbose(Messages.console_usingPwd());
        try {
            final String pwd = sftp.pwd();
            if (!isDirectoryAbsolute(pwd))
                throw new BapPublisherException(Messages.exception_pwdNotAbsolute(pwd));
            return pwd;
        } catch (SftpException sftpe) {
            final String message = Messages.exception_pwd(sftpe.getLocalizedMessage());
            LOG.warn(message, sftpe);
            throw new BapPublisherException(message); // NOPMD - it's in the log!
        }
    }

    private void connectSftpChannel(final BPBuildInfo buildInfo, final ChannelSftp channel) {
        buildInfo.printIfVerbose(Messages.console_sftp_connecting());
        try {
            channel.connect(getTimeout());
        } catch (JSchException jse) {
            final String message = Messages.exception_sftp_connect(jse.getLocalizedMessage());
            LOG.warn(message, jse);
            throw new BapSshSftpSetupException(message); // NOPMD - it's in the log!
        }
        buildInfo.printIfVerbose(Messages.console_sftp_connected());
    }

    private ChannelSftp openSftpChannel(final BPBuildInfo buildInfo, final Session session) {
        buildInfo.printIfVerbose(Messages.console_sftp_opening());
        final ChannelSftp sftp;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
        } catch (JSchException jse) {
            final String message = Messages.exception_sftp_open(jse.getLocalizedMessage());
            LOG.warn(message, jse);
            throw new BapSshSftpSetupException(message); // NOPMD - it's in the log!
        }
        buildInfo.printIfVerbose(Messages.console_sftp_opened());
        return sftp;
    }

    private Properties getSessionProperties() {
        final Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no");
        return props;
    }

    private void connect(final BPBuildInfo buildInfo, final Session session) {
        buildInfo.printIfVerbose(Messages.console_session_connecting());
        try {
            session.connect(getTimeout());
        } catch (JSchException jse) {
            final String message = Messages.exception_session_connect(getName(), jse.getLocalizedMessage());
            LOG.warn(message, jse);
            throw new BapPublisherException(message); // NOPMD - it's in the log!
        }
        buildInfo.printIfVerbose(Messages.console_session_connected());
    }

    private Session createSession(final BPBuildInfo buildInfo, final JSch ssh) {
        final BapSshCredentials overrideCreds = getPublisherOverrideCredentials(buildInfo);
        final String username = overrideCreds == null ? getUsername() : getOverrideCredsUsername(overrideCreds, buildInfo);
        try {
            buildInfo.printIfVerbose(Messages.console_session_creating(username, getHostnameTrimmed(), getPort()));
            return ssh.getSession(username, getHostnameTrimmed(), getPort());
        } catch (JSchException jse) {
            throw new BapPublisherException(Messages.exception_session_create(
                    username, getHostnameTrimmed(), getPort(), jse.getLocalizedMessage()), jse);
        }
    }

    private static String getOverrideCredsUsername(final BapSshCredentials overrideCreds, final BPBuildInfo buildInfo) {
        if ( overrideCreds.getInjectCredentials() ) {
            return getEnvironmentVariable(overrideCreds.getUsername(), buildInfo);
        }
        return overrideCreds.getUsername();
    }

    private static String getEnvironmentVariable(String var, final BPBuildInfo buildInfo) {
        return buildInfo.getEnvVars().get(var);
    }

    private static BapSshCredentials getPublisherOverrideCredentials(final BPBuildInfo buildInfo) {
        return (BapSshCredentials) buildInfo.get(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY);
    }

    protected JSch createJSch() {
        return new JSch();
    }

    public BapSshHostConfigurationDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BapSshHostConfigurationDescriptor.class);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapSshHostConfiguration that) {
        return super.addToEquals(builder, that)
            .append(keyInfo, that.keyInfo)
            .append(timeout, that.timeout)
            .append(overrideKey, that.overrideKey)
            .append(disableExec, that.disableExec);
    }

    @Override
    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return super.addToHashCode(builder)
            .append(keyInfo)
            .append(timeout)
            .append(overrideKey)
            .append(disableExec);
    }

    @Override
    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return super.addToToString(builder)
            .append("keyInfo", keyInfo)
            .append("timeout", timeout)
            .append("overrideKey", overrideKey)
            .append("disableExec", disableExec);
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        final BapSshHostConfiguration thatHostConfiguration = (BapSshHostConfiguration) that;

        return addToEquals(new EqualsBuilder(), thatHostConfiguration).isEquals();
    }

    @Override
    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    @Override
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    @Override
    public Object readResolve() {
        return super.readResolve();
    }

}
