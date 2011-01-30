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
