package jenkins.plugins.publish_over_ssh;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/** Tests evaluation of combinations for jumphost, implemented in the HostsHelper-class. 
 *  
 * @author Christian Langmann
 *
 */
public class HostsHelperTest {

    @Test
    public void testGetHostsEmptyJumpHosts() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("hostname"));
    }

    @Test
    public void testGetHostsOneJumpHosts() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "hostname"));
    }

    @Test
    public void testGetHostsTwoJumpHosts() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost jumphost2");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "jumphost2", "hostname"));
    }

    @Test
    public void testGetHostsTwoJumpHostsSeparator() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost, jumphost2");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "jumphost2", "hostname"));
    }

    @Test
    public void testGetHostsTwoJumpHostsSeparatorSemicolon() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost;jumphost2");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "jumphost2", "hostname"));
    }
}
