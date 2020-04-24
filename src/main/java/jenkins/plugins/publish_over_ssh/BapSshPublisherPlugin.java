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

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshPublisherPluginDescriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;


@SuppressWarnings({ "PMD.TooManyMethods", "PMD.LooseCoupling" })
public class BapSshPublisherPlugin extends BPPlugin<BapSshPublisher, BapSshClient, BapSshCommonConfiguration> {

    private static final long serialVersionUID = 1L;

    public BapSshPublisherPlugin(final ArrayList<BapSshPublisher> publishers, final boolean continueOnError, final boolean failOnError,
                                 final boolean alwaysPublishFromMaster, final String masterNodeName,
                                 final BapSshParamPublish paramPublish) {
        super(Messages.console_message_prefix(), publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName,
                paramPublish);
    }

    @DataBoundConstructor
    public BapSshPublisherPlugin() {
        super(Messages.console_message_prefix());
    }

    public BapSshParamPublish getParamPublish() {
        return (BapSshParamPublish) getDelegate().getParamPublish();
    }

    @DataBoundSetter
    public void setParamPublish(final BapSshParamPublish paramPublish) {
        this.getDelegate().setParamPublish(paramPublish);
    }

    public List<BapSshPublisher> getPublishers() {
        return this.getDelegate().getPublishers();
    }

    @DataBoundSetter
    public void setPublishers(final ArrayList<BapSshPublisher> publishers) {
        this.getDelegate().setPublishers(publishers);
    }

    public boolean isContinueOnError() {
        return this.getDelegate().isContinueOnError();
    }

    @DataBoundSetter
    public void setContinueOnError(final boolean continueOnError) {
        this.getDelegate().setContinueOnError(continueOnError);
    }

    public boolean isFailOnError() {
        return this.getDelegate().isFailOnError();
    }

    @DataBoundSetter
    public void setFailOnError(final boolean failOnError) {
        this.getDelegate().setFailOnError(failOnError);
    }

    public boolean isAlwaysPublishFromMaster() {
        return this.getDelegate().isAlwaysPublishFromMaster();
    }

    @DataBoundSetter
    public void setAlwaysPublishFromMaster(final boolean alwaysPublishFromMaster) {
        this.getDelegate().setAlwaysPublishFromMaster(alwaysPublishFromMaster);
    }

    public String getMasterNodeName() {
        return this.getDelegate().getMasterNodeName();
    }

    @DataBoundSetter
    public void setMasterNodeName(final String masterNodeName) {
        this.getDelegate().setMasterNodeName(masterNodeName);
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapSshPublisherPlugin) that).isEquals();
    }

    @Override
    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    @Override
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    @Override
    public Descriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(Descriptor.class);
    }

    public BapSshHostConfiguration getConfiguration(final String name) {
        return getDescriptor().getConfiguration(name);
    }

    @Extension @Symbol("sshPublisher")
    public static class Descriptor extends BapSshPublisherPluginDescriptor {
        @Override
        public Object readResolve() {
            return super.readResolve();
        }
    }

    /**
     * @deprecated
     * prevent xstream noise
     * */
    @Deprecated
    public static class DescriptorMessages implements BPPluginDescriptor.BPDescriptorMessages { }

}
