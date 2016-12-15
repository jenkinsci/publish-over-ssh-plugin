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
import hudson.model.Hudson;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshCommonConfigurationDescriptor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.DataBoundConstructor;

public class BapSshCommonConfiguration extends BapSshKeyInfo implements Describable<BapSshCommonConfiguration> {

    private static final long serialVersionUID = 1L;

    private final boolean disableAllExec;

    @DataBoundConstructor
    public BapSshCommonConfiguration(final String encryptedPassphrase, final String key, 
                                     final String keyPath, final boolean disableAllExec) {
        super(encryptedPassphrase, key, keyPath);
        this.disableAllExec = disableAllExec;
    }

    public boolean isDisableAllExec() {
        return disableAllExec;
    }

    public BapSshCommonConfigurationDescriptor getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(BapSshCommonConfigurationDescriptor.class);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapSshCommonConfiguration that) {
        return super.addToEquals(builder, that)
            .append(disableAllExec, that.disableAllExec);
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return super.addToHashCode(builder)
            .append(disableAllExec);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return super.addToToString(builder)
            .append("disableAllExec", disableAllExec);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapSshCommonConfiguration) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
