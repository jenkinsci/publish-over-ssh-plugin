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
import hudson.util.Secret;
import jenkins.plugins.publish_over.BPBuildInfo;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;

@SuppressWarnings("PMD.TooManyMethods")
public class BapSshKeyInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String passphrase;
    private Secret secretPassphrase;
    private String key;
    private String keyPath;

    public BapSshKeyInfo(final String encryptedPassphrase, final String key, final String keyPath) {
        secretPassphrase = Secret.fromString(encryptedPassphrase);
        this.key = key;
        this.keyPath = keyPath;
    }

    protected final String getPassphrase() { return Secret.toString(secretPassphrase); }
    public final void setPassphrase(final String passphrase) { secretPassphrase = Secret.fromString(passphrase); }

    public final String getEncryptedPassphrase() {
        return (secretPassphrase == null) ? null : secretPassphrase.getEncryptedValue();
    }

    public String getKey() { return key; }
    public void setKey(final String key) { this.key = key; }

    public String getKeyPath() { return keyPath; }
    public void setKeyPath(final String keyPath) { this.keyPath = keyPath; }

    public byte[] getEffectiveKey(final BPBuildInfo buildInfo) {
        if (hasKey())
            return BapSshUtil.toBytes(key);
        return buildInfo.readFileFromMaster(keyPath.trim());
    }

    public boolean useKey() {
        return hasKey() || hasKeyPath();
    }

    private boolean hasKey() {
        return Util.fixEmptyAndTrim(key) != null;
    }

    private boolean hasKeyPath() {
        return Util.fixEmptyAndTrim(keyPath) != null;
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(secretPassphrase)
            .append(key)
            .append(keyPath);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapSshKeyInfo that) {
        return builder.append(secretPassphrase, that.secretPassphrase)
            .append(key, that.key)
            .append(keyPath, that.keyPath);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("passphrase", "***")
            .append("key", "***")
            .append("keyPath", keyPath);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapSshKeyInfo) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public Object readResolve() {
        if (secretPassphrase == null)
            secretPassphrase = Secret.fromString(passphrase);
        passphrase = null;
        return this;
    }

}
