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
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import hudson.util.VersionNumber;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("PMD.TooManyMethods")
public class BapSshPublisherPlugin extends BPPlugin<BapSshPublisher, BapSshClient, BapSshCommonConfiguration> {

    private static final long serialVersionUID = 1L;

    @Extension
    public static final Descriptor DESCRIPTOR = new Descriptor();

    @DataBoundConstructor
    public BapSshPublisherPlugin(final ArrayList<BapSshPublisher> publishers, final boolean continueOnError, final boolean failOnError,
                                 final boolean alwaysPublishFromMaster, final String masterNodeName) {
        super(Messages.console_message_prefix(), publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return createEqualsBuilder((BapSshPublisherPlugin) that).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public BapSshHostConfiguration getConfiguration(final String name) {
        return DESCRIPTOR.getConfiguration(name);
    }

    public static class Descriptor extends BPPluginDescriptor<BapSshHostConfiguration, BapSshCommonConfiguration> {
        public Descriptor() {
            super(new DescriptorMessages(), BapSshPublisherPlugin.class, BapSshHostConfiguration.class, BapSshCommonConfiguration.class);
        }
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return !BPPlugin.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }
        public FormValidation doCheckKeyPath(@QueryParameter final String value) {
            //@todo confirm version number then enable
            if (false && Hudson.getVersion().isNewerThan(new VersionNumber("1.399"))) {
                try {
                    return Hudson.getInstance().getRootPath().validateRelativePath(value, true, true);
                } catch (IOException ioe) {
                    return FormValidation.error(ioe, "");
                }
            }
            return FormValidation.ok();
        }
        public FormValidation doCheckSourceFiles(@QueryParameter final String sourceFiles, @QueryParameter final String execCommand) {
            return checkTransferSet(sourceFiles, execCommand);
        }
        public FormValidation doCheckExecCommand(@QueryParameter final String sourceFiles, @QueryParameter final String execCommand) {
            return checkTransferSet(sourceFiles, execCommand);
        }
        public FormValidation doCheckExecTimeout(@QueryParameter final String value) {
            return FormValidation.validateNonNegativeInteger(value);
        }
        private FormValidation checkTransferSet(final String sourceFiles, final String execCommand) {
            return haveAtLeastOne(sourceFiles, execCommand) ? FormValidation.ok()
                : FormValidation.error(Messages.descriptor_sourceOrExec());
        }
        private boolean haveAtLeastOne(final String... values) {
            for (String value : values)
                if (Util.fixEmptyAndTrim(value) != null)
                    return true;
            return false;
        }
        public BapSshPublisherPlugin.Descriptor getPublisherDescriptor() {
            return this;
        }
    }

    public static class DescriptorMessages implements BPPluginDescriptor.BPDescriptorMessages {
        public String displayName() {
            return Messages.descriptor_displayName();
        }
        public String connectionOK() {
            return Messages.descriptor_testConnection_ok();
        }
        public String connectionErr() {
            return Messages.descriptor_testConnection_error();
        }
    }

}
