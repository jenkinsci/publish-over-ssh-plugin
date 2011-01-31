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
import com.jcraft.jsch.UserInfo;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import java.io.File;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BapSshHostConfigurationTest {
    
    private Map<String, String> envVars = new TreeMap<String, String>();
    private BPBuildInfo buildInfo = new BPBuildInfo(envVars, new FilePath(new File("aBaseDir")), Calendar.getInstance(), TaskListener.NULL, "");
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private JSch mockJSch = mockControl.createMock(JSch.class);
    private Session mockSession = mockControl.createMock(Session.class);
    private ChannelSftp mockSftp = mockControl.createMock(ChannelSftp.class);
    
    @Test public void testCreateClient() throws Exception {
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setUserInfo((UserInfo) anyObject());
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        mockSftp.connect(hostConfig.getTimeout());
        mockControl.replay();
        hostConfig.createClient(buildInfo);
        mockControl.verify();
    }
    
    @Test public void failToConnectSftpChanel() throws Exception {
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setUserInfo((UserInfo) anyObject());
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        expect(mockSession.openChannel("sftp")).andReturn(mockSftp);
        JSchException exception = new JSchException("meh");
        mockSftp.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
        mockControl.replay();
        try {
            hostConfig.createClient(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(exception.getLocalizedMessage()));
        }
        mockControl.verify();
    }
    
    @Test public void failToOpenSftpChanel() throws Exception {
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setUserInfo((UserInfo) anyObject());
        mockSession.setConfig((Properties) anyObject());
        mockSession.connect(hostConfig.getTimeout());
        JSchException exception = new JSchException("meh");
        expect(mockSession.openChannel("sftp")).andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        mockControl.replay();
        try {
            hostConfig.createClient(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(exception.getLocalizedMessage()));
        }
        mockControl.verify();
    }
    
    @Test public void testFailToConnect() throws Exception {
        BapSshHostConfiguration hostConfig = new BapSshHostConfigurationWithMockJSch();
        expect(mockJSch.getSession(hostConfig.getUsername(), hostConfig.getHostname(), hostConfig.getPort())).andReturn(mockSession);
        mockSession.setUserInfo((UserInfo) anyObject());
        mockSession.setConfig((Properties) anyObject());
        JSchException exception = new JSchException("meh");
        mockSession.connect(hostConfig.getTimeout());
        expectLastCall().andThrow(exception);
        expect(mockSession.isConnected()).andReturn(false);
        mockControl.replay();
        try {
            hostConfig.createClient(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(exception.getLocalizedMessage()));
        }
        mockControl.verify();
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
    
    
    public class BapSshHostConfigurationWithMockJSch extends BapSshHostConfiguration {
        
        private static final String TEST_NAME = "test config";
        private static final String TEST_HOSTNAME = "test.host.name";
        private static final String TEST_USERNAME = "testUser";
        private static final String TEST_PASSWORD = "test pass";
        private static final String TEST_REMOTE_ROOT = "/test/root";
        
        public BapSshHostConfigurationWithMockJSch() {
            this(TEST_NAME, TEST_HOSTNAME, TEST_USERNAME, TEST_PASSWORD, TEST_REMOTE_ROOT, DEFAULT_PORT, DEFAULT_TIMEOUT);
        }

        public BapSshHostConfigurationWithMockJSch(String name, String hostname, String username, String password, String remoteRootDir, int port, int timeout) {
            super(name, hostname, username, password, remoteRootDir, port, timeout, false, "", "");
        }

        @Override
        protected JSch createJSch() {
            return mockJSch;
        }
    }
    
}
