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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.plugins.publish_over.BPInstanceConfig;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

public class BapSshPreBuildWrapper extends BuildWrapper {
    
    @Extension(ordinal = 11)
    public static final Descriptor DESCRIPTOR = new Descriptor();
    
    BapSshAlwaysRunPublisherPlugin preBuild;
    
    @DataBoundConstructor
    public BapSshPreBuildWrapper(List<BapSshPublisher> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster, String masterNodeName) {
        preBuild = new BapSshAlwaysRunPublisherPlugin(publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName);
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        return preBuild.perform(build, launcher, listener) ? new Environment(){} : null;
    }
    
    public BPInstanceConfig getInstanceConfig() {
        return preBuild.getInstanceConfig();
    }
    
    public static class Descriptor extends BuildWrapperDescriptor {
        public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
            return true;
        }
        public String getDisplayName() {
            return Messages.preBuild_descriptor_displayName();
        }
        public String getConfigPage() {
            return getViewPage(BapSshPublisherPlugin.class, "config.jelly");
        }
        public BapSshPublisherPlugin.Descriptor getPublisherDescriptor() {
            return BapSshPublisherPlugin.DESCRIPTOR;
        }
    }
}
