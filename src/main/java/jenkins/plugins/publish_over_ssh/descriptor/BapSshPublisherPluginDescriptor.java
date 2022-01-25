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

package jenkins.plugins.publish_over_ssh.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPInstanceConfig;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.BapSshSftpSetupException;
import jenkins.plugins.publish_over_ssh.Messages;
import jenkins.plugins.publish_over_ssh.options.SshDefaults;
import jenkins.plugins.publish_over_ssh.options.SshPluginDefaults;
import net.sf.json.JSONObject;

@SuppressWarnings("PMD.TooManyMethods")
public class BapSshPublisherPluginDescriptor extends BuildStepDescriptor<Publisher> {

	/** null - prevent complaints from xstream */
	@SuppressFBWarnings(value = "URF_UNREAD_FIELD")
	private transient BPPluginDescriptor.BPDescriptorMessages msg;
	/** null - prevent complaints from xstream */
	@SuppressFBWarnings(value = "URF_UNREAD_FIELD")
	private transient Class commonConfigClass;
	/** null - prevent complaints from xstream */
	@SuppressFBWarnings(value = "URF_UNREAD_FIELD")
	private transient Class hostConfigClass;
	private final CopyOnWriteList<BapSshHostConfiguration> hostConfigurations = new CopyOnWriteList<BapSshHostConfiguration>();
	private BapSshCommonConfiguration commonConfig;
	private SshDefaults defaults;

	public BapSshPublisherPluginDescriptor() {
		super(BapSshPublisherPlugin.class);
		load();
		if (defaults == null)
			defaults = new SshPluginDefaults();
	}

	public BapSshCommonConfiguration getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(final BapSshCommonConfiguration commonConfig) {
		this.commonConfig = commonConfig;
	}

	public SshDefaults getDefaults() {
		return defaults;
	}

	@Override
	public String getDisplayName() {
		return Messages.descriptor_displayName();
	}

	public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
		return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
	}

	public List<BapSshHostConfiguration> getHostConfigurations() {
		List<BapSshHostConfiguration> retVal = new ArrayList<>();

		for (BapSshHostConfiguration current : hostConfigurations.getView()) {
			retVal.add(current);
		}

		Collections.sort(retVal, new Comparator<BapSshHostConfiguration>() {

			@Override
			public int compare(BapSshHostConfiguration p1, BapSshHostConfiguration p2) {
				return p1.getName().compareTo(p2.getName());
			}

		});

		return retVal;
	}

	public BapSshHostConfiguration getConfiguration(final String name) {
		for (BapSshHostConfiguration configuration : hostConfigurations) {
			if (configuration.getName().equals(name)) {
				return configuration;
			}
		}
		return null;
	}

	/**
	 * Add a Host Configuration to the list of configurations.
	 * 
	 * @param configuration Host Configuration to add. The common configuration will
	 *                      be automatically set.
	 */
	public void addHostConfiguration(final BapSshHostConfiguration configuration) {
		configuration.setCommonConfig(commonConfig);
		hostConfigurations.add(configuration);
	}

	/**
	 * Removes the given named Host Configuration from the list of configurations.
	 * 
	 * @param name The Name of the Host Configuration to remove.
	 */
	public void removeHostConfiguration(final String name) {
		BapSshHostConfiguration configuration = getConfiguration(name);
		if (configuration != null) {
			hostConfigurations.remove(configuration);
		}
	}

	@Override
	public boolean configure(final StaplerRequest request, final JSONObject formData) {
		final List<BapSshHostConfiguration> newConfigurations = request.bindJSONToList(BapSshHostConfiguration.class,
				formData.get("instance"));
		commonConfig = request.bindJSON(BapSshCommonConfiguration.class, formData.getJSONObject("commonConfig"));
		for (BapSshHostConfiguration hostConfig : newConfigurations) {
			hostConfig.setCommonConfig(commonConfig);
		}
		hostConfigurations.replaceBy(newConfigurations);
		if (isEnableOverrideDefaults())
			defaults = request.bindJSON(SshDefaults.class, formData.getJSONObject("defaults"));
		save();
		return true;
	}

	public boolean canSetMasterNodeName() {
		return false;
	}

	public String getDefaultMasterNodeName() {
		return BPInstanceConfig.DEFAULT_MASTER_NODE_NAME;
	}

	public boolean isEnableOverrideDefaults() {
		return true;
	}

	public BapSshPublisherDescriptor getPublisherDescriptor() {
		return Jenkins.get().getDescriptorByType(BapSshPublisherDescriptor.class);
	}

	public BapSshHostConfigurationDescriptor getHostConfigurationDescriptor() {
		return Jenkins.get().getDescriptorByType(BapSshHostConfigurationDescriptor.class);
	}

	public SshPluginDefaults.SshPluginDefaultsDescriptor getPluginDefaultsDescriptor() {
		return Jenkins.get().getDescriptorByType(SshPluginDefaults.SshPluginDefaultsDescriptor.class);
	}

	public jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages getCommonFieldNames() {
		return new jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages();
	}

	public jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages getCommonManageMessages() {
		return new jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages();
	}

	public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
		final BapSshHostConfiguration hostConfig = request.bindParameters(BapSshHostConfiguration.class, "");
		hostConfig.setCommonConfig(request.bindParameters(BapSshCommonConfiguration.class, ""));
		
		for(String current : request.getParameterMap().keySet()) {
			System.out.println(current + "=" + request.getParameter(current));
		}
		System.out.println("---");
		// name,hostname,hostCredentialsId,remoteRootDir,jumpHost,port,timeout,overrideKey,common.generalCredentialsId,proxyType,proxyHost,proxyPort,proxyCredentialsId"/>
		System.out.println("getName: " + hostConfig.getName());
		System.out.println("Hostname: " + hostConfig.getHostname());
		System.out.println("getHostCredentialsId: " + hostConfig.getHostCredentialsId());
		System.out.println("remoteRootDir: " + hostConfig.getRemoteRootDir());
		System.out.println("jumpHost: " + hostConfig.getJumpHost());
		System.out.println("port: " + hostConfig.getPort());
		System.out.println("timeout: " + hostConfig.getTimeout());
		System.out.println("overrideKey: " + hostConfig.isOverrideKey());
		System.out.println("proxyType: " + hostConfig.getProxyType());
		System.out.println("proxyHost: " + hostConfig.getProxyHost());
		System.out.println("proxyPort: " + hostConfig.getProxyPort());
		System.out.println("getProxyCredentialsId: " + hostConfig.getProxyCredentialsId());
		System.out.println("getCommonConfig().getGeneralCredentialsId(): " + hostConfig.getCommonConfig().getGeneralCredentialsId());
		
		return validateConnection(hostConfig, createDummyBuildInfo());
	}

	public static FormValidation validateConnection(BapSshHostConfiguration hostConfig, BPBuildInfo buildInfo) {
		try {
			hostConfig.createClient(buildInfo).disconnect();
			return FormValidation.ok(Messages.descriptor_testConnection_ok());
		} catch (BapSshSftpSetupException sse) {
			return connectionError(Messages.descriptor_testConnection_sftpError(), sse);
		} catch (Exception e) {
			return connectionError(Messages.descriptor_testConnection_error(), e);
		}
	}

	private static FormValidation connectionError(final String description, final Exception exception) {
		exception.printStackTrace();
		return FormValidation.errorWithMarkup("<p>" + description + "</p><p><pre>"
				+ Util.escape(exception.getClass().getCanonicalName() + ": " + exception.getLocalizedMessage())
				+ "</pre></p>");
	}

	public static BPBuildInfo createDummyBuildInfo() {
		return new BPBuildInfo(TaskListener.NULL, "", Jenkins.get().getRootPath(), null, null);
	}

	public Object readResolve() {
		// nuke the legacy config
		msg = null;
		commonConfigClass = null;
		hostConfigClass = null;
		if (defaults == null)
			defaults = new SshPluginDefaults();
		return this;
	}

}
