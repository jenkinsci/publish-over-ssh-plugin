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
import hudson.model.Hudson;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("PMD.LooseCoupling") // serializable
public class BapSshPostBuildWrapper extends BuildWrapper {

    private final BapSshAlwaysRunPublisherPlugin postBuild;

    @DataBoundConstructor
    public BapSshPostBuildWrapper(final ArrayList<BapSshPublisher> publishers, final boolean continueOnError, final boolean failOnError,
                                  final boolean alwaysPublishFromMaster, final String masterNodeName,
                                  final BapSshParamPublish paramPublish) {
        postBuild = new BapSshAlwaysRunPublisherPlugin(publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName,
                                                       paramPublish);
    }

    public BapSshPublisherPlugin getPostBuild() {
        return postBuild;
    }

    @SuppressWarnings("PMD.JUnit4TestShouldUseBeforeAnnotation")
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
                    throws IOException, InterruptedException {
        final Environment runPostBuild = new Environment() {
            @SuppressWarnings("PMD.JUnit4TestShouldUseAfterAnnotation")
            public boolean tearDown(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
                return postBuild.perform(build, listener);
            }
        };
        return runPostBuild;
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(postBuild);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapSshPostBuildWrapper that) {
        return builder.append(postBuild, that.postBuild);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("postBuild", postBuild);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapSshPostBuildWrapper) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    @Extension(ordinal = BapSshUtil.EXTENSION_ORDINAL_POST_BUILD_WRAPPER)
    public static class Descriptor extends BuildWrapperDescriptor {
        public boolean isApplicable(final AbstractProject<?, ?> abstractProject) {
            return true;
        }
        public String getDisplayName() {
            return Messages.postBuild_descriptor_displayName();
        }
        public BapSshPublisherPlugin.Descriptor getPublisherDescriptor() {
            return Hudson.getInstance().getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        }
    }

}
