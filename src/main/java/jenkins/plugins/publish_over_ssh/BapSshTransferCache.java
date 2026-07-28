package jenkins.plugins.publish_over_ssh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.Roles;
import org.jenkinsci.remoting.RoleChecker;

/**
 * Tracks files transferred to remote servers so unchanged files can be skipped.
 *
 * <p>The cache is stored below {@code JENKINS_HOME}, while source files normally live on an agent. All file
 * operations therefore have to run on the node that owns the corresponding {@link FilePath}; returning a plain
 * {@link File} across remoting loses that node information.
 */
public class BapSshTransferCache {
    private static final Logger LOGGER = Logger.getLogger(BapSshTransferCache.class.getName());
    private static final String CACHE_FILE_NAME = "remoteResourceCache.json";

    private final FilePath configFile;
    private HashMap<String, BapSshTransferCacheRow> data;

    public BapSshTransferCache(final FilePath configPath) {
        configFile = configPath.child(CACHE_FILE_NAME);
        try {
            data = configFile.act(new ReadCache());
            if (data == null) {
                data = new HashMap<>();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to read the Publish Over SSH transfer cache; all files will be uploaded", ex);
            data = new HashMap<>();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while reading the Publish Over SSH transfer cache", ex);
            data = new HashMap<>();
        }
    }

    /** Writes the in-memory cache on the node that owns the cache file. Cache failures must not fail a transfer. */
    public void save() {
        try {
            configFile.act(new WriteCache(data));
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to save the Publish Over SSH transfer cache", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while saving the Publish Over SSH transfer cache", ex);
        }
    }

    /**
     * Checks whether the resource should be uploaded. File metadata and the digest are calculated on the node that
     * owns the source file. If the cache cannot be checked, the safe behavior is to upload the file.
     */
    public boolean checkCachedResource(final FilePath filePath) {
        try {
            final BapSshTransferCacheMetadata metadata = filePath.act(new ReadMetadata());
            final BapSshTransferCacheRow cached = data.get(metadata.path);
            if (cached != null && metadata.lastModified <= cached.LastModified) {
                return false;
            }

            final BapSshTransferCacheResource resource = filePath.act(new ReadResource());
            if (cached == null) {
                data.put(resource.path, new BapSshTransferCacheRow(resource));
                return true;
            }
            return cached.mustUpdateWith(resource);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Unable to check the Publish Over SSH transfer cache; uploading the file", ex);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while checking the Publish Over SSH transfer cache; uploading the file", ex);
            return true;
        }
    }

    private static final class ReadCache implements FilePath.FileCallable<HashMap<String, BapSshTransferCacheRow>> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public HashMap<String, BapSshTransferCacheRow> invoke(final File file, final VirtualChannel channel)
                throws IOException {
            if (!file.isFile()) {
                return new HashMap<>();
            }
            return new ObjectMapper().readValue(file, new TypeReference<>() {});
        }

        @Override
        public void checkRoles(final RoleChecker checker) throws SecurityException {
            checker.check(this, Roles.MASTER);
        }
    }

    private static final class WriteCache implements FilePath.FileCallable<Void> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final HashMap<String, BapSshTransferCacheRow> data;

        private WriteCache(final HashMap<String, BapSshTransferCacheRow> data) {
            this.data = data;
        }

        @Override
        public Void invoke(final File file, final VirtualChannel channel) throws IOException {
            final File parent = file.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            new ObjectMapper().writeValue(file, data);
            return null;
        }

        @Override
        public void checkRoles(final RoleChecker checker) throws SecurityException {
            checker.check(this, Roles.MASTER);
        }
    }

    private static final class ReadResource implements FilePath.FileCallable<BapSshTransferCacheResource> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public BapSshTransferCacheResource invoke(final File file, final VirtualChannel channel) throws IOException {
            return BapSshTransferCacheResource.from(file);
        }

        @Override
        public void checkRoles(final RoleChecker checker) throws SecurityException {
            checker.check(this, Roles.SLAVE);
        }
    }

    private static final class ReadMetadata implements FilePath.FileCallable<BapSshTransferCacheMetadata> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public BapSshTransferCacheMetadata invoke(final File file, final VirtualChannel channel) throws IOException {
            return BapSshTransferCacheMetadata.from(file);
        }

        @Override
        public void checkRoles(final RoleChecker checker) throws SecurityException {
            checker.check(this, Roles.SLAVE);
        }
    }
}

/** Serializable source metadata used to avoid hashing files that have not changed. */
final class BapSshTransferCacheMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    final String path;
    final long lastModified;

    private BapSshTransferCacheMetadata(final String path, final long lastModified) {
        this.path = path;
        this.lastModified = lastModified;
    }

    static BapSshTransferCacheMetadata from(final File file) throws IOException {
        return new BapSshTransferCacheMetadata(
                file.getAbsolutePath(), Files.getLastModifiedTime(file.toPath()).toMillis());
    }
}

/** A serializable snapshot of a source file, calculated on the node where the source file resides. */
final class BapSshTransferCacheResource implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    final String path;
    final byte[] hashValue;
    final long lastModified;

    private BapSshTransferCacheResource(final String path, final byte[] hashValue, final long lastModified) {
        this.path = path;
        this.hashValue = hashValue;
        this.lastModified = lastModified;
    }

    static BapSshTransferCacheResource from(final File file) throws IOException {
        return new BapSshTransferCacheResource(
                file.getAbsolutePath(), calculateHash(file), Files.getLastModifiedTime(file.toPath()).toMillis());
    }

    private static byte[] calculateHash(final File file) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("MD5 is not available", ex);
        }

        try (InputStream input = Files.newInputStream(file.toPath());
                DigestInputStream digestInput = new DigestInputStream(input, digest)) {
            final byte[] buffer = new byte[8192];
            while (digestInput.read(buffer) != -1) {
                // DigestInputStream updates the digest while the stream is read.
            }
        }
        return digest.digest();
    }
}

/** A cache entry retained in its original JSON shape for compatibility with existing cache files. */
class BapSshTransferCacheRow implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public byte[] HashValue;

    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Backwards compatibility")
    public long LastModified;

    /** Used by Jackson when loading existing cache files. */
    public BapSshTransferCacheRow() {}

    BapSshTransferCacheRow(final BapSshTransferCacheResource resource) {
        HashValue = resource.hashValue;
        LastModified = resource.lastModified;
    }

    boolean mustUpdateWith(final BapSshTransferCacheResource resource) {
        if (resource.lastModified <= LastModified) {
            return false;
        }
        if (Arrays.equals(HashValue, resource.hashValue)) {
            LastModified = resource.lastModified;
            return false;
        }
        HashValue = resource.hashValue;
        LastModified = resource.lastModified;
        return true;
    }
}
