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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import jenkins.plugins.publish_over.BPBuildEnv;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.security.MasterToSlaveCallable;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
@WithJenkins
class BapSshKeyInfoTest {

    @Test
    void pathBasedKeyIsPreloadedBeforePublishingOnAgent(final JenkinsRule j) throws Exception {
        final FilePath controllerRoot = j.jenkins.getRootPath();
        assertNotNull(controllerRoot);
        final FilePath keyFile = controllerRoot.child("keys").child("test-key");
        final byte[] expectedKey = "test private key".getBytes(StandardCharsets.UTF_8);
        keyFile.write(new String(expectedKey, StandardCharsets.UTF_8), StandardCharsets.UTF_8.name());

        final TreeMap<String, String> environment = new TreeMap<>();
        environment.put("KEY_DIR", "keys");
        final BPBuildEnv currentBuild = new BPBuildEnv(environment, controllerRoot, null);
        final BPBuildInfo buildInfo = new BPBuildInfo(TaskListener.NULL, "SSH: ", controllerRoot, currentBuild, null);
        buildInfo.setEnvVars(environment);
        final BapSshKeyInfo keyInfo = new BapSshKeyInfo(null, null, "$KEY_DIR/test-key");
        keyInfo.preloadAuthentication(buildInfo);
        keyFile.delete();

        final DumbSlave agent = j.createOnlineSlave();
        final Computer computer = agent.toComputer();
        assertNotNull(computer);
        final VirtualChannel channel = computer.getChannel();
        assertNotNull(channel);

        assertArrayEquals(expectedKey, channel.call(new ReadEffectiveKey(keyInfo, buildInfo)));

        final BapSshKeyInfo passwordInfo = new BapSshKeyInfo("test-password", null, null);
        passwordInfo.preloadAuthentication(buildInfo);
        assertEquals("test-password", channel.call(new ReadEffectivePassphrase(passwordInfo, buildInfo)));
    }

    private static final class ReadEffectiveKey extends MasterToSlaveCallable<byte[], IOException> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final BapSshKeyInfo keyInfo;
        private final BPBuildInfo buildInfo;

        private ReadEffectiveKey(final BapSshKeyInfo keyInfo, final BPBuildInfo buildInfo) {
            this.keyInfo = keyInfo;
            this.buildInfo = buildInfo;
        }

        @Override
        public byte[] call() {
            return keyInfo.getEffectiveKey(buildInfo);
        }
    }

    private static final class ReadEffectivePassphrase extends MasterToSlaveCallable<String, IOException> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final BapSshKeyInfo keyInfo;
        private final BPBuildInfo buildInfo;

        private ReadEffectivePassphrase(final BapSshKeyInfo keyInfo, final BPBuildInfo buildInfo) {
            this.keyInfo = keyInfo;
            this.buildInfo = buildInfo;
        }

        @Override
        public String call() {
            return keyInfo.getEffectivePassphrase(buildInfo);
        }
    }
}
