package jenkins.plugins.publish_over_ssh;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ManagementLink;
import hudson.util.FormApply;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshPublisherPluginDescriptor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

@Extension
public class BapSshManagementLink extends ManagementLink {


  @Override
  public String getIconFileName() {

    return "/plugin/publish-over-ssh/images/ssh.png";
  }

  @Override
  public String getDisplayName() {
    return Messages.managementLink_displayName();
  }

  @Override
  public String getUrlName() {
    return "publish_over_ssh";
  }

  @Override
  public String getDescription() {
    return Messages.managementLink_description();
  }

  @POST
  public void doConfigure(StaplerRequest req, StaplerResponse res)
      throws ServletException, FormException, IOException {
    getDescriptor().configure(req, req.getSubmittedForm());
    FormApply.success(req.getContextPath() + "/manage").generateResponse(req, res, null);
  }

  public Descriptor getDescriptor() {
    return Jenkins.get().getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
  }

}
