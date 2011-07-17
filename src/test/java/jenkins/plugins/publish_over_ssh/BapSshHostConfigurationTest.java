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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals" })
public class BapSshHostConfigurationTest {

    private static final String TEST_NAME = "test config";
    private static final String TEST_HOSTNAME = "test.host.name";
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_REMOTE_ROOT = "/test/root";
    private static final String TEST_PASSPHRASE = "DEFAULT";

    private static final Logger HOST_CONFIG_LOGGER = Logger.getLogger(BapSshHostConfiguration.class.getCanonicalName());
    private static Level originalLogLevel;

    @BeforeClass
    public static void before() {
        SecretHelper.setSecretKey();
        originalLogLevel = HOST_CONFIG_LOGGER.getLevel();
        HOST_CONFIG_LOGGER.setLevel(Level.OFF);
    }

    @AfterClass
    public static void after() {
        SecretHelper.clearSecretKey();
        HOST_CONFIG_LOGGER.setLevel(originalLogLevel);
    }

    @Rule
    public TemporaryFolder jenkinsHome = new TemporaryFolder();
    private final IMocksControl mockControl = EasyMock.createStrictControl();
    private final JSch mockJSch = mockControl.createMock(JSch.class);
    private final Session mockSession = mockControl.createMock(Session.class);
    private final ChannelSftp mockSftp = mockControl.createMock(ChannelSftp.class);
    private final BapSshTestHelper testHelper = new BapSshTestHelper(mockControl, mockSftp);
    private BPBuildInfo buildInfo;
    private BapSshHostConfiguration hostConfig = createWithOverrideUsernameAndPassword(mockJSch);

    @Before
    public void setUp() throws Exception {
        buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(jenkinsHome.getRoot()), null, null);
    }

    @Test public void testCreateClientWithOverridePassword() throws Exception {
        assertCreateWithDefaultInfo(null);
    }

    private BapSshClient assertCreateWithDefaultInfo(final String responseFromPwd) throws JSchException, SftpException {
        final BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null, false);
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
        final String remoteRoot = "some/directory/in/my/home/dir";
        hostConfig.setRemoteRootDir(remoteRoot);
        final BapSshClient client = assertCreateWithDefaultInfo("/usr/home/bap/" + remoteRoot);
        assertEquals("/usr/home/bap/" + remoteRoot, client.getAbsoluteRemoteRoot());
    }

    @Test public void testCreateClientWithDefaultPassword() throws Exception {
        final BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, null, null, false);
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
        assertCreateClientWithDefaultKey(false);
    }

    @Test public void testCreateClientWithGlobalExecDisabled() throws Exception {
        final BapSshClient client = assertCreateClientWithDefaultKey(true);
        assertTrue(client.isDisableExec());
    }

    private BapSshClient assertCreateClientWithDefaultKey(final boolean disableExec) throws Exception {
        final String testKey = "MyVeryBigKey";
        final BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, testKey, null, disableExec);
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
        return assertCreateClient();
    }

    @Test public void testCreateClientWithOverrideKeyPath() throws Exception {
        final String testKeyFilename = "myPrivateKey";
        final RandomFile theKey = new RandomFile(jenkinsHome.getRoot(), testKeyFilename);
        hostConfig = createWithOverrideUsernameAndPassword(mockJSch, TEST_PASSPHRASE, testKeyFilename, "");
        final BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null, false);
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
        final String testKey = "MyVeryBigKey";
        final BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, testKey,
                                                                                                    "/this/file/will/not/be/used", false);
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
        final String remoteRoot = "some/directory/in/my/home/dir";
        hostConfig.setRemoteRootDir(remoteRoot);
        final BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null, false);
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
        hostConfig.setCommonConfig(new BapSshCommonConfiguration("", "", "", false));
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        final JSchException exception = new JSchException("meh");
        mockSftp.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expectDisconnect();
        assertCreateClientThrowsException(exception);
    }

    @Test public void failToOpenSftpChanel() throws Exception {
        hostConfig.setCommonConfig(new BapSshCommonConfiguration("", "", "", false));
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        final JSchException exception = new JSchException("meh");
        expect(mockSession.openChannel("sftp")).andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(exception);
    }

    @Test public void testFailToConnect() throws Exception {
        hostConfig.setCommonConfig(new BapSshCommonConfiguration("", "", "", false));
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(TEST_PASSPHRASE);
        mockSession.setConfig((Properties) anyObject());
        final JSchException exception = new JSchException("meh");
        mockSession.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(exception);
    }

    @Test public void testFailToCreateSession() throws Exception {
        final JSchException exception = new JSchException("meh");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andThrow(exception);
        mockControl.replay();
        try {
            hostConfig.createClient(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(hostConfig.getUsername()));
            assertTrue(bpe.getMessage().contains(hostConfig.getHostname()));
            assertTrue(bpe.getMessage().contains(Integer.toString(hostConfig.getPort())));
            assertTrue(bpe.getMessage().contains(exception.getLocalizedMessage()));
        }
        mockControl.verify();
    }

    @Test public void testCreateClientWithExecDisabled() throws Exception {
        hostConfig.setDisableExec(true);
        final BapSshClient client = assertCreateWithDefaultInfo(null);
        assertTrue(client.isDisableExec());
    }

    @Test public void testDontConnectSftpIfNoSourceFilesInAnyTransfers() throws Exception {
        final BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(TEST_PASSPHRASE, null, null, false);
        hostConfig = createWithDefaultKeyInfo(mockJSch, defaultKeyInfo);
        final BapSshTransfer transfer1 = new BapSshTransfer("", "", "", "", false, false, "ls -la", 10000);
        final BapSshTransfer transfer2 = new BapSshTransfer("", "", "", "", false, false, "pwd", 10000);
        final ArrayList<BapSshTransfer> transfers = new ArrayList<BapSshTransfer>();
        transfers.addAll(Arrays.asList(transfer1, transfer2));
        final BapSshPublisher publisher = new BapSshPublisher(hostConfig.getName(), false, transfers, false, false, null, null);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(defaultKeyInfo.getPassphrase());
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        mockControl.replay();
        hostConfig.createClient(buildInfo, publisher);
        mockControl.verify();
    }

    private void assertCreateClientThrowsException(final Exception messageToInclude) throws Exception {
        assertCreateClientThrowsException(messageToInclude.getLocalizedMessage());
    }

    private void assertCreateClientThrowsException(final String exceptionMessageShouldContain) throws Exception {
        testHelper.assertBPE(exceptionMessageShouldContain, new Runnable() {
            public void run() {
                hostConfig.createClient(buildInfo);
            }
        });
    }

    private BapSshClient assertCreateClient() {
        mockControl.replay();
        final BapSshClient client = hostConfig.createClient(buildInfo);
        mockControl.verify();
        return client;
    }

    private void expectDisconnect() throws Exception {
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
    }

    private BapSshHostConfiguration createWithDefaultKeyInfo(final JSch ssh, final BapSshCommonConfiguration defaultKeyInfo) {
        final BapSshHostConfiguration config = new BapSshHostConfigurationWithMockJSch(ssh);
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
        private static final long serialVersionUID = 1L;

        private final transient JSch ssh;

        protected BapSshHostConfigurationWithMockJSch(final JSch ssh) {
            this(ssh, TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, TEST_PASSPHRASE, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT, "", "");
        }

        protected BapSshHostConfigurationWithMockJSch(final JSch ssh, final String overridePassword, final String overrideKeyPath,
                                                   final String overrideKey) {
            this(ssh, TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, overridePassword, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT,
                        overrideKeyPath, overrideKey);
        }

        @SuppressWarnings("PMD.ExcessiveParameterList")
        protected BapSshHostConfigurationWithMockJSch(final JSch ssh, final String name, final String hostname, final String username,
                                                   final String overridePassword, final String remoteRootDir, final int port,
                                                   final int timeout, final String overrideKeyPath, final String overrideKey) {
            super(name, hostname, username, overridePassword, remoteRootDir, port, timeout, true, overrideKeyPath, overrideKey, false);
            this.ssh = ssh;
        }

        @Override
        protected JSch createJSch() {
            return ssh;
        }
    }

}
