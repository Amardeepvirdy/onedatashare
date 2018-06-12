package stork.module.ftpNew;

import stork.cred.StorkUserinfo;
import stork.feather.*;
import stork.feather.errors.AuthenticationRequired;
import stork.feather.util.ThreadBell;
import stork.module.Module;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.net.ftp.*;
import stork.util.Log;

/** A module for SFTP/SFTP file transfers. */
public class FTPNewModule extends Module<FTPNewResource> {
  {
    name("Stork FTP Module");
    protocols("ftp");
    description("A module for FTP file transfers.");
  }

  public FTPNewResource select(URI uri, Credential credential) {
    URI ep = uri.endpointURI();
    return new FTPNewSession(ep, credential).select(uri.path());
  }
}

class FTPNewSession extends Session<FTPNewSession, FTPNewResource> {
  transient FTPClient client;

  private transient String host, username, password;
  private transient int port = 21;

  /** Create an SFTPSession. */
  public FTPNewSession(URI uri, Credential credential) {
    super(uri, credential);
  }

  /** Get an SFTPResource. */
  public FTPNewResource select(Path path) {
    return new FTPNewResource(this, path);
  }

  /** Connect to the remote server. */
  public Bell<FTPNewSession> initialize() {
    host = uri.host();
    if (host == null)
      throw new RuntimeException("No hostname provided.");

    if (uri.port() > 0)
      port = uri.port();

    String[] ui = null;

    if (credential == null)
      ui = uri.userPass();
    else if (credential instanceof StorkUserinfo)
      ui = ((StorkUserinfo) credential).data();
    if (ui == null || ui.length != 2)
      throw new RuntimeException("Invalid credential.");

    username = ui[0];
    password = ui[1];
    if(username == null || password == null)
        throw new AuthenticationRequired("userinfo");
    // Do the actual connection on a new thread.
    return new ThreadBell<Void>() {
      public Void run() throws Exception {
        client = new FTPClient();
        client.connect(host);
        client.enterLocalPassiveMode();
        if(!client.login(username, password)){
          client.logout();
          throw new FTPConnectionClosedException("Unsuccessful login");
        }
        int reply = client.getReplyCode();
        if(!FTPReply.isPositiveCompletion(reply)){
          client.disconnect();
          throw new FTPConnectionClosedException("Unsuccessful connection");
        }
        client.setFileType(FTP.BINARY_FILE_TYPE);
        return null;
      }
    }.start().as(this);
  }

  protected void cleanup() {
    if (client.isConnected())
      try {
        client.disconnect();
      } catch (IOException e) {
        e.printStackTrace();
      }
    client = null;
  }
}

class FTPNewResource extends Resource<FTPNewSession, FTPNewResource> {
  public FTPNewResource(FTPNewSession session, Path path) {
    super(session, path);
  }

  public Bell<Stat> stat() {
    return new ThreadBell<Stat>() {
      public Stat run() throws Exception {
        // First stat the thing to see if it's a directory.
        Stat stat = new Stat();
        FTPFile[] fileList = session.client.listFiles(path.toString());
        if(fileList.length == 1 && fileList[0].isFile())
          return attrsToStat(fileList[0]);
        List<Stat> files = new LinkedList<Stat>();
        for(FTPFile f : fileList) {
          Stat s = attrsToStat(f);
          s.name = f.getName();
          files.add(s);
        }
        stat.setFiles(files);
        stat.dir = true;
        stat.file = !stat.dir;
        return stat;
      }
    }.startOn(initialize());
  }

  /** Convert JSch attrs to Feather stat. */
  private Stat attrsToStat(FTPFile f) {
    Stat stat = new Stat(path.name());
    stat.dir  = f.isDirectory();
    stat.file = !stat.dir;
    if (f.isSymbolicLink())
      stat.link = "(unknown)";
    stat.size = f.getSize();
    stat.perm = "";
    stat.time = f.getTimestamp().getTime().getTime() / 1000;
    return stat;
  }

  public Bell mkdir() {
    return new ThreadBell<Void>() {
      public Void run() throws Exception {
        session.client.makeDirectory(path.toString());
        return null;
      }
    }.startOn(initialize());
  }

  public Bell delete() {
    return new ThreadBell<Void>() {
      public Void run() throws Exception {
        if(path.toString().indexOf('.') > 0)
          session.client.deleteFile(path.toString());
        else
          session.client.removeDirectory(path.toString());
        return null;
      }
    }.startOn(initialize());
  }

  public Tap tap() {
    return new Tap(this) {
      protected Bell start(Bell bell) {
        return new ThreadBell<Void>() {
          public Void run() throws Exception {
            session.client.retrieveFile(path.toString(), asOutputStream());
            return null;
          } public void done() {
            finish();
          } public void fail(Throwable t) {
            finish(t);
          }
        }.startOn(initialize().and(bell));
      }
    };
  }

  public Sink sink() {
    return new Sink(this) {
      private java.io.OutputStream os;

      protected Bell start() {
        return initialize().and((Bell<Stat>)source().stat()).new As<Void>() {
          public Void convert(Stat stat) throws Exception {
            os = session.client.storeFileStream(path.toString());
            return null;
          } public void fail(Throwable t) {
            finish(t);
          }
        };
      }

      protected Bell drain(final Slice slice) {
        return new ThreadBell<Void>() {
          public Void run() throws Exception {
            os.write(slice.asBytes());
            os.flush();
            return null;
          }
        }.start();
      }

      protected void finish(Throwable t) {
        try {
          os.close();
        } catch (Exception e) {
          // Ignore.
        }
      }
    };
  }
}
