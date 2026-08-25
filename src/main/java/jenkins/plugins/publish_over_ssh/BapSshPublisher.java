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

import hudson.model.Describable;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisher;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshPublisherDescriptor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;

/**
 * Class required to enable stapler/DBC to bind to correct BPTransfer - BapSshTransfer
 */
@SuppressWarnings("PMD.LooseCoupling") // serializable
public class BapSshPublisher extends BapPublisher<BapSshTransfer> implements Describable<BapSshPublisher> {

    private static final long serialVersionUID = 1L;
    private static final String DIAGNOSTIC_BUILD = "agent-auth-v5";

    @DataBoundConstructor
    public BapSshPublisher(final String configName, final boolean verbose, final ArrayList<BapSshTransfer> transfers,
                           final boolean useWorkspaceInPromotion, final boolean usePromotionTimestamp, final BapSshRetry sshRetry,
                           final BapSshPublisherLabel sshLabel, final BapSshCredentials sshCredentials) {
        super(configName, verbose, transfers, useWorkspaceInPromotion, usePromotionTimestamp, sshRetry, sshLabel, sshCredentials);
    }

    public final boolean isSftpRequired() {
        for (BapSshTransfer transfer : getTransfers()) {
            if (transfer.hasConfiguredSourceFiles() || transfer.isUseSftpForExec()) return true;
        }
        return false;
    }

    @Override
    public void setEffectiveEnvironmentInBuildInfo(final BPBuildInfo buildInfo) {
        super.setEffectiveEnvironmentInBuildInfo(buildInfo);
        final BapSshPublisherPlugin.Descriptor descriptor =
                Jenkins.get().getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        final BapSshHostConfiguration configuration = descriptor.getConfiguration(getConfigName());
        if (configuration != null) {
            configuration.preloadAuthentication(buildInfo, this, descriptor.getCommonConfig());
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void perform(final BPHostConfiguration hostConfig, final BPBuildInfo buildInfo) throws Exception {
        buildInfo.println(Messages.console_diagnosticBuild(
                DIAGNOSTIC_BUILD, buildInfo.onMaster() ? "controller" : "agent"));
        try {
            super.perform(hostConfig, buildInfo);
        } catch (Exception exception) {
            buildInfo.println(Messages.console_failure(describeException(exception)));
            throw exception;
        } catch (LinkageError error) {
            buildInfo.println(Messages.console_failure(describeException(error)));
            throw error;
        }
    }

    private static String describeException(final Throwable exception) {
        final StringBuilder message = new StringBuilder();
        Throwable current = exception;
        int depth = 0;
        while (current != null && depth++ < 8) {
            if (message.length() > 0) {
                message.append(" <- caused by ");
            }
            message.append(current.getClass().getName());
            final String detail = current.getLocalizedMessage();
            if (detail != null && !detail.isBlank()) {
                message.append(": ").append(detail);
            }
            final Throwable cause = current.getCause();
            current = cause == current ? null : cause;
        }
        return message.toString();
    }

    public BapSshRetry getSshRetry() {
        return (BapSshRetry) super.getRetry();
    }

    public BapSshPublisherLabel getSshLabel() {
        return (BapSshPublisherLabel) super.getLabel();
    }

    public BapSshCredentials getSshCredentials() {
        return (BapSshCredentials) getCredentials();
    }

    public BapSshPublisherDescriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(BapSshPublisherDescriptor.class);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapSshPublisher) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
