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
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.plugins.publish_over.*;
import net.sf.json.JSONObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

public class BapSshBuilderPlugin extends Builder {

    private static final String PROMOTION_JOB_TYPE = "hudson.plugins.promoted_builds.PromotionProcess";
    
    @Extension
    public static final Descriptor DESCRIPTOR = new Descriptor();
    
    private BapSshPublisherPlugin delegate;
    
    @DataBoundConstructor
	public BapSshBuilderPlugin(List<BapSshPublisher> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster, String masterNodeName) {
		this.delegate = new BapSshPublisherPlugin(publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName);
    }

	public List<BapPublisher> getPublishers() { return delegate.getPublishers(); }
	public void setPublishers(List<BapPublisher> publishers) { delegate.setPublishers(publishers); }

    public boolean isContinueOnError() { return delegate.isContinueOnError(); }
    public void setContinueOnError(boolean continueOnError) { delegate.setContinueOnError(continueOnError); }

    public boolean isFailOnError() { return delegate.isFailOnError(); }
    public void setFailOnError(boolean failOnError) { delegate.setFailOnError(failOnError); }

    public boolean isAlwaysPublishFromMaster() { return delegate.isAlwaysPublishFromMaster(); }
    public void setAlwaysPublishFromMaster(boolean alwaysPublishFromMaster) { delegate.setAlwaysPublishFromMaster(alwaysPublishFromMaster); }

    public String getMasterNodeName() { return delegate.getMasterNodeName(); }
    public void setMasterNodeName(String masterNodeName) { delegate.setMasterNodeName(masterNodeName); }
    
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return delegate.perform(build, launcher, listener);
	}
    
    protected HashCodeBuilder createHashCodeBuilder() {
        return addToHashCode(new HashCodeBuilder());
    }

    protected HashCodeBuilder addToHashCode(HashCodeBuilder builder) {
        return builder.append(delegate);
    }
    
    protected EqualsBuilder createEqualsBuilder(BapSshBuilderPlugin that) {
        return addToEquals(new EqualsBuilder(), that);
    }
    
    protected EqualsBuilder addToEquals(EqualsBuilder builder, BapSshBuilderPlugin that) {
        return builder.append(delegate, that.delegate);
    }
    
    protected ToStringBuilder addToToString(ToStringBuilder builder) {
        return builder.append("delegate", delegate);
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        return createEqualsBuilder((BapSshBuilderPlugin) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }
    
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        private final transient Log log = LogFactory.getLog(Descriptor.class);
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return !PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }
        public String getDisplayName() {
            return Messages.builder_descriptor_displayName();
        }
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (log.isDebugEnabled())
                log.debug(Messages.builder_log_newInstance(formData.toString(2)));
            return super.newInstance(req, formData);
        }
        public String getConfigPage() {
            return getViewPage(BapSshPublisherPlugin.class, "config.jelly");
        }
        public List<BapSshHostConfiguration> getHostConfigurations() {
            return BapSshPublisherPlugin.DESCRIPTOR.getHostConfigurations();
        }
    }

}
