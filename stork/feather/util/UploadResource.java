package stork.feather.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Date;

import io.netty.buffer.ByteBuf;
import stork.core.handlers.UploadHandler;
import stork.feather.*;

/** A {@code Resource} produced by a {@code LocalSession}. */
public class UploadResource extends Resource<UploadSession,UploadResource> {
    // Separate reference to work around javac bug.
    public UploadTap upt = new UploadTap(this);

    public UploadResource(UploadSession session, Path path) {
        super(session, path);
    }
    public Bell<Stat> stat() {
        final Throwable throwable = new RuntimeException();
        return new ThreadBell<Stat>(session.executor) {
            { string = path().toString(); }
            public Stat run() {
                Stat stat = new Stat(session._filename);
                stat.size = session._totalSize;
                stat.file = true;
                stat.dir = false;
                stat.link = session._uri;
                stat.time = new Date().getTime();
                return stat;
            }
        }.start().detach();
    }
    // Get the absolute path to the file.
    private Path path() {
        return path.absolutize(session._path);
    }

    // Get the File based on upload.
    public Slice[] file() {
        return session._file;
    }

    public Tap<UploadResource> tap() {
        return upt;
    }
}

class UploadTap extends Tap<UploadResource> {
    final Slice[] file = source().file();
    private volatile Bell<?> pause = Bell.rungBell();
    private long remaining = 0;
    private short current_chunk = 0;

    // State of the current transfer.
    public UploadTap(UploadResource root) {
        super(root);
        remaining = source().session._totalSize;
    }

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
                if(current_chunk > 0 && file[current_chunk-1] != null){
                    file[current_chunk-1].asByteBuf().release();
                    file[current_chunk-1] = null;
                }
                if(source().session._file[current_chunk] == null){
                    doRead();
                    return null;
                }
                pause = drain(file[current_chunk]);
                String ckN = source().session._attributes[current_chunk].get("_chunkNumber");
                int sizeOfSlice = file[current_chunk].length();
                remaining -= Long.parseLong(source().session._attributes[current_chunk].get("_currentChunkSize"));

                source().session._attributes[current_chunk] = null;
                current_chunk++;

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
            UploadHandler.sessions.remove(source().uri());
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.finish(t);
    }
}
