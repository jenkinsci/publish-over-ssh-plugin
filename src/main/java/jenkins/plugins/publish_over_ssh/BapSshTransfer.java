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
import jenkins.plugins.publish_over.BPTransfer;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

public class BapSshTransfer extends BPTransfer {

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_EXEC_TIMEOUT = 120000;

    public static int getDefaultExecTimeout() { return DEFAULT_EXEC_TIMEOUT; }

    private String execCommand;
    private int execTimeout;

    BapSshTransfer(final String sourceFiles, final String remoteDirectory, final String removePrefix,
                          final boolean remoteDirectorySDF, final boolean flatten, final String execCommand, final int execTimeout) {
        this(sourceFiles, null, remoteDirectory, removePrefix, remoteDirectorySDF, flatten, execCommand, execTimeout);
    }

    @DataBoundConstructor
    public BapSshTransfer(final String sourceFiles, final String excludes, final String remoteDirectory, final String removePrefix,
                          final boolean remoteDirectorySDF, final boolean flatten, final String execCommand, final int execTimeout) {
        super(sourceFiles, excludes, remoteDirectory, removePrefix, remoteDirectorySDF, flatten);
        this.execCommand = execCommand;
        this.execTimeout = execTimeout;
    }

    public String getExecCommand() { return execCommand; }
    public void setExecCommand(final String execCommand) { this.execCommand = execCommand; }

    public int getExecTimeout() { return execTimeout; }
    public void setExecTimeout(final int execTimeout) { this.execTimeout = execTimeout; }

    public boolean hasExecCommand() {
        return Util.fixEmptyAndTrim(getExecCommand()) != null;
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        final BapSshTransfer thatTransfer = (BapSshTransfer) that;

        return createEqualsBuilder(thatTransfer)
            .append(execCommand, thatTransfer.execCommand)
            .append(execTimeout, thatTransfer.execTimeout)
            .isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder()
            .append(execCommand)
            .append(execTimeout)
            .toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE))
            .append(execCommand)
            .append(execTimeout)
            .toString();
    }

}
