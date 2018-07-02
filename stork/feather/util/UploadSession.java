package stork.feather.util;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import io.netty.buffer.ByteBuf;
import stork.ad.Ad;
import stork.feather.*;

/**
 * A {@code Session} capable of interacting with the Uploading file.
 * <p/>
 * Many of the methods in this session implementation perform long-running
 * operations concurrently using threads because this is the most
 * straighforward way to demonstrate the asynchronous nature of session
 * operations. However, this is often not the most efficient way to perform
 * operations concurrently, and ideal implementations would use an alternative
 * method.
 */
public class UploadSession extends Session<UploadSession,UploadResource> {
    final ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(1);
    final Slice _file;
    final Ad _attributes;
    static final String pathStr = "local";
    static final Path path = Path.create(UploadSession.pathStr);

    /** Create a {@code LocalSession} at the system root. */
    public UploadSession() { this(Path.ROOT); }

    /** Create a {@code LocalSession} at {@code path}. */
    public UploadSession(Slice file, Ad attributes) {
        super(URI.create(pathStr));
        _file = file;
        _attributes = attributes;
    }

    public UploadSession(Path path) {
        super(URI.create(path.toString()));
        _file = null;
        _attributes = null;
    }

    public UploadResource select(Path path) {
        return new UploadResource(this, path);
    }

    protected void finalize() {
        executor.shutdown();
    }

   /* public static void main(String[] args) {
        String sp = args.length > 0 ? args[0] : "/home/bwross/test";
        final Path path = Path.create(sp);
        final UploadResource s = new UploadSession(path).root();
        final HexDumpResource d = new HexDumpResource();

        Transfer t = s.transferTo(d);
        t.start();
        t.onStop().new Promise() {
            public void always() {
                s.session.close();
            }
        }.sync();
    }*/
}