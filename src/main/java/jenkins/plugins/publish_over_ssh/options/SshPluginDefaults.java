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
import jenkins.plugins.publish_over.options.GlobalDefaults;
import jenkins.plugins.publish_over.options.InstanceConfigOptions;
import jenkins.plugins.publish_over.options.ParamPublishOptions;
import jenkins.plugins.publish_over.options.PublisherLabelOptions;
import jenkins.plugins.publish_over.options.PublisherOptions;
import jenkins.plugins.publish_over.options.RetryOptions;
import jenkins.plugins.publish_over.view_defaults.manage_jenkins.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

import java.lang.reflect.Proxy;

public final class SshPluginDefaults extends SshDefaults {

    public static final GlobalDefaults GLOBAL_DEFAULTS = new GlobalDefaults();
    private static final SshTransferOptions TRANSFER_DEFAULTS;

    static {
        TRANSFER_DEFAULTS = (SshTransferOptions) Proxy.newProxyInstance(
                SshTransferOptions.class.getClassLoader(),
                new Class[]{SshTransferOptions.class},
                new SshPluginDefaultsHandler());
    }

    @DataBoundConstructor
    public SshPluginDefaults() { }

    public InstanceConfigOptions getInstanceConfig() {
        return GLOBAL_DEFAULTS;
    }

    public ParamPublishOptions getParamPublish() {
        return GLOBAL_DEFAULTS;
    }

    public PublisherOptions getPublisher() {
        return GLOBAL_DEFAULTS;
    }

    public PublisherLabelOptions getPublisherLabel() {
        return GLOBAL_DEFAULTS;
    }

    public RetryOptions getRetry() {
        return GLOBAL_DEFAULTS;
    }

    public SshTransferOptions getTransfer() {
        return TRANSFER_DEFAULTS;
    }

    @Extension
    public static final class SshPluginDefaultsDescriptor extends SshDefaultsDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.defaults_pluginDefaults();
        }

    }

}
