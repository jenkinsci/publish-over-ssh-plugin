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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import jenkins.plugins.publish_over_ssh.helper.BapSshTestHelper;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods" })
public class BapSshClientTest {

    private static final String DIRECTORY_PATH = "a/directory/with/sub/dirs";
    private static final String DIRECTORY_PATH_WIN = "a\\directory\\with\\sub\\dirs";
    private static final String DIRECTORY = "aDirectory";
    private static final String FILENAME = "my.file";
    private static final FilePath FILE_PATH = new FilePath(new File(FILENAME));
    private static final Logger SSH_CLIENT_LOGGER = Logger.getLogger(BapSshClient.class.getCanonicalName());
    private static  Level originalLogLevel;

    @BeforeClass
    public static void before() {
        originalLogLevel = SSH_CLIENT_LOGGER.getLevel();
        SSH_CLIENT_LOGGER.setLevel(Level.OFF);
    }

    @AfterClass
    public static void after() {
        SSH_CLIENT_LOGGER.setLevel(originalLogLevel);
    }

    private final IMocksControl mockControl = EasyMock.createStrictControl();
    private final BPBuildInfo buildInfo = BapSshTestHelper.createEmpty(true);
    private final Session mockSession = mockControl.createMock(Session.class);
    private final Session mockSession2 = mockControl.createMock(Session.class);
    private final ChannelSftp mockSftp = mockControl.createMock(ChannelSftp.class);
    private final BapSshClient bapSshClient = new BapSshClient(buildInfo, mockSession);
    private final BapSshTransfer mockTransfer = mockControl.createMock(BapSshTransfer.class);
    private final InputStream anInputStream = mockControl.createMock(InputStream.class);
    private final BapSshTestHelper testHelper = new BapSshTestHelper(mockControl, mockSftp);

    @Before public void setUp() throws Exception {
        bapSshClient.setSftp(mockSftp);
    }

    @Test public void testGetSession() {
        assertEquals(mockSession, bapSshClient.getSession());        
    }
    
    @Test public void testAddSession() {
        bapSshClient.addSession(mockSession2);
        assertEquals(mockSession2, bapSshClient.getSession());
    }
    
    @Test public void testChangeDirectory() throws Exception {
        testHelper.expectDirectoryCheck(DIRECTORY_PATH, true);
        mockSftp.cd(DIRECTORY_PATH);
        assertChangeDirectory(true, DIRECTORY_PATH);
    }

    @Test public void testChangeDirectoryNotADirectory() throws Exception {
        testHelper.expectDirectoryCheck(DIRECTORY_PATH, false);
        assertChangeDirectory(false, DIRECTORY_PATH);
    }

    @Test public void testChangeDirectoryCannotStat() throws Exception {
        expect(mockSftp.stat(DIRECTORY_PATH)).andThrow(new SftpException(1, "stat what? where? I ain't got no file"));
        assertChangeDirectory(false, DIRECTORY_PATH);
    }

    private void assertChangeDirectory(final boolean expectedReturn, final String directory) {
        mockControl.replay();
        assertEquals(expectedReturn, bapSshClient.changeDirectory(directory));
        mockControl.verify();
    }

    @Test public void testChangeDirectoryCdFails() throws Exception {
        bapSshClient.setSftp(mockSftp);
        testHelper.expectDirectoryCheck(DIRECTORY_PATH, true);
        mockSftp.cd(DIRECTORY_PATH);
        final String message = "nah. not doin' that";
        expectLastCall().andThrow(new SftpException(1, message));
        testHelper.assertBPE(message, new Runnable() { public void run() {
            bapSshClient.changeDirectory(DIRECTORY_PATH);
        } });
    }

    @Test public void testMakeDirectory() throws Exception {
        mockSftp.mkdir(DIRECTORY);
        assertMakeDirectory(true, DIRECTORY);
    }

    @Test public void testMakeDirectoryRefuseAttemptAtSubDirectories() throws Exception {
        assertMakeDirectory(false, DIRECTORY_PATH);
    }

    @Test public void testMakeDirectoryRefuseAttemptAtSubDirectoriesOnWindows() throws Exception {
        assertMakeDirectory(false, DIRECTORY_PATH_WIN);
    }

    @Test public void testMakeDirectoryMkdirFails() throws Exception {
        mockSftp.mkdir(DIRECTORY);
        expectLastCall().andThrow(new SftpException(1, "I'm sorry, Dave. I'm afraid I can't do that."));
        assertMakeDirectory(false, DIRECTORY);
    }

    private void assertMakeDirectory(final boolean expectedReturn, final String directory) {
        mockControl.replay();
        assertEquals(expectedReturn, bapSshClient.makeDirectory(directory));
        mockControl.verify();
    }

    @Test public void testTransferFile() throws Exception {
        mockSftp.put(anInputStream, FILENAME);
        mockControl.replay();
        bapSshClient.transferFile(mockTransfer, FILE_PATH, anInputStream);
        mockControl.verify();
    }

    @Test public void testDisconnect() throws Exception {
        mockControl.checkOrder(false);
        expect(mockSftp.isConnected()).andReturn(true);
        mockSftp.disconnect();
        expect(mockSession.isConnected()).andReturn(true);
        mockSession.disconnect();
        assertDisconnect();
    }

    @Test public void testDisconnectTwoSessions() throws Exception {
        bapSshClient.addSession(mockSession2);
        mockControl.checkOrder(false);
        expect(mockSftp.isConnected()).andReturn(true);
        mockSftp.disconnect();
        expect(mockSession2.isConnected()).andReturn(true);
        mockSession2.disconnect();
        expect(mockSession.isConnected()).andReturn(true);
        mockSession.disconnect();
        assertDisconnect();
    }
    
    @Test public void testDisconnectNotConnected() throws Exception {
        mockControl.checkOrder(false);
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
        assertDisconnect();
    }

    @Test public void testDisconnectHandleNulls() throws Exception {
        bapSshClient.setSftp(null);
        expect(mockSession.isConnected()).andReturn(false);
        assertDisconnect();
    }

    private void assertDisconnect() throws Exception {
        mockControl.replay();
        bapSshClient.disconnect();
        mockControl.verify();
    }

    @Test public void testDisconnectQuietlySurpressExceptions() throws Exception {
        mockControl.checkOrder(false);
        expect(mockSftp.isConnected()).andReturn(true);
        mockSftp.disconnect();
        expectLastCall().andThrow(new RuntimeException("ooooh something went wrong innit"));
        expect(mockSession.isConnected()).andReturn(true);
        mockSession.disconnect();
        expectLastCall().andThrow(new RuntimeException("more bad stuff"));
        mockControl.replay();
        bapSshClient.disconnectQuietly();
        mockControl.verify();
    }

    @Test public void testBeginTransfersFailIfNoSourceFilesAndNoExecCommand() throws Exception {
        try {
            final int execTimeout = 10000;
            bapSshClient.beginTransfers(new BapSshTransfer("", "", "", false, false, "", execTimeout));
            fail();
        } catch (BapPublisherException bpe) {
            assertEquals(Messages.exception_badTransferConfig(), bpe.getMessage());
        }
    }

    @Test public void testBeginTransfersFailIfNoSourceFilesWhenExecDisabled() throws Exception {
        try {
            final BapSshClient noExecBapSshClient = new BapSshClient(buildInfo, mockSession, true);
            noExecBapSshClient.setSftp(mockSftp);
            final int execTimeout = 10000;
            noExecBapSshClient.beginTransfers(new BapSshTransfer("", "", "", false, false, "something to exec", execTimeout));
            fail();
        } catch (BapPublisherException bpe) {
            assertEquals(Messages.exception_badTransferConfig_noExec(), bpe.getMessage());
        }
    }

    @Test public void testEndTransfersAndCanUseEnvVars() throws Exception {
        final String command = "ls -ltr /var/log/$BUILD_NUMBER*";
        buildInfo.getEnvVars().put("BUILD_NUMBER", "42");
        final String expectedCommand = "ls -ltr /var/log/42*";
        final int expectedConnectTimeout = 30000;
        final int exitStatus = 0;
        final int pollsBeforeClosed = 10;
        final TestExec exec = new TestExec(expectedCommand, expectedConnectTimeout, exitStatus, pollsBeforeClosed);
        expect(mockSession.openChannel("exec")).andReturn(exec);
        expect(mockSession.getTimeout()).andReturn(expectedConnectTimeout);
        mockControl.replay();
        final int execCommandTimeout = 120000;
        bapSshClient.endTransfers(new BapSshTransfer("", "", "", false, false, command, execCommandTimeout));
        mockControl.verify();
        exec.assertMethodsCalled();
    }

    @Test public void testEndTransfersThrowsExceptionIfCommandFailed() throws Exception {
        final String command = "ls -ltr /var/log";
        final int expectedConnectTimeout = 30000;
        final int expectedExitStatus = 44;
        final int pollsBeforeClosed = 10;
        final TestExec exec = new TestExec(command, expectedConnectTimeout, expectedExitStatus, pollsBeforeClosed);
        expect(mockSession.openChannel("exec")).andReturn(exec);
        expect(mockSession.getTimeout()).andReturn(expectedConnectTimeout);
        final int execCommandTimeout = 120000;
        testHelper.assertBPE(Integer.toString(expectedExitStatus), new Runnable() { public void run() {
            bapSshClient.endTransfers(new BapSshTransfer("", "", "", false, false, command, execCommandTimeout));
        } });
        exec.assertMethodsCalled();
        assertFalse(exec.isUsePty());
    }

    @Test public void testEndTransfersThrowsExceptionIfCommandTimesOut() throws Exception {
        final String command = "ls -ltr /var/log";
        final int connectTimeout = 30000;
        final int exitStatus = 44;
        // ~ 40s
        final int pollsBeforeClosed = 200;
        final TestExec exec = new TestExec(command, connectTimeout, exitStatus, pollsBeforeClosed);
        expect(mockSession.openChannel("exec")).andReturn(exec);
        expect(mockSession.getTimeout()).andReturn(connectTimeout);
        final long start = System.currentTimeMillis();
        final int shortExecTimeout = 2000;
        testHelper.assertBPE("timed out", new Runnable() { public void run() {
            bapSshClient.endTransfers(new BapSshTransfer("", "", "", false, false, command, shortExecTimeout));
        } });
        final long duration = System.currentTimeMillis() - start;
        // expect to return in 2s + some overhead 4 test and pre thread prod code + very slow machines.
        // @ 10s this should never fail ...
        final int lenientMaxExecutionTime = 10000;
        assertTrue(duration < lenientMaxExecutionTime);
        exec.assertMethodsCalled();
    }

    @Test public void testEndTransfersDoesNothingIfNoExecCommand() throws Exception {
        final int execTimeout = 10000;
        mockControl.replay();
        bapSshClient.endTransfers(new BapSshTransfer("*.java", "", "", false, false, "", execTimeout));
        mockControl.verify();
    }

    @Test public void testEndTransfersDoesNothingIfExecDisabled() throws Exception {
        final BapSshClient noExecBapSshClient = new BapSshClient(buildInfo, mockSession, true);
        noExecBapSshClient.setSftp(mockSftp);
        final int execTimeout = 10000;
        mockControl.replay();
        noExecBapSshClient.endTransfers(new BapSshTransfer("*.java", "", "", false, false, "something fun to exec", execTimeout));
        mockControl.verify();
    }

    @Test public void testCanExecInPty() throws Exception {
        final String command = "n/a";
        final int timeout = 20000;
        final int pollsBeforeClosed = 1;
        final TestExec exec = new TestExec(command, timeout, 0, pollsBeforeClosed);
        expect(mockSession.openChannel("exec")).andReturn(exec);
        expect(mockSession.getTimeout()).andReturn(timeout);
        mockControl.replay();
        bapSshClient.endTransfers(new BapSshTransfer("", "", "", "", false, false, command, timeout, true, false, false, null));
        assertTrue(exec.isUsePty());
        assertFalse(exec.isUseAgentForwarding());
    }

    @Test public void testUseAgentForwarding() throws Exception {
        final String command = "n/a";
        final int timeout = 20000;
        final int pollsBeforeClosed = 1;
        final TestExec exec = new TestExec(command, timeout, 0, pollsBeforeClosed);
        expect(mockSession.openChannel("exec")).andReturn(exec);
        expect(mockSession.getTimeout()).andReturn(timeout);
        mockControl.replay();
        BapSshTransfer transfer = new BapSshTransfer("", "", "", "", false, false, command, timeout, true, false, false, null);
        transfer.setUseAgentForwarding(true);
        bapSshClient.endTransfers(transfer);
        assertTrue(exec.isUsePty());
        assertTrue(exec.isUseAgentForwarding());
    }

    public static class TestExec extends ChannelExec {
        private final String expectedCommand;
        private final int expectedTimeout;
        private final int exitStatus;
        private int pollsBeforeClosed;
        private boolean setCommandCalled, connectCalled;
        private boolean usePty;
        public TestExec(final String expectedCommand, final int expectedConnectTimeout, final int exitStatus, final int pollsBeforeClosed) {
            this.expectedCommand = expectedCommand;
            this.expectedTimeout = expectedConnectTimeout;
            this.exitStatus = exitStatus;
            this.pollsBeforeClosed = pollsBeforeClosed;
        }
        public boolean isConnected() {
            return false;
        }
        public synchronized boolean isClosed() { // NOPMD
            return --pollsBeforeClosed < 0;
        }
        public void setInputStream(final InputStream inputStream) { }
        public void setOutputStream(final OutputStream outputStream, final boolean dontClose) {
            assertTrue(dontClose);
        }
        public void setErrStream(final OutputStream outputStream, final boolean dontClose) {
            assertTrue(dontClose);
        }
        public void setCommand(final String command) {
            setCommandCalled = true;
            assertEquals(expectedCommand, command);
        }
        public void connect(final int timeout) {
            connectCalled = true;
            assertEquals(expectedTimeout, timeout);
        }
        public int getExitStatus() {
            return exitStatus;
        }
        public void assertMethodsCalled() {
            if (!setCommandCalled || !connectCalled) fail();
        }
        public void setPty(final boolean usePty) {
            this.usePty = usePty;
        }
        public boolean isUsePty() {
            return usePty;
        }
        public boolean isUseAgentForwarding() {
            return agent_forwarding;
        }
    }

}
