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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.SecretHelper;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import jenkins.plugins.publish_over_ssh.helper.BapSshTestHelper;
import jenkins.plugins.publish_over_ssh.helper.RandomFile;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Properties;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BapSshHostConfigurationTest {

    private static final String TEST_NAME = "test config";
    private static final String TEST_HOSTNAME = "test.host.name";
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_REMOTE_ROOT = "/test/root";
    private static final String TEST_PASSPHRASE = "DEFAULT";

    @BeforeClass
    public static void before() {
        SecretHelper.setSecretKey();
    }

    @AfterClass
    public static void after() {
        SecretHelper.clearSecretKey();
    }

    @Rule
    public TemporaryFolder jenkinsHome = new TemporaryFolder();
    private BPBuildInfo buildInfo;
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private JSch mockJSch = mockControl.createMock(JSch.class);
    private Session mockSession = mockControl.createMock(Session.class);
    private ChannelSftp mockSftp = mockControl.createMock(ChannelSftp.class);
    private BapSshTestHelper testHelper = new BapSshTestHelper(mockControl, mockSftp);
    private BapSshHostConfiguration hostConfig = createWithOverrideUsernameAndPassword(mockJSch);

    @Before
    public void setUp() throws Exception {
        buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(jenkinsHome.getRoot()), null, null);
    }

    @Test public void testCreateClientWithOverridePassword() throws Exception {
        assertCreateWithDefaultInfo(null);
    }

    private BapSshClient assertCreateWithDefaultInfo(final String responseFromPwd) throws JSchException, SftpException {
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(hostConfig.getPassword());
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        if (responseFromPwd != null)
            expect(mockSftp.pwd()).andReturn(responseFromPwd);
        return assertCreateClient();
    }

    @Test public void testCreateClientWithRelativeRemoteDir() throws Exception {
        String remoteRoot = "some/directory/in/my/home/dir";
        hostConfig.setRemoteRootDir(remoteRoot);
        BapSshClient client = assertCreateWithDefaultInfo("/usr/home/bap/" + remoteRoot);
        assertEquals("/usr/home/bap/" + remoteRoot, client.getAbsoluteRemoteRoot());
    }

    @Test public void testCreateClientWithDefaultPassword() throws Exception {
        BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, null, null);
        hostConfig = createWithDefaultKeyInfo(mockJSch, defaultKeyInfo);
        hostConfig.setPassword("Ignore me");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(defaultKeyInfo.getPassphrase());
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        assertCreateClient();
    }

    @Test public void testCreateClientWithDefaultKey() throws Exception {
        String testKey = "MyVeryBigKey";
        BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, testKey, null);
        hostConfig = createWithDefaultKeyInfo(mockJSch, defaultKeyInfo);
        hostConfig.setPassword("Ignore me");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockJSch.addIdentity(isA(String.class), aryEq(BapSshUtil.toBytes(testKey)), (byte[]) isNull(),
                    aryEq(BapSshUtil.toBytes(defaultKeyInfo.getPassphrase())));
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        assertCreateClient();
    }

    @Test public void testCreateClientWithOverrideKeyPath() throws Exception {
        String testKeyFilename = "myPrivateKey";
        RandomFile theKey = new RandomFile(jenkinsHome.getRoot(), testKeyFilename);
        hostConfig = createWithOverrideUsernameAndPassword(mockJSch, TEST_PASSPHRASE, testKeyFilename, "");
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockJSch.addIdentity(isA(String.class), aryEq(theKey.getContents()), (byte[]) isNull(),
                    aryEq(BapSshUtil.toBytes(TEST_PASSPHRASE)));
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        assertCreateClient();
    }

    @Test public void testCreateClientWillUseKeyIfKeyAndKeyPathPresent() throws Exception {
        String testKey = "MyVeryBigKey";
        BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, testKey, "/this/file/will/not/be/used");
        hostConfig = createWithDefaultKeyInfo(mockJSch, defaultKeyInfo);
        hostConfig.setPassword("Ignore me");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockJSch.addIdentity(isA(String.class), aryEq(BapSshUtil.toBytes(testKey)), (byte[]) isNull(),
                    aryEq(BapSshUtil.toBytes(TEST_PASSPHRASE)));
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        assertCreateClient();
    }

    @Test public void testCreateClientFailsIfPwdReturnsRelativePath() throws Exception {
        String remoteRoot = "some/directory/in/my/home/dir";
        hostConfig.setRemoteRootDir(remoteRoot);
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        expect(mockSftp.pwd()).andReturn("home/bap/" + remoteRoot);
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException("home/bap/" + remoteRoot);
    }

    @Test public void failToConnectSftpChanel() throws Exception {
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        JSchException exception = new JSchException("meh");
        mockSftp.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expectDisconnect();
        assertCreateClientThrowsException(exception);
    }

    @Test public void failToOpenSftpChanel() throws Exception {
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        JSchException exception = new JSchException("meh");
        expect(mockSession.openChannel("sftp")).andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(exception);
    }

    @Test public void testFailToConnect() throws Exception {
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        JSchException exception = new JSchException("meh");
        mockSession.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(exception);
    }

    @Test public void testFailToCreateSession() throws Exception {
        JSchException exception = new JSchException("meh");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andThrow(exception);
        mockControl.replay();
        try {
            hostConfig.createClient(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(hostConfig.getUsername()));
            assertTrue(bpe.getMessage().contains(hostConfig.getHostname()));
            assertTrue(bpe.getMessage().contains("" + hostConfig.getPort()));
            assertTrue(bpe.getMessage().contains(exception.getLocalizedMessage()));
        }
        mockControl.verify();
    }

    private void assertCreateClientThrowsException(final Exception messageToInclude) throws Exception {
        assertCreateClientThrowsException(messageToInclude.getLocalizedMessage());
    }

    private void assertCreateClientThrowsException(final String exceptionMessageShouldContain) throws Exception {
        testHelper.assertBPE(exceptionMessageShouldContain, new Runnable() { public void run() {
            hostConfig.createClient(buildInfo);
        } });
    }

    private BapSshClient assertCreateClient() {
        mockControl.replay();
        BapSshClient client = hostConfig.createClient(buildInfo);
        mockControl.verify();
        return client;
    }

    private void expectDisconnect() throws Exception {
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
    }

    private BapSshHostConfiguration createWithDefaultKeyInfo(final JSch ssh, final BapSshCommonConfiguration defaultKeyInfo) {
        BapSshHostConfiguration config = new BapSshHostConfigurationWithMockJSch(ssh);
        config.setCommonConfig(defaultKeyInfo);
        config.setOverrideKey(false);
        return config;
    }

    private BapSshHostConfiguration createWithOverrideUsernameAndPassword(final JSch ssh) {
        return new BapSshHostConfigurationWithMockJSch(ssh);
    }

    private BapSshHostConfiguration createWithOverrideUsernameAndPassword(final JSch ssh,
                final String overridePassword, final String overrideKeyPath, final String overrideKey) {
        return new BapSshHostConfigurationWithMockJSch(ssh, overridePassword, overrideKeyPath, overrideKey);
    }

    public static final class BapSshHostConfigurationWithMockJSch extends BapSshHostConfiguration {
        static final long serialVersionUID = 1L;

        private transient JSch ssh;

        private BapSshHostConfigurationWithMockJSch(final JSch ssh) {
            this(ssh, TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, TEST_PASSPHRASE, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT, "", "");
        }

        private BapSshHostConfigurationWithMockJSch(final JSch ssh, final String overridePassword, final String overrideKeyPath,
                                                   final String overrideKey) {
            this(ssh, TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, overridePassword, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT,
                        overrideKeyPath, overrideKey);
        }

        private BapSshHostConfigurationWithMockJSch(final JSch ssh, final String name, final String hostname, final String username,
                                                   final String overridePassword, final String remoteRootDir, final int port,
                                                   final int timeout, final String overrideKeyPath, final String overrideKey) {
            super(name, hostname, username, overridePassword, remoteRootDir, port, timeout, true, overrideKeyPath, overrideKey);
            this.ssh = ssh;
        }

        @Override
        protected JSch createJSch() {
            return ssh;
        }
    }

}
