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

import hudson.Util;
import hudson.model.Describable;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over.BPTransfer;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshTransferDescriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class BapSshTransfer extends BPTransfer implements Describable<BapSshTransfer> {

    private static final long serialVersionUID = 1L;

    private String execCommand;
    private int execTimeout;
    private boolean usePty;
    private boolean useAgentForwarding;
    private boolean useSftpForExec;
    private boolean keepFilePermissions;

    BapSshTransfer(final String sourceFiles, final String remoteDirectory, final String removePrefix,
                   final boolean remoteDirectorySDF, final boolean flatten, final String execCommand, final int execTimeout) {
        this(sourceFiles, null, remoteDirectory, removePrefix, remoteDirectorySDF, flatten, execCommand, execTimeout, false, false, false, false, null);
    }

    public BapSshTransfer(final String sourceFiles, final String excludes, final String remoteDirectory, final String removePrefix,
                          final boolean remoteDirectorySDF, final boolean flatten, final String execCommand, final int execTimeout,
                          final boolean usePty, final boolean keepFilePermissions, final boolean noDefaultExcludes, final boolean makeEmptyDirs,
                          final String patternSeparator) {
        super(sourceFiles, excludes, remoteDirectory, removePrefix, remoteDirectorySDF, flatten, false, noDefaultExcludes, makeEmptyDirs, patternSeparator);
        this.execCommand = execCommand;
        this.execTimeout = execTimeout;
        this.usePty = usePty;
        this.useAgentForwarding = false;
        this.keepFilePermissions = keepFilePermissions;
    }

    @DataBoundConstructor
    public BapSshTransfer(final String sourceFiles, final String excludes, final String remoteDirectory, final String removePrefix,
                          final boolean remoteDirectorySDF, final boolean flatten, final boolean cleanRemote, final String execCommand, final int execTimeout,
                          final boolean usePty, final boolean keepFilePermissions, final boolean noDefaultExcludes, final boolean makeEmptyDirs,
                          final String patternSeparator) {
        super(sourceFiles, excludes, remoteDirectory, removePrefix, remoteDirectorySDF, flatten, cleanRemote, noDefaultExcludes, makeEmptyDirs, patternSeparator);
        this.execCommand = execCommand;
        this.execTimeout = execTimeout;
        this.usePty = usePty;
        this.useAgentForwarding = false;
        this.keepFilePermissions = keepFilePermissions;
    }

    public String getExecCommand() { return execCommand; }

    @DataBoundSetter
    public void setExecCommand(String execCommand) {
        this.execCommand = execCommand;
    }

    public int getExecTimeout() { return execTimeout; }

    @DataBoundSetter
    public void setExecTimeout(int execTimeout) {
        this.execTimeout = execTimeout;
    }

    public boolean hasExecCommand() {
        return Util.fixEmptyAndTrim(getExecCommand()) != null;
    }

    public boolean isUsePty() {
        return usePty;
    }

    @DataBoundSetter
    public void setUsePty(boolean usePty) {
        this.usePty = usePty;
    }

    public boolean isUseSftpForExec() {
        return useSftpForExec;
    }

    @DataBoundSetter
    public void setUseSftpForExec(boolean useSftpForExec) {
        this.useSftpForExec = useSftpForExec;
    }

    public boolean isUseAgentForwarding() {
        return useAgentForwarding;
    }

    @DataBoundSetter
    public void setUseAgentForwarding(boolean value) {
        useAgentForwarding = value;
    }

    public boolean isKeepFilePermissions() {
        return keepFilePermissions;
    }

    @DataBoundSetter
    public void setKeepFilePermissions(boolean keepFilePermissions) {
        this.keepFilePermissions = keepFilePermissions;
    }

    public BapSshTransferDescriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(BapSshTransferDescriptor.class);
    }

    @Override
    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return super.addToHashCode(builder).append(execCommand).append(execTimeout).append(usePty).append(useAgentForwarding).append(useSftpForExec);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapSshTransfer that) {
        return super.addToEquals(builder, that)
                .append(execCommand, that.execCommand)
                .append(execTimeout, that.execTimeout)
                .append(usePty, that.usePty)
                .append(useAgentForwarding, that.useAgentForwarding)
                .append(useSftpForExec, that.useSftpForExec)
                .append(keepFilePermissions, that.keepFilePermissions);
    }

    @Override
    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return super.addToToString(builder)
                .append("execCommand", execCommand)
                .append("execTimeout", execTimeout)
                .append("pseudoTty", usePty)
                .append("agentForwarding", useAgentForwarding)
                .append("useSftpForExec", useSftpForExec)
                .append("keepFilePermissions", keepFilePermissions);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapSshTransfer) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
