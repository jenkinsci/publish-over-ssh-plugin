package jenkins.plugins.publish_over_ssh.descriptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BapSshHostConfigurationDescriptorTest {

	@Test
	public void testValidHostnames() {
		final BapSshHostConfigurationDescriptor x = new BapSshHostConfigurationDescriptor();

		final String[] validHostnames = new String[] { "a", "localhost", "superserver123", "server.company.com",
				"server.in.a.subnet.com" };
		for (String validHostname : validHostnames) {
			assertTrue(x.isValidHostname(validHostname));
		}
	}

	@Test
	public void testInvalidHostnames() {
		final BapSshHostConfigurationDescriptor x = new BapSshHostConfigurationDescriptor();

		final String[] invalidHostnames = new String[] { "", null, "name with space", "namew/ithslash", "namewith@",
				"namewith{" };
		for (String invalidHostname : invalidHostnames) {
			assertFalse(x.isValidHostname(invalidHostname));
		}
	}

	@Test
	public void testValidIPs() {
		final BapSshHostConfigurationDescriptor x = new BapSshHostConfigurationDescriptor();

		final String[] valid = new String[] { "1.2.3.4", "127.0.0.1", "192.168.178.1", "255.255.255.255" };
		for (String validIP : valid) {
			assertTrue(x.isValidIP(validIP));
		}
	}

	@Test
	public void testInvalidIPs() {
		final BapSshHostConfigurationDescriptor x = new BapSshHostConfigurationDescriptor();

		final String[] invalid = new String[] { "", "1", "1.2", "1.2.3", "1.2.3.4.5" };
		for (String invalidIP : invalid) {
			assertFalse(x.isValidIP(invalidIP));
		}
	}

}
