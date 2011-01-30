package jenkins.plugins.publish_over_ssh;

import jenkins.plugins.publish_over.BapPublisher;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;


public class BapSshPublisher extends BapPublisher<BapSshTransfer> {
    
    @DataBoundConstructor
    public BapSshPublisher(String configName, boolean verbose, List<BapSshTransfer> transfers) {
        super(configName, verbose, transfers);
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        return createEqualsBuilder((BapSshPublisher) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }
    
    
}
