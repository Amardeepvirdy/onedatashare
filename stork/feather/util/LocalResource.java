package stork.feather.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import stork.feather.*;

/** A {@code Resource} produced by a {@code LocalSession}. */
public class LocalResource extends Resource<LocalSession,LocalResource> {
  // Separate reference to work around javac bug.
  LocalSession session;

  public LocalResource(LocalSession session, Path path) {
    super(session, path);
    this.session = session;
  }

  // Get the absolute path to the file.
  private Path path() {
    return path.absolutize(session.path);
  }

  // Get the File based on the path.
  public File file() {
    Path p = session.path.append(path);
    return new File(p.toString());
  }

  public Bell<LocalResource> mkdir() {
    return new ThreadBell(session.executor) {
      public Object run() {
        File file = file();
        if (file.exists() && file.isDirectory())
          throw new RuntimeException("Resource is a file.");
        else if (!file.mkdirs())
          throw new RuntimeException("Could not create directory.");
        return null;
      }
    }.start().as(this).detach();
  }

  public Bell<LocalResource> unlink() {
    return new ThreadBell(session.executor) {
      private File root = file();

      public Object run() {
        remove(root);
        return null;
      }

      // Recursively remove files.
      private void remove(File file) {
        if (isCancelled()) {
          throw new java.util.concurrent.CancellationException();
        } if (!file.exists()) {
          if (file == root)
            throw new RuntimeException("Resource does not exist: "+file);
        } else try {
          // If not a symlink and is a directory, delete contents.
          if (file.getCanonicalFile().equals(file) && file.isDirectory())
            for (File f : file.listFiles()) remove(f);
          if (!file.delete() && file == root)
            throw new RuntimeException("Resource could not be deleted: "+file);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }.start().as(this).detach();
  }

  // Returns the target File if this is a symlink, null otherwise.
  private File resolveLink(File file) {
    try {
      File cf = file.getCanonicalFile();
      if (!file.equals(cf))
        return cf;
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public Bell<Stat> stat() {
    return new ThreadBell<Stat>(session.executor) {
      public Stat run() {
        File file = file();

        if (!file.exists())
          throw new RuntimeException("Resource does not exist: "+file);

        Stat stat = new Stat(file.getName());
        stat.size = file.length();
        stat.file = file.isFile();
        stat.dir = file.isDirectory();
        
        File sym = resolveLink(file);
        if (sym != null)
          stat.link = Path.create(file.toString());

        if (stat.dir) stat.setFiles(file.list());
        stat.time = file.lastModified();

        return stat;
      }
    }.start().detach();
  }

  public Tap<LocalResource> tap() {
    return new LocalTap(this);
  }
}

class LocalTap extends Tap<LocalResource> {
  final File file = source().file();

  // Small hack to take advantage of NIO features.
  private WritableByteChannel nioToFeather = new WritableByteChannel() {
    public int write(ByteBuffer buffer) {
      Slice slice = new Slice(buffer);
      drain(slice);
      return slice.length();
    }

    public void close() { }
    public boolean isOpen() { return true; }
  };

  private RandomAccessFile raf;
  private FileChannel channel;
  private long offset = 0, remaining = 0;
  private long chunkSize = 16384;
  private volatile Bell pause;

  // State of the current transfer.
  public LocalTap(LocalResource root) { super(root); }

  public Bell start(Bell bell) throws Exception {
    if (!file.exists())
      throw new RuntimeException("File not found");
    if (!file.canRead())
      throw new RuntimeException("Permission denied");
    if (!file.isFile())
      throw new RuntimeException("Resource is a directory");

    // Set up state.
    raf = new RandomAccessFile(file, "r");
    channel = raf.getChannel();
    remaining = file.length();

    // When bell rings, start a loop to send all chunks.
    return bell.promise(new BellLoop(this) {
      public Bell lock() { return pause; }
      public void body() throws Exception {
        if (remaining <= 0) {
          finish();
          ring();
        } else {
          System.out.println("Sending: "+offset);
          long len = remaining < chunkSize ? remaining : chunkSize;
          len = channel.transferTo(offset, len, nioToFeather);
          offset += len;
          remaining -= len;
        }
      }
    });
  }

  public synchronized void pause(Bell bell) { pause = bell; }
}
