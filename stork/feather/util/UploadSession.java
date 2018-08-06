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
    public Slice[] _file;
    public Ad[] _attributes;
    final Path _path;
    final UploadResource _upr;
    final String _uri;
    final long _totalSize;
    final String _filename;
    short current_chunk = 0;
    private short gotten_chunk = 0;
    long totalNumbebrOfChunks = 0;

    final Semaphore available;
    final Bell<UploadInfo> newSlice = new Bell<UploadInfo>();
    /** Create a {@code LocalSession} at {@code path}. */
    public UploadSession(Slice file, Ad attributes) {
        super(URI.create(attributes.get("uri[uri]")));
        _uri = attributes.get("uri[uri]");
        _path = Path.create(_uri);
        _filename = attributes.get("_filename");
        _totalSize = Long.parseLong(attributes.get("_totalSize"));
        long total = _totalSize;
        int chunk = Integer.parseInt(attributes.get("_chunkSize"));
        totalNumbebrOfChunks = total / chunk + (total % chunk == 0 ? 0 : 1) ;
        available = new Semaphore(0, false);
        _file = new Slice[Math.toIntExact(totalNumbebrOfChunks)];
        _attributes = new Ad[Math.toIntExact(totalNumbebrOfChunks)];
        addFileToArray(file, attributes);
        _upr = new UploadResource(this, _path);
    }

    public void addFileToArray(Slice file, Ad attributes){
        int chunkNum = Integer.parseInt(attributes.get("_chunkNumber"));
        System.out.println("chunkNum Arrived "+ chunkNum);


        _file[chunkNum] = file;
        _attributes[chunkNum] = attributes;
        /* For testing purpose try{
            TimeUnit.SECONDS.sleep(1);
        }catch (Exception e){}*/
        while(gotten_chunk < totalNumbebrOfChunks && _file[gotten_chunk] != null){
            gotten_chunk++;
            available.release();
        }
       /* System.out.println("chunkNum Arrived "+ chunkNum);
        while(!newSlice.isDone()) {
            try {
                TimeUnit.MICROSECONDS.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        newSlice.ring(new UploadInfo(file, attributes));*/
    }

    public UploadResource select(Path path, String id) {
        return _upr ;
    }

    protected void finalize() {
        executor.shutdown();
    }
}
class UploadInfo{
    Slice slice;
    Ad att;
    public UploadInfo(Slice f, Ad attrib){
        att = attrib;
        slice = f;
    }
}