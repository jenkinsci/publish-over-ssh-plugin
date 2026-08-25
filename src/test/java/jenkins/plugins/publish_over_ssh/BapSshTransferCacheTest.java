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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import java.io.IOException;
import java.io.Serial;
import java.util.HashMap;
import jenkins.security.MasterToSlaveCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
@WithJenkins
class BapSshTransferCacheTest {

    @TempDir
    private java.io.File temporaryDirectory;

    @Test
    void savesAndReloadsCache() throws Exception {
        final FilePath cacheDirectory = new FilePath(temporaryDirectory);
        final FilePath source = cacheDirectory.child("source.txt");
        source.write("first version", "UTF-8");

        final BapSshTransferCache firstCache = new BapSshTransferCache(cacheDirectory);
        assertTrue(firstCache.checkCachedResource(source));
        firstCache.save();

        final BapSshTransferCache reloadedCache = new BapSshTransferCache(cacheDirectory);
        assertFalse(reloadedCache.checkCachedResource(source));

        source.write("second version", "UTF-8");
        source.touch(System.currentTimeMillis() + 2000);
        assertTrue(reloadedCache.checkCachedResource(source));
    }

    @Test
    void malformedCacheDoesNotPreventUploadOrSave() throws Exception {
        final FilePath cacheDirectory = new FilePath(temporaryDirectory);
        cacheDirectory.child("remoteResourceCache.json").write("not-json", "UTF-8");
        final FilePath source = cacheDirectory.child("source.txt");
        source.write("content", "UTF-8");

        final BapSshTransferCache cache = new BapSshTransferCache(cacheDirectory);
        assertTrue(cache.checkCachedResource(source));
        assertDoesNotThrow(cache::save);
        assertTrue(cacheDirectory.child("remoteResourceCache.json").length() > 0);
    }

    @Test
    void runtimeCacheFailuresDoNotFailTransfer() throws Exception {
        final FilePath cacheDirectory = mock(FilePath.class);
        final FilePath cacheFile = mock(FilePath.class);
        when(cacheDirectory.child("remoteResourceCache.json")).thenReturn(cacheFile);
        doReturn(new HashMap<String, BapSshTransferCacheRow>())
                .doThrow(new SecurityException("save denied"))
                .when(cacheFile)
                .act(anyFileCallable());

        final BapSshTransferCache cache = new BapSshTransferCache(cacheDirectory);
        final FilePath source = mock(FilePath.class);
        doThrow(new SecurityException("read denied")).when(source).act(anyFileCallable());

        assertTrue(cache.checkCachedResource(source));
        assertDoesNotThrow(cache::save);
    }

    @Test
    void cacheOnControllerCanBeUsedWhilePublishingOnAgent(final JenkinsRule j) throws Exception {
        final FilePath controllerRoot = j.jenkins.getRootPath();
        assertNotNull(controllerRoot);
        final FilePath cacheDirectory = controllerRoot.child("transfer-cache-test");
        cacheDirectory.mkdirs();

        final DumbSlave agent = j.createOnlineSlave();
        assertNotNull(agent);
        final FilePath agentRoot = agent.getRootPath();
        assertNotNull(agentRoot);
        final FilePath source = agentRoot.child("source.txt");
        source.write("agent content", "UTF-8");
        final Computer computer = agent.toComputer();
        assertNotNull(computer);
        final VirtualChannel channel = computer.getChannel();
        assertNotNull(channel);

        assertTrue(channel.call(new CheckCacheOnAgent(cacheDirectory, source)));
        assertTrue(cacheDirectory.child("remoteResourceCache.json").exists());
        assertFalse(channel.call(new CheckCacheOnAgent(cacheDirectory, source)));
    }

    private static final class CheckCacheOnAgent extends MasterToSlaveCallable<Boolean, IOException> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final FilePath cacheDirectory;
        private final FilePath source;

        private CheckCacheOnAgent(final FilePath cacheDirectory, final FilePath source) {
            this.cacheDirectory = cacheDirectory;
            this.source = source;
        }

        @Override
        public Boolean call() throws IOException {
            final BapSshTransferCache cache = new BapSshTransferCache(cacheDirectory);
            final boolean shouldUpload = cache.checkCachedResource(source);
            cache.save();
            return shouldUpload;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> FilePath.FileCallable<T> anyFileCallable() {
        return (FilePath.FileCallable<T>) any(FilePath.FileCallable.class);
    }
}
