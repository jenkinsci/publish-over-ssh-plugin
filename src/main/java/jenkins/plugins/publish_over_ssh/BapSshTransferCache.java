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
import java.util.Map;

public class BapSshTransferCache {
  private static final String CACHEFILENAME = "remoteResourceCache.json";
  private HashMap<String, BapSshTransferCacheRow> data;
  private File configFile;
  private FilePath.FileCallable<File> callableFile;
  public BapSshTransferCache(FilePath configPath) {
    try {
      callableFile = new FilePath.FileCallable<>() {
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

  public boolean checkCachedResource( FilePath filePath ) {
    try {
      File resource = filePath.act(callableFile);
      if( !data.containsKey(resource.getAbsolutePath()) ) {
        data.put( resource.getAbsolutePath(), new BapSshTransferCacheRow(resource) );
        return true;
      }

      BapSshTransferCacheRow resourceEntry = data.get(resource.getAbsolutePath());
      return resourceEntry.mustUpdateWith( resource );
    }
    catch ( IOException | InterruptedException ex )
    {
      data = new HashMap<>();
      configFile = new File( CACHEFILENAME ); // todo check
    }

    return false;
  }
}

class BapSshTransferCacheRow {
  public byte[] HashValue;
  public long   LastAccess;

  public BapSshTransferCacheRow() {
    // For deserialization with Jackson library
  }

  public BapSshTransferCacheRow(File fileRef) {
    HashValue = getHashValue(fileRef);
    try {
      LastAccess = Files.getLastModifiedTime(fileRef.toPath()).toMillis();
    }
    catch (IOException ex) {
      LastAccess = 0;
    }
  }

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

  public boolean mustUpdateWith(File fileRef) {
    try {
      long newAccessTime = Files.getLastModifiedTime(fileRef.toPath()).toMillis();
      if (newAccessTime <= LastAccess)
        return false;
    }
    catch (IOException ex)
    {
      return false;
    }

    byte[] newHashValue = getHashValue(fileRef);
    if( Arrays.equals( HashValue, newHashValue ) )
      return false;

    try {
      LastAccess = Files.getLastModifiedTime(fileRef.toPath()).toMillis();
    }
    catch (IOException ex) {
      LastAccess = 0;
    }
    HashValue = newHashValue;
    return true;
  }
}
