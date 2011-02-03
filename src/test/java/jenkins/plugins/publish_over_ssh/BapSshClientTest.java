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
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import hudson.model.TaskListener;
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
import java.util.Calendar;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class BapSshClientTest {
    
    private static final String DIRECTORY_PATH = "a/directory/with/sub/dirs";
    private static final String DIRECTORY_PATH_WIN = "a\\directory\\with\\sub\\dirs";
    private static final String DIRECTORY = "aDirectory";
    private static final String FILENAME = "my.file";
    private static final FilePath FILE_PATH = new FilePath(new File(FILENAME));
    private static  Level originalLogLevel;
    
    @BeforeClass
    public static void before() {
        String packageName = getLoggerName();
        originalLogLevel = Logger.getLogger(packageName).getLevel();
        Logger.getLogger(packageName).setLevel(Level.OFF);
    }

    @AfterClass
    public static void after() {
        Logger.getLogger(getLoggerName()).setLevel(originalLogLevel);
    }
    
    private static String getLoggerName() {
        return BapSshClient.class.getCanonicalName();
    }
    
    private BPBuildInfo buildInfo = new BPBuildInfo(new TreeMap<String, String>(), new FilePath(new File("")), Calendar.getInstance(), TaskListener.NULL, "", new FilePath(new File("")));    
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private JSch mockJSch = mockControl.createMock(JSch.class);
    private Session mockSession = mockControl.createMock(Session.class);
    private ChannelSftp mockSftp = mockControl.createMock(ChannelSftp.class);
    private BapSshClient bapSshClient = new BapSshClient(buildInfo, mockJSch, mockSession);
    private BapSshTransfer mockTransfer = mockControl.createMock(BapSshTransfer.class);
    private InputStream anInputStream = mockControl.createMock(InputStream.class);
    private BapSshTestHelper testHelper = new BapSshTestHelper(mockControl, mockSftp);
    
    @Before public void setUp() throws Exception {
        bapSshClient.setSftp(mockSftp);
    }
    
    @Test public void testChangeDirectory() throws Exception {
        testHelper.expectDirectoryCheck(DIRECTORY_PATH, true);
        mockSftp.cd(DIRECTORY_PATH);
        assertChangeDirectory(true, DIRECTORY_PATH);
    }
    
    @Test public void testChangeDirectory_notADirectory() throws Exception {
        testHelper.expectDirectoryCheck(DIRECTORY_PATH, false);
        assertChangeDirectory(false, DIRECTORY_PATH);
    }
    
    @Test public void testChangeDirectory_cannotStat() throws Exception {
        expect(mockSftp.stat(DIRECTORY_PATH)).andThrow(new SftpException(1, "stat what? where? I ain't got no file"));
        assertChangeDirectory(false, DIRECTORY_PATH);
    }

    private void assertChangeDirectory(boolean expectedReturn, String directory) {
        mockControl.replay();
        assertEquals(expectedReturn, bapSshClient.changeDirectory(directory));
        mockControl.verify();
    }

    @Test public void testChangeDirectory_cdFails() throws Exception {
        bapSshClient.setSftp(mockSftp);
        testHelper.expectDirectoryCheck(DIRECTORY_PATH, true);
        mockSftp.cd(DIRECTORY_PATH);
        String message = "nah. not doin' that";
        expectLastCall().andThrow(new SftpException(1, message));
        testHelper.assertBPE(message, new Runnable() { public void run() {
            bapSshClient.changeDirectory(DIRECTORY_PATH);
        }});
    }
    
    @Test public void testMakeDirectory() throws Exception {
        mockSftp.mkdir(DIRECTORY);
        assertMakeDirectory(true, DIRECTORY);
    }
    
    @Test public void testMakeDirectory_refuseAttemptAtSubDirectories() throws Exception {
        assertMakeDirectory(false, DIRECTORY_PATH);
    }
    
    @Test public void testMakeDirectory_refuseAttemptAtSubDirectoriesOnWindows() throws Exception {
        assertMakeDirectory(false, DIRECTORY_PATH_WIN);
    }
    
    @Test public void testMakeDirectory_mkdirFails() throws Exception {
        mockSftp.mkdir(DIRECTORY);
        expectLastCall().andThrow(new SftpException(1, "I'm sorry, Dave. I'm afraid I can't do that."));
        assertMakeDirectory(false, DIRECTORY);
    }
    
    private void assertMakeDirectory(boolean expectedReturn, String directory) {
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
    
    @Test public void testDisconnect_notConnected() throws Exception {
        mockControl.checkOrder(false);
        expect(mockSftp.isConnected()).andReturn(false);
        expect(mockSession.isConnected()).andReturn(false);
        assertDisconnect();
    }
    
    @Test public void testDisconnect_handleNulls() throws Exception {
        bapSshClient.setSftp(null);
        expect(mockSession.isConnected()).andReturn(false);
        assertDisconnect();
    }
    
    private void assertDisconnect() throws Exception {
        mockControl.replay();
        bapSshClient.disconnect();
        mockControl.verify();
    }
    
    @Test public void testDisconnectQuietly_surpressExceptions() throws Exception {
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
    
    //    @TODO ensure that we cannot be configured without Source files and exec so that this should never occur if using the GUI
    @Test public void testBeginTransfers_failIfNoSourceFilesAndNoExecCommand() throws Exception {
        try {
            bapSshClient.beginTransfers(new BapSshTransfer("", "", "", false, false, "", 10000));
            fail();
        } catch (BapPublisherException bpe) {
            assertEquals(Messages.exception_badTransferConfig(), bpe.getMessage());
        }
    }
    
}
