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

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import jenkins.plugins.publish_over.BPValidators;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class BapSshHostConfiguration extends BapHostConfiguration implements Describable<BapSshHostConfiguration> {

    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public BapSshHostConfiguration(final String name, final String hostname, final String username, final String encryptedPassword,
                                   final String remoteRootDir, final int port, final int timeout, final boolean overrideKey,
                                   final String keyPath, final String key, final boolean disableExec) {
        super(name, hostname, username, encryptedPassword, remoteRootDir, port, timeout, overrideKey, keyPath, key, disableExec);
    }

    public DescriptorImpl getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        final BapSshHostConfiguration thatHostConfig = (BapSshHostConfiguration) that;

        return addToEquals(new EqualsBuilder(), thatHostConfig).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BapSshHostConfiguration> {
        @Override
        public String getDisplayName() {
            return Messages.global_common_descriptor();
        }
        public int getDefaultPort() {
            return BapHostConfiguration.DEFAULT_PORT;
        }
        public int getDefaultTimeout() {
            return BapHostConfiguration.DEFAULT_TIMEOUT;
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
    }

}
