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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPInstanceConfig;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import jenkins.plugins.publish_over.JenkinsCapabilities;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.BapSshSftpSetupException;
import jenkins.plugins.publish_over_ssh.Messages;
import jenkins.plugins.publish_over_ssh.options.SshDefaults;
import jenkins.plugins.publish_over_ssh.options.SshPluginDefaults;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;

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

    public BapSshCommonConfiguration getCommonConfig() { return commonConfig; }
    public void setCommonConfig(final BapSshCommonConfiguration commonConfig) { this.commonConfig = commonConfig; }

    public SshDefaults getDefaults() {
        return defaults;
    }

    public String getDisplayName() {
        return Messages.descriptor_displayName();
    }

    public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
        return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
    }

    public List<BapSshHostConfiguration> getHostConfigurations() {
        return hostConfigurations.getView();
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
     * @param configuration Host Configuration to add. The common configuration will be automatically set.
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
        return JenkinsCapabilities.missing(JenkinsCapabilities.MASTER_HAS_NODE_NAME);
    }

    public String getDefaultMasterNodeName() {
        return BPInstanceConfig.DEFAULT_MASTER_NODE_NAME;
    }

    public boolean isEnableOverrideDefaults() {
        return JenkinsCapabilities.available(JenkinsCapabilities.SIMPLE_DESCRIPTOR_SELECTOR);
    }

    public BapSshPublisherDescriptor getPublisherDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BapSshPublisherDescriptor.class);
    }

    public BapSshHostConfigurationDescriptor getHostConfigurationDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BapSshHostConfigurationDescriptor.class);
    }

    public SshPluginDefaults.SshPluginDefaultsDescriptor getPluginDefaultsDescriptor() {
        return Hudson.getInstance().getDescriptorByType(SshPluginDefaults.SshPluginDefaultsDescriptor.class);
    }

    public jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages getCommonFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.BPInstanceConfig.Messages();
    }

    public jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages getCommonManageMessages() {
        return new jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages();
    }

    public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
        final BapSshHostConfiguration hostConfig = request.bindParameters(BapSshHostConfiguration.class, "");
        hostConfig.setCommonConfig(request.bindParameters(BapSshCommonConfiguration.class, "common."));
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
        return FormValidation.errorWithMarkup("<p>"
                + description + "</p><p><pre>"
                + Util.escape(exception.getClass().getCanonicalName() + ": " + exception.getLocalizedMessage())
                + "</pre></p>");
    }

    public static BPBuildInfo createDummyBuildInfo() {
        return new BPBuildInfo(
            TaskListener.NULL,
            "",
            Hudson.getInstance().getRootPath(),
            null,
            null
        );
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
