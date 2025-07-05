package jenkins.plugins.publish_over_ssh;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;

/** Tests evaluation of combinations for jumphost, implemented in the HostsHelper-class. 
 *  
 * @author Christian Langmann
 *
 */
class HostsHelperTest {

    @Test
    void testGetHostsEmptyJumpHosts() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("hostname"));
    }

    @Test
    void testGetHostsOneJumpHosts() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "hostname"));
    }

    @Test
    void testGetHostsTwoJumpHosts() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost jumphost2");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "jumphost2", "hostname"));
    }

    @Test
    void testGetHostsTwoJumpHostsSeparator() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost, jumphost2");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "jumphost2", "hostname"));
    }

    @Test
    void testGetHostsTwoJumpHostsSeparatorSemicolon() {
        String[] hosts = BapSshHostConfiguration.HostsHelper.getHosts("hostname", "jumphost;jumphost2");
        assertThat(hosts, is(not((String[]) null)));
        assertThat(hosts, arrayContaining("jumphost", "jumphost2", "hostname"));
    }
}
