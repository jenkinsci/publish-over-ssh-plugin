package jenkins.plugins.publish_over_ssh.descriptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import jenkins.plugins.publish_over_ssh.HostnameAndIPValidator;

public class HostnameAndIPValidatorTest {

	@Test
	public void testValidHostnames() {
		final String[] validHostnames = new String[] { "a", "localhost", "superserver123", "server.company.com",
				"server.in.a.subnet.com" };
		for (String validHostname : validHostnames) {
			assertTrue(HostnameAndIPValidator.isValidHostname(validHostname));
		}
	}

	@Test
	public void testInvalidHostnames() {
		final String[] invalidHostnames = new String[] { "", null, "name with space", "namew/ithslash", "namewith@",
				"namewith{" };
		for (String invalidHostname : invalidHostnames) {
			assertFalse(HostnameAndIPValidator.isValidHostname(invalidHostname));
		}
	}

	@Test
	public void testValidIPs() {
		final String[] valid = new String[] { "1.2.3.4", "127.0.0.1", "192.168.178.1", "255.255.255.255" };
		for (String validIP : valid) {
			assertTrue(HostnameAndIPValidator.isValidIP(validIP));
		}
	}

	@Test
	public void testInvalidIPs() {
		final String[] invalid = new String[] { "", "1", "1.2", "1.2.3", "1.2.3.4.5" };
		for (String invalidIP : invalid) {
			assertFalse(HostnameAndIPValidator.isValidIP(invalidIP));
		}
	}

}
