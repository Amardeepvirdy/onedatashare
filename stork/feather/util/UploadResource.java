package stork.feather.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import stork.core.handlers.UploadHandler;
import stork.feather.*;

/** A {@code Resource} produced by a {@code LocalSession}. */
public class UploadResource extends Resource<UploadSession,UploadResource> {
    // Separate reference to work around javac bug.
    public UploadTap upt = new UploadTap(this);

    public UploadResource(UploadSession session, Path path) {
        super(session, path, null);
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
    private Slice tempSlice;

    // State of the current transfer.
    public UploadTap(UploadResource root) {
        super(root);
        remaining = source().session._totalSize;
        System.out.println("TotalSize is " + remaining);
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
        //remaining = file.length();d

        System.out.println("BBefore promise");
        return source().initialize().and(bell).new Promise() {

            public void done() {
                System.out.println("its running");

                        doRead();

            }

        };
    }

    public void doRead() {
        pause.new As<Void>() {

            public Void convert(Object o) throws Exception {
                new Thread(new Runnable() {
                    public void run(){
                        int current_chunk = source().session.current_chunk;
                        if(current_chunk > 0 && file[current_chunk-1] != null){
                            file[current_chunk-1].asByteBuf().release();
                            file[current_chunk-1] = null;
                        }
                        try {
                            source().session.available.acquire();
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        if(file[current_chunk] == null){
                            finish(new Exception("Semaphore Waited for ten seconds but upload not completed"));
                            return;
                        }
                        Slice dataToSend = file[current_chunk];
                        //Slice dataToSend = o.slice;
                        pause = drain(dataToSend);
                        String ckN = source().session._attributes[current_chunk].get("_chunkNumber");
                        //String ckN = o.att.get("_chunkNumber");
                        int actualSize = dataToSend.length();
                        //long expectedSize = Long.parseLong(source().session._attributes[current_chunk].get("_currentChunkSize"));
                        remaining -= actualSize;
                        System.out.println("Sending " + ckN );
                        System.out.println("remaining is " + remaining);
                        source().session._file[current_chunk] = null;
                        source().session.current_chunk++;

                        if (remaining > 0){
                            new Thread(new Runnable() {
                                public void run(){
                                    doRead();
                                }
                            }).start();
                        }else {
                            finish();
                        }
                    }
                }).start();
                return null;
            } public void fail(Throwable t) {
                t.printStackTrace();
                finish(t);
            }
        };
    }

    protected void finish(Throwable t) {
        if(t != null){
            t.printStackTrace();
        }
        try {
            UploadHandler.sessions.remove(source().uri());
            source().session.available.release(Math.toIntExact(source().session.totalNumbebrOfChunks));
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.finish(t);
    }
}
