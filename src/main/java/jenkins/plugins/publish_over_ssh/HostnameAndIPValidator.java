package jenkins.plugins.publish_over_ssh;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class HostnameAndIPValidator {

	// https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
	private static final Pattern VALID_IP_ADDRESS_PATTERN = Pattern.compile(
			"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

	// https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
	private static final Pattern VALID_HOSTNAME_PATTERN = Pattern.compile(
			"^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

	public static boolean isValidIP(final String pIPAsString) {
		if (pIPAsString == null || StringUtils.isEmpty(pIPAsString)) {
			return false;
		}

		Matcher matcher = VALID_IP_ADDRESS_PATTERN.matcher(pIPAsString);
		return matcher.matches();
	}

	public static boolean isValidHostname(final String pHostnameAsString) {
		if (pHostnameAsString == null || StringUtils.isEmpty(pHostnameAsString)) {
			return false;
		}

		Matcher matcher = VALID_HOSTNAME_PATTERN.matcher(pHostnameAsString);
		return matcher.matches();
	}

	public static boolean isValidHostNameOrIP(final String pValue) {
		return isValidHostname(pValue) || isValidIP(pValue);
	}
}
