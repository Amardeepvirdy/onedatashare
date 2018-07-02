package stork.feather.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import io.netty.buffer.ByteBuf;
import stork.feather.*;

/** A {@code Resource} produced by a {@code LocalSession}. */
public class UploadResource extends Resource<UploadSession,UploadResource> {
    // Separate reference to work around javac bug.
    public UploadSession session;

    public UploadResource(UploadSession session, Path path) {
        super(session, path);
        this.session = session;
    }
    public Bell<Stat> stat() {
        final Throwable throwable = new RuntimeException();
        return new ThreadBell<Stat>(session.executor) {
            { string = path().toString(); }
            public Stat run() {
                Slice file = file();

                Stat stat = new Stat(session._attributes.get("filename"));
                stat.size = Integer.parseInt(session._attributes.get("_currentChunkSize"));
                stat.file = true;
                stat.dir = false;
                stat.link = "local";
                stat.time = 0;
                return stat;
            }
        }.start().detach();
    }
    // Get the absolute path to the file.
    private Path path() {
        return path.absolutize(session.path);
    }

    // Get the File based on upload.
    public Slice file() {
        return session._file;
    }

    public Tap<UploadResource> tap() {
        return new UploadTap(this);
    }
}

class UploadTap extends Tap<UploadResource> {
    final Slice file = source().file();
    private volatile Bell<?> pause = Bell.rungBell();
    private long remaining = 0;

    // State of the current transfer.
    public UploadTap(UploadResource root) { super(root); }

    public Bell start(Bell bell) throws Exception {
       // if (!file.)
         //   throw new RuntimeException("Permission denied");


        /*
        if(!file.exists()){
            throw new RuntimeException("File not found");
        }
        if(!file.canRead()){
            throw new RuntimeException("Permission denied");
        }
        if(!file.isFile()){
            throw new RuntimeException("Resource is a directory");
        }*/
        // Set up state.
        //remaining = file.length();
        return bell.new Promise() {
            public void done() { doRead(); }
        };
    }

    public void doRead() {
        pause.new As<Void>() {
            public Void convert(Object o) throws Exception {
                pause = drain(file);
                remaining = 0;
                if (remaining > 0)
                    doRead();
                else
                    finish();
                return null;
            } public void fail(Throwable t) {
                finish(t);
            }
        };
    }

    protected void finish(Throwable t) {
        try {
        } catch (Exception e) { }
        super.finish(t);
    }
}
