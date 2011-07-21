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

package jenkins.plugins.publish_over_ssh.options;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import jenkins.plugins.publish_over.options.RetryOptions;
import jenkins.plugins.publish_over_ssh.BapSshRetry;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SshOverrideRetryDefaults implements RetryOptions, Describable<SshOverrideRetryDefaults> {

    private final int retries;
    private final long retryDelay;

    @DataBoundConstructor
    public SshOverrideRetryDefaults(final int retries, final long retryDelay) {
        this.retries = retries;
        this.retryDelay = retryDelay;
    }

    public int getRetries() {
        return retries;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public SshOverrideRetryDefaultsDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(SshOverrideRetryDefaultsDescriptor.class);
    }

    @Extension
    public static class SshOverrideRetryDefaultsDescriptor extends Descriptor<SshOverrideRetryDefaults> {

        @Override
        public String getDisplayName() {
            return "SshOverrideRetryDefaultsDescriptor - not visible ...";
        }

        public FormValidation doCheckRetries(@QueryParameter final String value) {
            return FormValidation.validateNonNegativeInteger(value);
        }

        public FormValidation doCheckRetryDelay(@QueryParameter final String value) {
            return FormValidation.validatePositiveInteger(value);
        }

        public jenkins.plugins.publish_over.view_defaults.Retry.Messages getCommonFieldNames() {
            return new jenkins.plugins.publish_over.view_defaults.Retry.Messages();
        }

        public String getConfigPage() {
            return getViewPage(BapSshRetry.class, "config.jelly");
        }

    }

}
