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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.model.Describable;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisher;
import jenkins.plugins.publish_over.BapPublisherException;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshHostConfigurationDescriptor;

@SuppressWarnings("PMD.TooManyMethods")
public class BapSshHostConfiguration extends BPHostConfiguration<BapSshClient, BapSshCommonConfiguration>
		implements Describable<BapSshHostConfiguration> {

	public static final String CONFIG_KEY_PREFERRED_AUTHENTICATIONS = "PreferredAuthentications";
	private static final Log LOG = LogFactory.getLog(BapSshHostConfiguration.class);

	static final String LOCALHOST = "127.0.0.1";
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_PORT = 22;
	public static final int DEFAULT_TIMEOUT = 300000;

	public static final String DEFAULT_JUMP_HOST = "";
	public static final String HTTP_PROXY_TYPE = "http";
	public static final String SOCKS_4_PROXY_TYPE = "socks4";
	public static final String SOCKS_5_PROXY_TYPE = "socks5";

	private int timeout;
	private boolean overrideCredentials;
	private boolean disableExec;

	private String hostCredentialsId;
	private String jumpHost;

	private String proxyType;
	private String proxyHost;
	private int proxyPort;
	private String proxyCredentialsId;

	public BapSshHostConfiguration() {
		// use this constructor instead of the default w/o parameters because there is
		// some business logic in there...
		super(null, null, null, null, null, 0);
		this.hostCredentialsId = null;
	}

	// CSOFF: ParameterNumberCheck
	@SuppressWarnings("PMD.ExcessiveParameterList") // DBC for you!
	@DataBoundConstructor
	public BapSshHostConfiguration(final String name, final String hostname, final String hostCredentialsId,
			final String remoteRootDir, final int port, final int timeout, final boolean overrideCredentials,
			final boolean disableExec) {
		// TODO: SWA, username is empty

		// CSON: ParameterNumberCheck
		super(name, hostname, "", null, remoteRootDir, port);
		this.timeout = timeout;
		this.overrideCredentials = overrideCredentials;
		this.hostCredentialsId = hostCredentialsId;
		this.disableExec = disableExec;
	}

	@DataBoundSetter
	public void setJumpHost(final String jumpHost) {
		this.jumpHost = jumpHost;
	}

	public String getJumpHost() {
		return jumpHost;
	}

	@DataBoundSetter
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@DataBoundSetter
	@Override
	public void setHostname(String hostname) {
		super.setHostname(hostname);
	}

	@DataBoundSetter
	@Override
	public void setRemoteRootDir(String remoteRootDir) {
		super.setRemoteRootDir(remoteRootDir);
	}

	@DataBoundSetter
	@Override
	public void setPort(int port) {
		super.setPort(port);
	}

	public int getTimeout() {
		return timeout;
	}

	@DataBoundSetter
	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	public boolean isOverrideKey() {
		return overrideCredentials;
	}

	@DataBoundSetter
	public void setOverrideKey(final boolean overrideKey) {
		this.overrideCredentials = overrideKey;
	}

	public boolean isDisableExec() {
		return disableExec;
	}

	@DataBoundSetter
	public void setDisableExec(final boolean disableExec) {
		this.disableExec = disableExec;
	}

	public String getProxyType() {
		return proxyType;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public String getProxyCredentialsId() {
		return proxyCredentialsId;
	}

	@DataBoundSetter
	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}

	@DataBoundSetter
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	@DataBoundSetter
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@DataBoundSetter
	public void setProxyCredentialsId(String proxyCredentialsId) {
		this.proxyCredentialsId = proxyCredentialsId;
	}

	public String getHostCredentialsId() {
		return hostCredentialsId;
	}

	@DataBoundSetter
	public void setHostCredentialsId(String hostCredentialsId) {
		this.hostCredentialsId = hostCredentialsId;
	}

	public boolean isEffectiveDisableExec() {
		return getCommonConfig().isDisableAllExec() || disableExec;
	}

	private String getEffectiveCredentialsId(final BPBuildInfo buildInfo) {
		if (buildInfo != null) {
			final String publisherCredentials = getPublisherOverrideCredentials(buildInfo);
			if (StringUtils.isNotEmpty(publisherCredentials)) {
				// this is configured in a specific job
				LOG.info("use overriden credentials: " + publisherCredentials);
				return publisherCredentials;
			}
		}

		String retVal = null;
		if (overrideCredentials) {
			retVal = hostCredentialsId;
			LOG.info("use host specific credentials: " + hostCredentialsId);
		} else {
			retVal = getCommonConfig().getGeneralCredentialsId();
			LOG.info("use common specific credentials: " + retVal + ", host creds are set: " + hostCredentialsId);
		}

		return retVal;
	}

	private UsernamePasswordCredentials getEffectiveCredentials(@Nullable final BPBuildInfo buildInfo) {
		final String effectiveCredentials = getEffectiveCredentialsId(buildInfo);
		if (StringUtils.isEmpty(effectiveCredentials)) {
			// this is configured in a specific job
			return null;
		}

		return this.getCredentialsUserPasswordOrNull(effectiveCredentials);
	}

	@Override
	public BapSshClient createClient(final BPBuildInfo buildInfo, final BapPublisher publisher) {
		if (publisher instanceof BapSshPublisher) {
			return createClient(buildInfo, ((BapSshPublisher) publisher).isSftpRequired());
		}
		throw new IllegalArgumentException("Invalid type passed to createClient");
	}

	@Override
	public BapSshClient createClient(final BPBuildInfo buildInfo) {
		return createClient(buildInfo, true);
	}

	public BapSshClient createClient(final BPBuildInfo buildInfo, final boolean connectSftp) {

		final JSch ssh = createJSch();
		String[] hosts = getHosts();
		Session session = createSession(buildInfo, ssh, hosts[0], getPort());
		configureAuthentication(buildInfo, ssh, session);
		final BapSshClient bapClient = new BapSshClient(buildInfo, session, isEffectiveDisableExec());
		try {
			connect(buildInfo, session);
			for (int i = 1; i < hosts.length; i++) {
				int assignedPort = session.setPortForwardingL(0, hosts[i], getPort());
				session = createSession(buildInfo, ssh, LOCALHOST, assignedPort);
				bapClient.addSession(session);
				configureAuthentication(buildInfo, ssh, session);
				connect(buildInfo, session);
			}
			if (connectSftp)
				setupSftp(bapClient);
		} catch (IOException e) {
			bapClient.disconnectQuietly();
			throw new BapPublisherException(Messages.exception_failedToCreateClient(e.getLocalizedMessage()), e);
		} catch (JSchException e) {
			bapClient.disconnectQuietly();
			throw new BapPublisherException(Messages.exception_failedToCreateClient(e.getLocalizedMessage()), e);
		} catch (BapPublisherException e) {
			bapClient.disconnectQuietly();
			throw new BapPublisherException(Messages.exception_failedToCreateClient(e.getLocalizedMessage()), e);
		}
		return bapClient;
	}

	/**
	 * create a list of hosts from the explicit stated target host and an optional
	 * list of jumphosts
	 * 
	 * @return list of hosts
	 */
	String[] getHosts() {
		return HostsHelper.getHosts(getHostnameTrimmed(), jumpHost);
	}

	static class HostsHelper {
		static String[] getHosts(String target, String jumpHosts) {
			ArrayList<String> hosts = new ArrayList<>();
			if (jumpHosts != null) {
				String[] jumpHostsList = jumpHosts.split("[ ;,]");
				for (String host : jumpHostsList) {
					if (StringUtils.isNotBlank(host))
						hosts.add(host);
				}
			}
			hosts.add(target);
			return hosts.toArray(new String[hosts.size()]);
		}
	}

	private void configureAuthentication(final BPBuildInfo buildInfo, final JSch ssh, Session session) {
		final UsernamePasswordCredentials credentials = getEffectiveCredentials(buildInfo);

		if (credentials == null) {
			System.out.println("General Credentials: " + this.getCommonConfig().getGeneralCredentialsId());
			System.out.println("Host Credentials: " + this.hostCredentialsId);
			throw new IllegalArgumentException("failed to detect credentials");
		}

		final Properties sessionProperties = getSessionProperties();
		session.setPassword(credentials.getPassword().getPlainText());
		sessionProperties.put(CONFIG_KEY_PREFERRED_AUTHENTICATIONS, "keyboard-interactive,password");

//			System.out.println("no credentials given");
//			if (keyInfo.useKey()) {
//				setKey(buildInfo, ssh, keyInfo);
//				sessionProperties.put(CONFIG_KEY_PREFERRED_AUTHENTICATIONS, "publickey");
//			} else {
//				session.setPassword(Util.fixNull(keyInfo.getPassphrase()));
//				sessionProperties.put(CONFIG_KEY_PREFERRED_AUTHENTICATIONS, "keyboard-interactive,password");
//			}

		session.setConfig(sessionProperties);
	}

	private void setupSftp(final BapSshClient bapClient) throws IOException {
		final BPBuildInfo buildInfo = bapClient.getBuildInfo();
		final ChannelSftp sftp = openSftpChannel(buildInfo, bapClient.getSession());
		bapClient.setSftp(sftp);
		connectSftpChannel(buildInfo, sftp);
		changeToRootDirectory(bapClient);
		setRootDirectoryInClient(bapClient, sftp);
	}

	private void setKey(final BPBuildInfo buildInfo, final JSch ssh, final LegacyBapSshKeyInfo keyInfo) {
		try {
			ssh.addIdentity("TheKey", keyInfo.getEffectiveKey(buildInfo), null,
					BapSshUtil.toBytes(keyInfo.getPassphrase()));
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

	private Session createSession(final BPBuildInfo buildInfo, final JSch ssh, String hostname, int port) {
		final String overrideCredsId = getPublisherOverrideCredentials(buildInfo);
		System.out.println("override creds: " + overrideCredsId);

		String overrideUserName = null;
		if (overrideCredsId != null) {
			UsernamePasswordCredentials overrideCredentials = getCredentialsUserPasswordOrNull(overrideCredsId);
			if (overrideCredentials != null) {
				overrideUserName = overrideCredentials.getUsername();
			} else {
				// hier ist was faul
			}
		}

		final String username = overrideUserName == null ? getUsername() : overrideUserName;

		try {
			buildInfo.printIfVerbose(Messages.console_session_creating(username, hostname, port));
			Session session = ssh.getSession(username, hostname, port);

			if (StringUtils.isNotEmpty(proxyType) && StringUtils.isNotEmpty(proxyHost)) {
				if (StringUtils.equals(HTTP_PROXY_TYPE, proxyType)) {
					ProxyHTTP proxyHTTP = new ProxyHTTP(proxyHost, proxyPort);
					if (StringUtils.isNotEmpty(proxyCredentialsId)) {
						UsernamePasswordCredentials proxyCreds = getCredentialsUserPasswordOrNull(proxyCredentialsId);
						proxyHTTP.setUserPasswd(proxyCreds.getUsername(), proxyCreds.getPassword().getPlainText());
					} else {
						proxyHTTP.setUserPasswd(null, null);
					}
					session.setProxy(proxyHTTP);
				} else if (StringUtils.equals(SOCKS_4_PROXY_TYPE, proxyType)) {
					ProxySOCKS4 proxySocks4 = new ProxySOCKS4(proxyHost, proxyPort);
					if (StringUtils.isNotEmpty(proxyCredentialsId)) {
						UsernamePasswordCredentials proxyCreds = getCredentialsUserPasswordOrNull(proxyCredentialsId);
						proxySocks4.setUserPasswd(proxyCreds.getUsername(), proxyCreds.getPassword().getPlainText());
					} else {
						proxySocks4.setUserPasswd(null, null);
					}
					session.setProxy(proxySocks4);
				} else if (StringUtils.equals(SOCKS_5_PROXY_TYPE, proxyType)) {
					ProxySOCKS5 proxySocks5 = new ProxySOCKS5(proxyHost, proxyPort);
					if (StringUtils.isNotEmpty(proxyCredentialsId)) {
						UsernamePasswordCredentials proxyCreds = getCredentialsUserPasswordOrNull(proxyCredentialsId);
						proxySocks5.setUserPasswd(proxyCreds.getUsername(), proxyCreds.getPassword().getPlainText());
					} else {
						proxySocks5.setUserPasswd(null, null);
					}
					session.setProxy(proxySocks5);
				}
			}
			return session;
		} catch (JSchException jse) {
			throw new BapPublisherException(Messages.exception_session_create(username, getHostnameTrimmed(), getPort(),
					jse.getLocalizedMessage()), jse);
		}
	}

	private static String getPublisherOverrideCredentials(final BPBuildInfo buildInfo) {
		return (String) buildInfo.get(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY);
	}

	protected JSch createJSch() {
		return new JSch();
	}

	public BapSshHostConfigurationDescriptor getDescriptor() {
		return Jenkins.get().getDescriptorByType(BapSshHostConfigurationDescriptor.class);
	}

	protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapSshHostConfiguration that) {
		return super.addToEquals(builder, that).append(hostCredentialsId, that.hostCredentialsId)
				.append(timeout, that.timeout).append(overrideCredentials, that.overrideCredentials)
				.append(jumpHost, that.jumpHost).append(disableExec, that.disableExec).append(proxyType, that.proxyType)
				.append(proxyHost, that.proxyHost).append(proxyPort, that.proxyPort)
				.append(proxyCredentialsId, that.proxyCredentialsId);
	}

	@Override
	protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
		return super.addToHashCode(builder).append(hostCredentialsId).append(timeout).append(overrideCredentials)
				.append(jumpHost).append(disableExec).append(proxyType).append(proxyHost).append(proxyPort)
				.append(proxyCredentialsId);
	}

	@Override
	protected ToStringBuilder addToToString(final ToStringBuilder builder) {
		return super.addToToString(builder).append("keyInfo", hostCredentialsId).append("timeout", timeout)
				.append("overrideKey", overrideCredentials).append("jumpHost", jumpHost)
				.append("disableExec", disableExec).append("proxyType", proxyType).append("proxyHost", proxyHost)
				.append("proxyPort", proxyPort).append("proxyCredentialsId", proxyCredentialsId);
	}

	@Override
	public boolean equals(final Object that) {
		if (this == that)
			return true;
		if (that == null || getClass() != that.getClass())
			return false;
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

	private UsernamePasswordCredentials getCredentialsUserPasswordOrNull(final String pCredentialId) {
		if (pCredentialId == null) {
			return null;
		}
		return CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(UsernamePasswordCredentialsImpl.class, Jenkins.get(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.allOf(CredentialsMatchers.withId(pCredentialId)));
	}

	@Override
	public String getUsername() {
		return getEffectiveCredentials(null).getUsername();
	}

	public void setUsername(final String username) {
		LOG.warn("no credentials should be stored with this method: setUsername");
	}

	@Override
	public String getPassword() {
		LOG.warn("no credentials should be received with this method: getPassword");
		return "";
	}

	@Override
	public void setPassword(final String password) {
		LOG.warn("no credentials should be stored with this method: setPassword");
	}

	@Override
	public String getEncryptedPassword() {
		LOG.warn("no credentials should be received with this method: getEncryptedPassword");
		return "";
	}

}
