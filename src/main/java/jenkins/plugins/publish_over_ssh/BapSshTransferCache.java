package jenkins.plugins.publish_over_ssh;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class allows to handle the tracking of the files transferred to the
 * remote servers so that those upload can be skipped if possible.
 *
 * The files are tracked via a per-job json cache file ("remoteResourceCache.json")
 * which contains a map of the absolute paths of the uploaded files and the values:
 * * Last access timestamp
 * * MD5 Hash value
 *
 * The files are signaled to be uploaded in two scenarios:
 * * They were not uploaded previously, they are now tracked and uploaded
 * * They were already uploaded before, only if the file has newer access timestamp and a *different* MD5 value they are uploaded
 *
 * This is done for performance reasons, large files may take a long time to compute the MD5 hash
 * so if they are not newer then there is no need to compute it because we already know that the upload
 * will be rejected.
 */
public class BapSshTransferCache {
  private static final String CACHEFILENAME = "remoteResourceCache.json";
  private HashMap<String, BapSshTransferCacheRow> data;
  private File configFile;
  private FilePath.FileCallable<File> callableFile;
  public BapSshTransferCache(FilePath configPath) {
    try {
      callableFile = new FilePath.FileCallable<File>() {
        @Override
        public File invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
          return file;
        }
        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {}
      };

      configFile = configPath.child(CACHEFILENAME).act(callableFile);

      ObjectMapper mapper = new ObjectMapper();
      data = mapper.readValue(configFile, new TypeReference<HashMap<String, BapSshTransferCacheRow>>() {});
    }
    catch ( IOException | InterruptedException ex )
    {
      data = new HashMap<>();
    }
  }

  /**
   * Writes the current memory data to the cache file as JSON
   */
  public void save() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(configFile, data);
    }
    catch ( IOException ex )
    {
      data = null;
    }
  }

  /**
   * Checks if the resources indicated by the FilePath instance
   * should be uploaded or not according to the cache
   *
   * @param filePath  path of the resource
   * @return true if the resource shall be uploaded, false otherwise
   */
  public boolean checkCachedResource( FilePath filePath ) {
    try {
      File resource = filePath.act(callableFile);
      if( !data.containsKey(resource.getAbsolutePath()) ) {
        // the resource is tracked already, register it and let i upload
        data.put( resource.getAbsolutePath(), new BapSshTransferCacheRow(resource) );
        return true;
      }

      // the resource was already tracked, check it with the cached data
      BapSshTransferCacheRow resourceEntry = data.get(resource.getAbsolutePath());
      return resourceEntry.mustUpdateWith( resource );
    }
    catch ( IOException | InterruptedException ex )
    {
      data = new HashMap<>();
      configFile = new File( CACHEFILENAME );
    }

    // in case of exception than reject the upload
    return false;
  }
}

/**
 * Class that tracks a single resources' values in the cache
 */
class BapSshTransferCacheRow {
  public byte[] HashValue;
  public long LastModified;

  /**
   * For deserialization compatibility with Jackson library
   * the fields are automatically serialized/deserialized
    */
  public BapSshTransferCacheRow() {}

  /**
   * Used to explicitly initialize a resource in the cache with
   * it's current values of md5 hash and last modified time
   *
   * @param fileRef
   */
  public BapSshTransferCacheRow(File fileRef) {
    HashValue = getHashValue(fileRef);
    try {
      LastModified = Files.getLastModifiedTime(fileRef.toPath()).toMillis();
    }
    catch (IOException ex) {
      LastModified = 0;
    }
  }

  /**
   * Calculates the MD5 hash value of a file
   *
   * @param fileRef file to be hashed
   * @return byte array of the file's hash, null in case of error
   */
  private byte[] getHashValue(File fileRef) {
    try {
      byte[] tmpBuffer = new byte[8192];

      MessageDigest md = MessageDigest.getInstance("MD5");
      try (InputStream is = Files.newInputStream(fileRef.toPath());
           DigestInputStream dis = new DigestInputStream(is, md))
      {
        while (dis.available() > 0) {
          dis.read(tmpBuffer, 0, 8192);
        }
      }
      return md.digest();
    }
    catch (IOException | NoSuchAlgorithmException ex) {
      return null;
    }
  }

  /**
   * Checks if the current instance has to be updated with the file indicated.
   * Returns true if both:
   * * last modified time for the input file is newer than the tracked one
   * * MD5 hash of the input file is different than the tracked one
   *
   * For performance reasons if the modified time checks fails, no hash is computed.
   *
   * @param fileRef  file to check
   * @return  true if the input file updated the row
   */
  public boolean mustUpdateWith(File fileRef) {
    try {
      long newModifiedTime = Files.getLastModifiedTime(fileRef.toPath()).toMillis();
      if (newModifiedTime <= LastModified)
        return false; // input files is older / current with tracked one, do not upload
    }
    catch (IOException ex)
    {
      return false;
    }

    byte[] newHashValue = getHashValue(fileRef);
    if( Arrays.equals( HashValue, newHashValue ) )
      return false; // file is newer but hash is the same, don't upload

    // Here both conditions are true so update the tracked data
    try {
      LastModified = Files.getLastModifiedTime(fileRef.toPath()).toMillis();
    }
    catch (IOException ex) {
      LastModified = 0;
    }
    HashValue = newHashValue;
    return true; // Let the file be uploaded
  }
}
