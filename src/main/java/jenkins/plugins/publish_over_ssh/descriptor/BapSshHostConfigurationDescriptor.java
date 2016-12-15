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

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import jenkins.plugins.publish_over.BPValidators;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.Messages;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@Extension
public class BapSshHostConfigurationDescriptor extends Descriptor<BapSshHostConfiguration> {

    public BapSshHostConfigurationDescriptor() {
        super(BapSshHostConfiguration.class);
    }

    @Override
    public String getDisplayName() {
        return Messages.global_common_descriptor();
    }

    public String getDefaultJumpHost() {
        return BapSshHostConfiguration.DEFAULT_JUMP_HOST;
    }

    public int getDefaultPort() {
        return BapSshHostConfiguration.DEFAULT_PORT;
    }

    public int getDefaultTimeout() {
        return BapSshHostConfiguration.DEFAULT_TIMEOUT;
    }

    public FormValidation doCheckName(@QueryParameter final String value) {
        return BPValidators.validateName(value);
    }

    public FormValidation doCheckHostname(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }

    public FormValidation doCheckUsername(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }

    public FormValidation doCheckPort(@QueryParameter final String value) {
        return FormValidation.validatePositiveInteger(value);
    }

    public FormValidation doCheckTimeout(@QueryParameter final String value) {
        return FormValidation.validateNonNegativeInteger(value);
    }

    public FormValidation doCheckKeyPath(@QueryParameter final String value) {
        return BPValidators.validateFileOnMaster(value);
    }

    public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
        final BapSshPublisherPlugin.Descriptor pluginDescriptor = Hudson.getInstance().getDescriptorByType(
                BapSshPublisherPlugin.Descriptor.class);
        return pluginDescriptor.doTestConnection(request, response);
    }

    public jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages getCommonFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages();
    }

}
