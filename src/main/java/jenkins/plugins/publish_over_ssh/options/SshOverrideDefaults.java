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
import jenkins.plugins.publish_over.options.InstanceConfigOptions;
import jenkins.plugins.publish_over.options.ParamPublishOptions;
import jenkins.plugins.publish_over.options.PublisherLabelOptions;
import jenkins.plugins.publish_over.options.PublisherOptions;
import jenkins.plugins.publish_over.options.RetryOptions;
import jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

public class SshOverrideDefaults extends SshDefaults {

    private final SshOverrideInstanceConfigDefaults overrideInstanceConfig;
    private final SshOverrideParamPublishDefaults overrideParamPublish;
    private final SshOverridePublisherDefaults overridePublisher;
    private final SshOverridePublisherLabelDefaults overridePublisherLabel;
    private final SshOverrideRetryDefaults overrideRetry;
    private final SshOverrideTransferDefaults overrideTransfer;

    @DataBoundConstructor
    public SshOverrideDefaults(final SshOverrideInstanceConfigDefaults overrideInstanceConfig,
                               final SshOverrideParamPublishDefaults overrideParamPublish,
                               final SshOverridePublisherDefaults overridePublisher,
                               final SshOverridePublisherLabelDefaults overridePublisherLabel,
                               final SshOverrideRetryDefaults overrideRetry,
                               final SshOverrideTransferDefaults overrideTransfer) {
        this.overrideInstanceConfig = overrideInstanceConfig;
        this.overrideParamPublish = overrideParamPublish;
        this.overridePublisher = overridePublisher;
        this.overridePublisherLabel = overridePublisherLabel;
        this.overrideRetry = overrideRetry;
        this.overrideTransfer = overrideTransfer;
    }

    // prevent the property type being clobbered in the descriptor map by using different names from the IF
    public SshOverrideInstanceConfigDefaults getOverrideInstanceConfig() {
        return overrideInstanceConfig;
    }

    public SshOverrideParamPublishDefaults getOverrideParamPublish() {
        return overrideParamPublish;
    }

    public SshOverridePublisherDefaults getOverridePublisher() {
        return overridePublisher;
    }

    public SshOverridePublisherLabelDefaults getOverridePublisherLabel() {
        return overridePublisherLabel;
    }

    public SshOverrideRetryDefaults getOverrideRetry() {
        return overrideRetry;
    }

    public SshOverrideTransferDefaults getOverrideTransfer() {
        return overrideTransfer;
    }

    public InstanceConfigOptions getInstanceConfig() {
        return overrideInstanceConfig;
    }

    public ParamPublishOptions getParamPublish() {
        return overrideParamPublish;
    }

    public PublisherOptions getPublisher() {
        return overridePublisher;
    }

    public PublisherLabelOptions getPublisherLabel() {
        return overridePublisherLabel;
    }

    public RetryOptions getRetry() {
        return overrideRetry;
    }

    public SshTransferOptions getTransfer() {
        return overrideTransfer;
    }

    @Extension
    public static class SshOverrideDefaultsDescriptor extends SshDefaultsDescriptor {

        private static final SshPluginDefaults PLUGIN_DEFAULTS = new SshPluginDefaults();

        @Override
        public String getDisplayName() {
            return Messages.defaults_overrideDefaults();
        }

        public SshPluginDefaults getPluginDefaults() {
            return PLUGIN_DEFAULTS;
        }

    }

}
