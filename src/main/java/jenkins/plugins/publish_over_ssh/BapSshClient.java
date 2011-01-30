package jenkins.plugins.publish_over_ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import hudson.FilePath;
import jenkins.plugins.publish_over.BPDefaultClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

public class BapSshClient extends BPDefaultClient<BapSshTransfer> {
    
    private static Log LOG = LogFactory.getLog(BapSshClient.class);
    
    private JSch ssh;
    private Session session;
    private ChannelSftp sftp;

    public BapSshClient(JSch ssh, Session session) {
        this.ssh = ssh;
        this.session = session;
    }
    
    public void setSftp(ChannelSftp sftp) {
        this.sftp = sftp;
    }

    public boolean changeToInitialDirectory() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean changeDirectory(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean makeDirectory(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void transferFile(BapSshTransfer bapSshTransfer, FilePath filePath, InputStream inputStream) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void disconnect() throws Exception {
        disconnectSftp();
        disconnectSession();
    }
    
    private void disconnectSftp() {
        if (sftp == null) return;
        if (sftp.isConnected())
            sftp.disconnect();
    }
    
    private void disconnectSession() {
        if (session == null) return;
        if (session.isConnected())
                session.disconnect();
    }

    public void disconnectQuietly() {
        try {
            disconnectSftp();
        } catch (Exception e) {
            LOG.warn(Messages.exception_disconnect_sftp(e.getLocalizedMessage()));
        }
        try {
            disconnectSession();
        } catch (Exception e) {
            LOG.warn(Messages.exception_disconnect_session(e.getLocalizedMessage()));
        }
    }
}
