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

package jenkins.plugins.publish_over_ssh.helper;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.plugins.publish_over.BPBuildEnv;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BapPublisherException;
import org.easymock.classextension.IMocksControl;

import java.io.File;
import java.util.Calendar;
import java.util.TreeMap;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BapSshTestHelper {

    public static BPBuildEnv createEmptyBuildEnv() {
        return new BPBuildEnv(new TreeMap<String, String>(), new FilePath(new File("")), Calendar.getInstance());
    }

    public static BPBuildInfo createEmpty() {
        return createEmpty(true);
    }

    public static BPBuildInfo createEmpty(final boolean setEffectiveEnvironment) {
        final BPBuildInfo buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), createEmptyBuildEnv(), null);
        if (setEffectiveEnvironment) {
            buildInfo.setBuildTime(buildInfo.getCurrentBuildEnv().getBuildTime());
            buildInfo.setBaseDirectory(buildInfo.getCurrentBuildEnv().getBaseDirectory());
            buildInfo.setEnvVars(buildInfo.getCurrentBuildEnv().getEnvVars());
        }
        return buildInfo;
    }


    private final IMocksControl mockControl;
    private final ChannelSftp mockSftp;

    public BapSshTestHelper(final IMocksControl mockControl, final ChannelSftp mockSftp) {
        this.mockControl = mockControl;
        this.mockSftp = mockSftp;
    }

    public void expectDirectoryCheck(final String directory, final boolean isDirectory) throws SftpException {
        final SftpATTRS mockAttrs = mockControl.createMock(SftpATTRS.class);
        expect(mockSftp.stat(directory)).andReturn(mockAttrs);
        expect(mockAttrs.isDir()).andReturn(isDirectory);
    }

    public void assertBPE(final String message, final Runnable toExec) {
        mockControl.replay();
        try {
            toExec.run();
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(message));
        }
        mockControl.verify();
    }

}
