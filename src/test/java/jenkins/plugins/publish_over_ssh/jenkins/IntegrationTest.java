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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisher;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.BapSshTransfer;
import jenkins.plugins.publish_over_ssh.BapSshUtil;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
@WithJenkins
class IntegrationTest {

    // @TODO test that we get the expected result when in a promotion

    @Test
    void testIntegration(JenkinsRule j) throws Exception {
        final JSch mockJsch = mock(JSch.class);
        final Session mockSession = mock(Session.class);
        final ChannelSftp mockSftp = mock(ChannelSftp.class);
        final int port = 28;
        final int timeout = 3000;
        final BapSshHostConfiguration testHostConfig = new BapSshHostConfiguration() {
            @Override
            public JSch createJSch() {
                return mockJsch;
            }
            @Serial
            @Override
            public Object readResolve() {
                return super.readResolve();
            }
        };
        JenkinsTestHelper.fill(testHostConfig, "testConfig", "testHostname", "testUsername", "",
                "/testRemoteRoot", "", port, timeout, false, "", "", false);
        final BapSshCommonConfiguration commonConfig = new BapSshCommonConfiguration("passphrase", "key", "", false);
        new JenkinsTestHelper().setGlobalConfig(commonConfig, testHostConfig);
        final String dirToIgnore = "target";
        final int execTimeout = 10000;
        final BapSshTransfer transfer = new BapSshTransfer("**/*", null, "sub-home", dirToIgnore, false, false, "", execTimeout, false, false, false, false, null);
        final BapSshPublisher publisher = new BapSshPublisher(testHostConfig.getName(), false,
            new ArrayList<>(Collections.singletonList(transfer)), false, false, null, null, null);
        final BapSshPublisherPlugin plugin = new BapSshPublisherPlugin(
            new ArrayList<>(Collections.singletonList(publisher)), false, false, false, "master", null);

        final FreeStyleProject project = j.createFreeStyleProject();
        project.getPublishersList().add(plugin);
        final String buildDirectory = "build-dir";
        final String buildFileName = "file.txt";
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
                                   throws InterruptedException, IOException {
                final FilePath dir = build.getWorkspace().child(dirToIgnore).child(buildDirectory);
                dir.mkdirs();
                dir.child(buildFileName).write("Helloooooo", "UTF-8");
                build.setResult(Result.SUCCESS);
                return true;
            }
        });

        when(mockJsch.getSession(testHostConfig.getUsername(), testHostConfig.getHostname(), testHostConfig.getPort()))
                .thenReturn(mockSession);
        when(mockSession.openChannel("sftp")).thenReturn(mockSftp);
        final SftpATTRS mockAttrs = mock(SftpATTRS.class);
        when(mockAttrs.isDir()).thenReturn(true);
        when(mockSftp.stat(anyString())).thenReturn(mockAttrs);

        j.assertBuildStatusSuccess(project.scheduleBuild2(0).get());

        verify(mockJsch).addIdentity("TheKey", BapSshUtil.toBytes("key"), null, BapSshUtil.toBytes("passphrase"));
        verify(mockSession).connect(timeout);
        verify(mockSftp).connect(timeout);
        verify(mockSftp).cd(transfer.getRemoteDirectory());
        verify(mockSftp).cd("build-dir");
        verify(mockSftp).put((InputStream) any(), eq(buildFileName));
    }

    @Test
    void agentAuthenticationFailureReportsOriginalCause(JenkinsRule j) throws Exception {
        final DumbSlave agent = j.createOnlineSlave();
        final int timeout = 3000;
        final BapSshHostConfiguration hostConfig = JenkinsTestHelper.prepare(
                "agentConfig", "127.0.0.1", "testUsername", "", "/tmp", "", 22, timeout, false, "", "", false);
        final BapSshCommonConfiguration commonConfig =
                new BapSshCommonConfiguration("", "not-a-private-key", "", false);
        new JenkinsTestHelper().setGlobalConfig(commonConfig, hostConfig);

        final BapSshTransfer transfer = new BapSshTransfer(
                "artifact.txt", null, "", "", false, false, "", timeout, false, false, false, false, null);
        final BapSshPublisher publisher = new BapSshPublisher(hostConfig.getName(), false,
                new ArrayList<>(Collections.singletonList(transfer)), false, false, null, null, null);
        final BapSshPublisherPlugin plugin = new BapSshPublisherPlugin(
                new ArrayList<>(Collections.singletonList(publisher)), false, true, false, "master", null);

        final FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedNode(agent);
        project.getPublishersList().add(plugin);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("artifact.txt").write("test", "UTF-8");
                return true;
            }
        });

        final FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogContains("Diagnostic build [agent-auth-v5], execution [agent]", build);
        j.assertLogContains("Authentication mode [publickey], credential source [inline-key]", build);
        j.assertLogContains("Failed to add SSH key", build);
    }

    @Test
    void agentPasswordIsPreparedOnController(JenkinsRule j) throws Exception {
        final DumbSlave agent = j.createOnlineSlave();
        final int timeout = 1000;
        final BapSshHostConfiguration hostConfig = JenkinsTestHelper.fill(
                new IncompatibleCommonConfigHostConfiguration(), "passwordConfig", "127.0.0.1", "testUsername",
                "", "/tmp", "", 1, timeout, false, "", "", false);
        final BapSshCommonConfiguration commonConfig =
                new BapSshCommonConfiguration("test-password", "", "", false);
        new JenkinsTestHelper().setGlobalConfig(commonConfig, hostConfig);

        final BapSshTransfer transfer = new BapSshTransfer(
                "artifact.txt", null, "", "", false, false, "", timeout, false, false, false, false, null);
        final BapSshPublisher publisher = new BapSshPublisher(hostConfig.getName(), false,
                new ArrayList<>(Collections.singletonList(transfer)), false, false, null, null, null);
        final BapSshPublisherPlugin plugin = new BapSshPublisherPlugin(
                new ArrayList<>(Collections.singletonList(publisher)), false, true, false, "master", null);

        final FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedNode(agent);
        project.getPublishersList().add(plugin);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("artifact.txt").write("test", "UTF-8");
                return true;
            }
        });

        final FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogContains(
                "Authentication mode [password], credential source [controller-preloaded-password]", build);
        j.assertLogContains("Failed to connect session", build);
    }

    private static final class IncompatibleCommonConfigHostConfiguration extends BapSshHostConfiguration {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public BapSshCommonConfiguration getCommonConfig() {
            throw new NoSuchMethodError("Simulates an incompatible publish-over API on the agent");
        }
    }

}
