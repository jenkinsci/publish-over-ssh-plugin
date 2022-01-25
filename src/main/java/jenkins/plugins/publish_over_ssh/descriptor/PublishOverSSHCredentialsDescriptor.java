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

import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.PublishOverSSHCredentials;

@Extension
public class PublishOverSSHCredentialsDescriptor extends Descriptor<PublishOverSSHCredentials> {

	public PublishOverSSHCredentialsDescriptor() {
		super(PublishOverSSHCredentials.class);
	}

	@Override
	public String getDisplayName() {
		return "not seen";
	}

	public FormValidation doCheckCredentialsId(@QueryParameter final String value) {
		return FormValidation.validateRequired(value);
	}

	public FormValidation doTestConnection(@QueryParameter final String configName,
			@QueryParameter final String credentialsId) {
		final BPBuildInfo buildInfo = BapSshPublisherPluginDescriptor.createDummyBuildInfo();
		buildInfo.put(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY, credentialsId);
		Jenkins j = Jenkins.getInstanceOrNull();
		final BapSshPublisherPlugin.Descriptor pluginDescriptor;
		if (j != null) {
			pluginDescriptor = j.getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
		} else {
			throw new NullPointerException("Jenkins is not ready on going to be offline...");
		}

		final BapSshHostConfiguration hostConfig = pluginDescriptor.getConfiguration(configName);
		return BapSshPublisherPluginDescriptor.validateConnection(hostConfig, buildInfo);
	}

}
