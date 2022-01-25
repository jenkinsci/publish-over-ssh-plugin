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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPValidators;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.Messages;

@Extension
public class BapSshHostConfigurationDescriptor extends Descriptor<BapSshHostConfiguration> {

	// https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
	private static final Pattern VALID_IP_ADDRESS_PATTERN = Pattern.compile(
			"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

	// https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
	private static final Pattern VALID_HOSTNAME_PATTERN = Pattern.compile(
			"^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

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
		FormValidation retVal = FormValidation.validateRequired(value);
		if (retVal.kind != FormValidation.Kind.OK) {
			return retVal;
		}

		if (isValidHostname(value) || isValidIP(value)) {
			return FormValidation.ok();
		}

		return FormValidation.error("Hostname is not a Hostname or IP: " + value);
	}

	public FormValidation doCheckPort(@QueryParameter final String value) {
		return FormValidation.validatePositiveInteger(value);
	}

	public FormValidation doCheckTimeout(@QueryParameter final String value) {
		return FormValidation.validateNonNegativeInteger(value);
	}

	public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
		final BapSshPublisherPlugin.Descriptor pluginDescriptor;
		Jenkins j = Jenkins.getInstanceOrNull();
		if (j != null) {
			pluginDescriptor = j.getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
		} else {
			throw new NullPointerException("Jenkins is not ready on going to be offline...");
		}
		return pluginDescriptor.doTestConnection(request, response);
	}

	public jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages getCommonFieldNames() {
		return new jenkins.plugins.publish_over.view_defaults.HostConfiguration.Messages();
	}

	public ListBoxModel doFillHostCredentialsIdItems(@AncestorInPath Item pItem) {
		final ListBoxModel retVal;

		retVal = new StandardListBoxModel().includeMatchingAs(ACL.SYSTEM, pItem, StandardUsernameCredentials.class,
				Collections.<DomainRequirement>emptyList(),
				CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
						CredentialsMatchers.instanceOf(BasicSSHUserPrivateKey.class)));

		return retVal;
	}

	/**
	 * For proxy this must be Username & Password credentials
	 * 
	 * @param pItem
	 * @return
	 */
	public ListBoxModel doFillProxyCredentialsIdItems(@AncestorInPath Item pItem) {
		final ListBoxModel retVal;

		retVal = new StandardListBoxModel().includeMatchingAs(ACL.SYSTEM, pItem, StandardUsernameCredentials.class,
				Collections.<DomainRequirement>emptyList(),
				CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
						CredentialsMatchers.instanceOf(BasicSSHUserPrivateKey.class)));

		return retVal;
	}

	boolean isValidIP(final String pIPAsString) {
		if (pIPAsString == null || StringUtils.isEmpty(pIPAsString)) {
			return false;
		}

		Matcher matcher = VALID_IP_ADDRESS_PATTERN.matcher(pIPAsString);
		return matcher.matches();
	}

	boolean isValidHostname(final String pHostnameAsString) {
		if (pHostnameAsString == null || StringUtils.isEmpty(pHostnameAsString)) {
			return false;
		}

		Matcher matcher = VALID_HOSTNAME_PATTERN.matcher(pHostnameAsString);
		return matcher.matches();
	}

}
