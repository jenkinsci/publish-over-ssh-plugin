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
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.options.PublisherOptions;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import org.kohsuke.stapler.DataBoundConstructor;

public class SshOverridePublisherDefaults implements PublisherOptions, Describable<SshOverridePublisherDefaults> {

    private final String configName;
    private final boolean useWorkspaceInPromotion;
    private final boolean usePromotionTimestamp;
    private final boolean verbose;
    private final Jenkins j = Jenkins.getInstanceOrNull();

    @DataBoundConstructor
    public SshOverridePublisherDefaults(final String configName, final boolean useWorkspaceInPromotion, final boolean usePromotionTimestamp,
                                        final boolean verbose) {
        this.configName = configName;
        this.usePromotionTimestamp = usePromotionTimestamp;
        this.useWorkspaceInPromotion = useWorkspaceInPromotion;
        this.verbose = verbose;
    }

    public String getConfigName() {
        return configName;
    }

    public boolean isUseWorkspaceInPromotion() {
        return useWorkspaceInPromotion;
    }

    public boolean isUsePromotionTimestamp() {
        return usePromotionTimestamp;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public SshOverridePublisherDefaultsDescriptor getDescriptor() {
        if(j != null) {
            return j.getDescriptorByType(SshOverridePublisherDefaultsDescriptor.class);
        }
        else {
            throw new NullPointerException("Jenkins is not ready on going to be offline...");
        }
    }

    @Extension
    public static class SshOverridePublisherDefaultsDescriptor extends Descriptor<SshOverridePublisherDefaults> {

        @Override
        public String getDisplayName() {
            return "SshOverridePublisherDefaultsDescriptor - not visible ...";
        }

        public BapSshPublisherPlugin.Descriptor getPublisherPluginDescriptor() {
            return Jenkins.getActiveInstance().getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        }

        public jenkins.plugins.publish_over.view_defaults.BapPublisher.Messages getCommonFieldNames() {
            return new jenkins.plugins.publish_over.view_defaults.BapPublisher.Messages();
        }

    }

}
