package jenkins.plugins.publish_over_ssh;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.Credentials;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshCredentialsDescriptor;

public class PublishOverSSHCredentials implements Credentials<PublishOverSSHCredentials> {

	private static final long serialVersionUID = 2451496428123830831L;

	private final String credentialsId;

	@DataBoundConstructor
	public PublishOverSSHCredentials(final String credentialsId) {
		this.credentialsId = credentialsId;
	}

	@Override
	public Descriptor<PublishOverSSHCredentials> getDescriptor() {
		return Jenkins.get().getDescriptorByType(BapSshCredentialsDescriptor.class);
	}

	public String getCredentialsId() {
		return credentialsId;
	}

}
