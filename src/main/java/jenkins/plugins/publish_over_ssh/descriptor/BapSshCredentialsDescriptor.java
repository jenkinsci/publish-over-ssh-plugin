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
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over_ssh.BapSshCredentials;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;

import java.io.IOException;

@Extension
public class BapSshCredentialsDescriptor extends Descriptor<BapSshCredentials> {

    public BapSshCredentialsDescriptor() {
        super(BapSshCredentials.class);
    }

    @Override
    public String getDisplayName() {
        return "not seen";
    }

    public FormValidation doCheckUsername(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }

    public FormValidation doCheckKeyPath(@QueryParameter final String value) {
        AccessControlled subject = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
        if (subject == null) {
            subject = Jenkins.getInstance();
        }
        if (!subject.hasPermission(Item.CONFIGURE)&&subject.hasPermission(Item.EXTENDED_READ)) {
            return FormValidation.ok();
        }
        try {
            return Jenkins.getInstance().getRootPath().validateRelativePath(value, true, true);
        } catch (final IOException ioe) {
            return FormValidation.error(ioe, "");
        } catch (final NullPointerException npe) {
            return FormValidation.error(npe, "");
        }
    }

    public FormValidation doTestConnection(@QueryParameter final String configName, @QueryParameter final String username,
                                           @QueryParameter final String encryptedPassphrase, @QueryParameter final String key,
                                           @QueryParameter final String keyPath) {
        final BapSshCredentials credentials = new BapSshCredentials(username, encryptedPassphrase, key, keyPath);
        final BPBuildInfo buildInfo = BapSshPublisherPluginDescriptor.createDummyBuildInfo();
        buildInfo.put(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY, credentials);
        final BapSshPublisherPlugin.Descriptor pluginDescriptor = Hudson.getInstance().getDescriptorByType(
                                                                                                    BapSshPublisherPlugin.Descriptor.class);
        final BapSshHostConfiguration hostConfig = pluginDescriptor.getConfiguration(configName);
        return BapSshPublisherPluginDescriptor.validateConnection(hostConfig, buildInfo);
    }

    public jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages getCommonFieldNames() {
        return new jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages();
    }

}
