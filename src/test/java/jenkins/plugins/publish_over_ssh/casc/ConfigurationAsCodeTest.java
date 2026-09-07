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

package jenkins.plugins.publish_over_ssh.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshPublisherPluginDescriptor;
import jenkins.plugins.publish_over_ssh.options.SshOverrideDefaults;
import jenkins.plugins.publish_over_ssh.options.SshOverrideTransferDefaults;
import jenkins.plugins.publish_over_ssh.options.SshPluginDefaults;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void shouldImportFullSystemConfiguration(JenkinsConfiguredWithCodeRule rule) {
        assertFullConfiguration(rule);
    }

    @Test
    @ConfiguredWithCode("minimal.yml")
    void shouldImportMinimalConfigurationWithPluginDefaults(JenkinsConfiguredWithCodeRule rule) {
        BapSshPublisherPluginDescriptor d = rule.jenkins.getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        assertEquals(1, d.getHostConfigurations().size());
        assertEquals("only-host", d.getHostConfigurations().get(0).getName());
        assertInstanceOf(SshPluginDefaults.class, d.getDefaults());
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void shouldExportWithoutPlaintextSecretsAndReapply(JenkinsConfiguredWithCodeRule rule) throws Exception {
        String exported = rule.exportToString(true);

        assertTrue(exported.contains("sshPublisher:"));
        assertTrue(exported.contains("hostConfigurations:"));
        assertTrue(exported.contains("overrideDefaults:"));
        assertTrue(exported.contains("useAgentForwarding: true"));
        // secrets never appear in plain text
        assertFalse(exported.contains("common-passphrase"));
        assertFalse(exported.contains("host-passphrase"));
        assertFalse(exported.contains("proxy-secret"));
        // excluded attributes never appear
        assertFalse(exported.contains("proxyPassword:"));
        assertEquals(1, StringUtils.countMatches(exported, "commonConfig:")); // only under sshPublisher, not per host

        // wipe, then re-apply the export
        BapSshPublisherPluginDescriptor d = rule.jenkins.getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        d.setHostConfigurations(List.of());
        d.setCommonConfig(new BapSshCommonConfiguration("", "", "", true));
        d.setDefaults(new SshPluginDefaults());

        Path file = Files.createTempFile(rule.jenkins.getRootDir().toPath(), "ssh-casc-", ".yml");
        Files.writeString(file, exported);
        ConfigurationAsCode.get().configure(file.toString());

        assertFullConfiguration(rule);
    }

    @Test
    @ConfiguredWithCode("minimal.yml")
    void commonConfigIsInjectedRegardlessOfSetterOrder(JenkinsConfiguredWithCodeRule rule) {
        BapSshPublisherPluginDescriptor d = rule.jenkins.getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);
        BapSshCommonConfiguration common = new BapSshCommonConfiguration("pw", "", "/k", false);
        d.setCommonConfig(common); // after hosts were already set by the fixture
        assertSame(common, d.getHostConfigurations().get(0).getCommonConfig());

        BapSshHostConfiguration extra = new BapSshHostConfiguration();
        extra.setName("extra");
        d.setHostConfigurations(List.of(extra)); // after common was already set
        assertSame(common, d.getConfiguration("extra").getCommonConfig());
    }

    private static void assertFullConfiguration(JenkinsConfiguredWithCodeRule rule) {
        BapSshPublisherPluginDescriptor d = rule.jenkins.getDescriptorByType(BapSshPublisherPlugin.Descriptor.class);

        BapSshCommonConfiguration common = d.getCommonConfig();
        assertEquals("common-passphrase", Secret.toString(Secret.decrypt(common.getEncryptedPassphrase())));
        assertTrue(common.getKey().contains("synthetic-test-key-not-real"));
        assertFalse(common.isDisableAllExec());

        List<BapSshHostConfiguration> hosts = d.getHostConfigurations(); // sorted by name
        assertEquals(2, hosts.size());

        BapSshHostConfiguration keyHost = d.getConfiguration("key-host");
        assertEquals("key.example.test", keyHost.getHostname());
        assertEquals("deploy", keyHost.getUsername());
        assertEquals("/srv/app", keyHost.getRemoteRootDir());
        assertEquals(2222, keyHost.getPort());
        assertEquals(60000, keyHost.getTimeout());
        assertEquals("bastion.example.test", keyHost.getJumpHost());
        assertEquals(32, keyHost.getSftpPipelineDepth());
        assertTrue(keyHost.isDisableExec());
        assertTrue(keyHost.isAvoidSameFileUploads());
        assertTrue(keyHost.isOverrideKey());
        assertEquals("host-passphrase", Secret.toString(Secret.decrypt(keyHost.getEncryptedPassword())));
        assertEquals("/var/lib/jenkins/.ssh/id_deploy", keyHost.getKeyPath());
        assertSame(common, keyHost.getCommonConfig());

        BapSshHostConfiguration proxyHost = d.getConfiguration("proxy-host");
        assertEquals("socks5", proxyHost.getProxyType());
        assertEquals("proxy.internal.test", proxyHost.getProxyHost());
        assertEquals(1080, proxyHost.getProxyPort());
        assertEquals("proxyuser", proxyHost.getProxyUser());
        assertEquals("proxy-secret", proxyHost.getSecretProxyPassword().getPlainText());
        assertSame(common, proxyHost.getCommonConfig());

        SshOverrideDefaults defaults = assertInstanceOf(SshOverrideDefaults.class, d.getDefaults());
        assertTrue(defaults.getOverrideInstanceConfig().isAlwaysPublishFromMaster());
        assertTrue(defaults.getOverrideInstanceConfig().isContinueOnError());
        assertTrue(defaults.getOverrideInstanceConfig().isFailOnError());
        assertEquals("SSH_PUBLISH", defaults.getOverrideParamPublish().getParameterName());
        assertEquals("key-host", defaults.getOverridePublisher().getConfigName());
        assertTrue(defaults.getOverridePublisher().isUseWorkspaceInPromotion());
        assertTrue(defaults.getOverridePublisher().isUsePromotionTimestamp());
        assertTrue(defaults.getOverridePublisher().isVerbose());
        assertEquals("release", defaults.getOverridePublisherLabel().getLabel());
        assertEquals(4, defaults.getOverrideRetry().getRetries());
        assertEquals(15000, defaults.getOverrideRetry().getRetryDelay());

        SshOverrideTransferDefaults t = defaults.getOverrideTransfer();
        assertEquals("target/*.jar", t.getSourceFiles());
        assertEquals("target/*-sources.jar", t.getExcludes());
        assertEquals("target", t.getRemovePrefix());
        assertEquals("builds", t.getRemoteDirectory());
        assertTrue(t.isFlatten());
        assertTrue(t.isRemoteDirectorySDF());
        assertTrue(t.isCleanRemote());
        assertEquals("sudo systemctl restart app", t.getExecCommand());
        assertEquals(90000, t.getExecTimeout());
        assertTrue(t.isUsePty());
        assertTrue(t.isUseAgentForwarding());
        assertTrue(t.isKeepFilePermissions());
        assertTrue(t.isNoDefaultExcludes());
        assertTrue(t.isMakeEmptyDirs());
        assertEquals("[, ]+", t.getPatternSeparator());
    }
}
