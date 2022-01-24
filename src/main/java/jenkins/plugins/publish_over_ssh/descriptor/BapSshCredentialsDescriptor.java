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

import java.util.Collections;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.LegacyBapSshCredentials;

@Extension
public class BapSshCredentialsDescriptor extends Descriptor<LegacyBapSshCredentials> {

	public BapSshCredentialsDescriptor() {
		super(LegacyBapSshCredentials.class);
	}

	@Override
	public String getDisplayName() {
		return "not seen";
	}

	public FormValidation doTestConnection(@QueryParameter final String configName,
			@QueryParameter final String credentialsId) {

		final LegacyBapSshCredentials legacyCredentials = null;

		final BPBuildInfo buildInfo = BapSshPublisherPluginDescriptor.createDummyBuildInfo();
		buildInfo.put(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY, legacyCredentials);
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

	public jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages getCommonFieldNames() {
		return new jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages();
	}

	public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item pItem) {
		final ListBoxModel retVal;

		retVal = new StandardListBoxModel().includeEmptyValue().includeMatchingAs(ACL.SYSTEM, pItem,
				StandardUsernamePasswordCredentials.class, Collections.<DomainRequirement>emptyList(),
				CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

		return retVal;
	}

}
