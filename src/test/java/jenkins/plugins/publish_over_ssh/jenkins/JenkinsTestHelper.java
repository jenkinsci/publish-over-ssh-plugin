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

package jenkins.plugins.publish_over_ssh.jenkins;

import hudson.util.Secret;
import java.util.Arrays;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshPublisherPluginDescriptor;

public class JenkinsTestHelper {

    public static BapSshHostConfiguration fill(final BapSshHostConfiguration toFill, final String name, final String hostname, final String username, final String encryptedPassword,
                                               final String remoteRootDir, final String jumpHost, final int port, final int timeout, final boolean overrideKey,
                                               final String keyPath, final String key, final boolean disableExec) {
        toFill.setName(name);
        toFill.setHostname(hostname);
        toFill.setUsername(username);
        toFill.setEncryptedPassword(encryptedPassword);
        toFill.setRemoteRootDir(remoteRootDir);
        toFill.setJumpHost(jumpHost);
        toFill.setPort(port);
        toFill.setTimeout(timeout);
        toFill.setOverrideKey(overrideKey);
        toFill.setKeyPath(keyPath);
        toFill.setKey(key);
        toFill.setDisableExec(disableExec);
        return toFill;
    }

    public static BapSshHostConfiguration fillProxySettings(final BapSshHostConfiguration toFill, final String proxyType, final String proxyHost, final int proxyPort, final String proxyUser, final String proxyPassword) {
        toFill.setProxyType(proxyType);
        toFill.setProxyHost(proxyHost);
        toFill.setProxyPort(proxyPort);
        toFill.setProxyUser(proxyUser);
        toFill.setSecretProxyPassword(Secret.fromString(proxyPassword));
        return toFill;
    }

    public static BapSshHostConfiguration prepare(final String name, final String hostname, final String username, final String encryptedPassword,
                                                  final String remoteRootDir, final String jumpHost, final int port, final int timeout, final boolean overrideKey,
                                                  final String keyPath, final String key, final boolean disableExec) {
        BapSshHostConfiguration bapSshHostConfiguration = new BapSshHostConfiguration();
        return fill(bapSshHostConfiguration, name, hostname, username, encryptedPassword, remoteRootDir, jumpHost, port, timeout, overrideKey, keyPath, key, disableExec);
    }


    public void setGlobalConfig(final BapSshCommonConfiguration commonConfig, final BapSshHostConfiguration... newHostConfigurations) {
        final BapSshPublisherPluginDescriptor descriptor =
                Jenkins.get().getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        descriptor.setCommonConfig(commonConfig);
        descriptor.setHostConfigurations(Arrays.asList(newHostConfigurations));
    }

}
