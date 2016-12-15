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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisher;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.BapSshTransfer;
import jenkins.plugins.publish_over_ssh.BapSshUtil;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class IntegrationTest extends HudsonTestCase {

// @TODO test that we get the expected result when in a promotion

    @Test
    public void testIntegration() throws Exception {
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
        final BapSshTransfer transfer = new BapSshTransfer("**/*", null, "sub-home", dirToIgnore, false, false, "", execTimeout, false, false, false, null);
        final BapSshPublisher publisher = new BapSshPublisher(testHostConfig.getName(), false,
                        new ArrayList<BapSshTransfer>(Collections.singletonList(transfer)), false, false, null, null, null);
        final BapSshPublisherPlugin plugin = new BapSshPublisherPlugin(
                        new ArrayList<BapSshPublisher>(Collections.singletonList(publisher)), false, false, false, "master", null);

        final FreeStyleProject project = createFreeStyleProject();
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

        assertBuildStatusSuccess(project.scheduleBuild2(0).get());

        verify(mockJsch).addIdentity("TheKey", BapSshUtil.toBytes("key"), null, BapSshUtil.toBytes("passphrase"));
        verify(mockSession).connect(timeout);
        verify(mockSftp).connect(timeout);
        verify(mockSftp).cd(transfer.getRemoteDirectory());
        verify(mockSftp).cd("build-dir");
        verify(mockSftp).put((InputStream) anyObject(), eq(buildFileName));
    }

}
