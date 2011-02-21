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

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BapSshHostConfigurationTest {
    
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
    private Map<String, String> envVars = new TreeMap<String, String>();
    private BPBuildInfo buildInfo;
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private JSch mockJSch = mockControl.createMock(JSch.class);
    private Session mockSession = mockControl.createMock(Session.class);
    private ChannelSftp mockSftp = mockControl.createMock(ChannelSftp.class);
    private BapSshTestHelper testHelper = new BapSshTestHelper(mockControl, mockSftp);
    
    @Before
    public void setUp() throws Exception {
        buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(jenkinsHome.getRoot()), null, null);
    }
    
    @Test public void testCreateClientWithOverridePassword() throws Exception {
        String testPass = "TEST PASSWORD";
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(testPass);
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(testPass);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void testCreateClientWithDefaultPassword() throws Exception {
        String testPass = "TEST PASSWORD";
        BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(testPass, null, null);
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(defaultKeyInfo);
        hostConfig.setPassword("Ignore me");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(testPass);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void testCreateClientWithDefaultKey() throws Exception {
        String testPass = "TEST PASSWORD";
        String testKey = "MyVeryBigKey";
        BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(testPass, testKey, null);
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(defaultKeyInfo);
        hostConfig.setPassword("Ignore me");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockJSch.addIdentity(isA(String.class), aryEq(BapSshUtil.toBytes(testKey)), (byte[])isNull(), aryEq(BapSshUtil.toBytes(testPass)));
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void testCreateClientWithOverrideKeyPath() throws Exception {
        String testPass = "TEST PASSWORD";
        String testKeyFilename = "myPrivateKey";
        RandomFile theKey = new RandomFile(jenkinsHome.getRoot(), testKeyFilename);
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(testPass, testKeyFilename, "");
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockJSch.addIdentity(isA(String.class), aryEq(theKey.getContents()), (byte[])isNull(), aryEq(BapSshUtil.toBytes(testPass)));
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void testCreateClientWillUseKeyIfKeyAndKeyPathPresent() throws Exception {
        String testPass = "TEST PASSWORD";
        String testKey = "MyVeryBigKey";
        BapSshCommonConfiguration defaultKeyInfo = new BapSshCommonConfiguration(testPass, testKey, "/this/file/will/not/be/used");
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(defaultKeyInfo);
        hostConfig.setPassword("Ignore me");
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockJSch.addIdentity(isA(String.class), aryEq(BapSshUtil.toBytes(testKey)), (byte[])isNull(), aryEq(BapSshUtil.toBytes(testPass)));
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void testCreateClientWithRelativeRemoteDir() throws Exception {
        String testPass = "TEST PASSWORD";
        String remoteRoot = "some/directory/in/my/home/dir";
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(testPass);
        hostConfig.setRemoteRootDir(remoteRoot);
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(testPass);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        expect(mockSftp.pwd()).andReturn("/usr/home/bap/" + remoteRoot);
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void testCreateClientFailsIfPwdReturnsRelativePath() throws Exception {
        String testPass = "TEST PASSWORD";
        String remoteRoot = "some/directory/in/my/home/dir";
        final BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch(testPass);
        hostConfig.setRemoteRootDir(remoteRoot);
        BapSshCommonConfiguration commonConfiguration = new BapSshCommonConfiguration("Ignore me", null, null);
        hostConfig.setCommonConfig(commonConfiguration);
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword(testPass);
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        testHelper.expectDirectoryCheck(hostConfig.getRemoteRootDir(), true);
        mockSftp.cd(hostConfig.getRemoteRootDir());
        expect(mockSftp.pwd()).andReturn("home/bap/" + remoteRoot);
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(hostConfig, "home/bap/" + remoteRoot);
    }
    
    @Test public void failToConnectSftpChanel() throws Exception {
        final BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword("");
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        JSchException exception = new JSchException("meh");
        mockSftp.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expectDisconnect();
        assertCreateClientThrowsException(hostConfig, exception);
    }
    
    @Test public void failToOpenSftpChanel() throws Exception {
        final BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword("");
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        JSchException exception = new JSchException("meh");
        expect(mockSession.openChannel("sftp")).andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(hostConfig, exception);
    }
    
    @Test public void testFailToConnect() throws Exception {
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setPassword("");
        mockSession.setConfig((Properties) anyObject());
        JSchException exception = new JSchException("meh");
        mockSession.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        assertCreateClientThrowsException(hostConfig, exception);
    }
    
    @Test public void testFailToCreateSession() throws Exception {
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
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
    
    private void assertCreateClientThrowsException(BapSshHostConfiguration hostConfig, Exception messageToInclude) throws Exception {
        assertCreateClientThrowsException(hostConfig, messageToInclude.getLocalizedMessage());
    }
    
    private void assertCreateClientThrowsException(final BapSshHostConfiguration hostConfig, String exceptionMessageShouldContain) throws Exception {
        testHelper.assertBPE(exceptionMessageShouldContain, new Runnable() { public void run() {
            hostConfig.createClient(buildInfo);
        }});
    }
    
    private void expectDisconnect() throws Exception {
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
    }
    
    public class BapSshHostConfigurationWithMockJSch extends BapSshHostConfiguration {
        
        private static final String TEST_NAME = "test config";
        private static final String TEST_HOSTNAME = "test.host.name";
        private static final String TEST_USERNAME = "testUser";
        private static final String TEST_REMOTE_ROOT = "/test/root";
        
        public BapSshHostConfigurationWithMockJSch(final BapSshCommonConfiguration defaultKeyInfo) {
            this();
            setOverrideKey(false);
            setCommonConfig(defaultKeyInfo);
        }
        
        public BapSshHostConfigurationWithMockJSch(final String overridePassword) {
            super(TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, overridePassword, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT, true, null, null);
        }
        
        public BapSshHostConfigurationWithMockJSch(final String overridePassword, final String overrideKeyPath, final String overrideKey) {
            super(TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, overridePassword, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT, true, overrideKeyPath, overrideKey);
        }
        
        public BapSshHostConfigurationWithMockJSch() {
            this(TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, null, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT);
        }

        public BapSshHostConfigurationWithMockJSch(final String name, final String hostname, final String username, final String password,
                                                   final String remoteRootDir, final int port, final int timeout) {
            super(name, hostname, username, password, remoteRootDir, port, timeout, true, "", "");
        }

        @Override
        protected JSch createJSch() {
            return mockJSch;
        }
    }
    
}
