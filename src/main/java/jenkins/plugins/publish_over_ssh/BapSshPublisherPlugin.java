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
import jenkins.plugins.publish_over.BPPlugin;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class BapSshPublisherPlugin extends BPPlugin<BapSshPublisher, BapSshClient> {
    
        private static BPPluginDescriptor.DescriptorMessages createDescriptorMessages() {
        return new BPPluginDescriptor.DescriptorMessages() {
            public String displayName() {
                return Messages.descriptor_displayName();
            }
            public String connectionOK() {
                return Messages.descriptor_testConnection_ok();
            }
            public String connectionErr() {
                return Messages.descriptor_testConnection_error();
            }
        };
    }

    @Extension
    public static final BPPluginDescriptor<BapSshHostConfiguration> DESCRIPTOR = new BPPluginDescriptor<BapSshHostConfiguration>(
            createDescriptorMessages(), BapSshPublisherPlugin.class, BapSshHostConfiguration.class);

    @DataBoundConstructor
	public BapSshPublisherPlugin(List<BapSshPublisher> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster) {
        super(Messages.console_message_prefix(), publishers, continueOnError, failOnError, alwaysPublishFromMaster);
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        return createEqualsBuilder((BapSshPublisherPlugin) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public BapSshHostConfiguration getConfiguration(String name) {
		return DESCRIPTOR.getConfiguration(name);
	}
    
}
