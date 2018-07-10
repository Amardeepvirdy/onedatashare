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
    final int _totalSize;
    final String _filename;

    /** Create a {@code LocalSession} at {@code path}. */
    public UploadSession(Slice file, Ad attributes) {

        super(URI.create(attributes.get("uri[uri]")));
        _uri = attributes.get("uri[uri]");
        _path = Path.create(_uri);
        _filename = attributes.get("_filename");
        _totalSize = Integer.parseInt(attributes.get("_totalSize"));
        int total = _totalSize;
        int chunk = Integer.parseInt(attributes.get("_chunkSize"));
        int totalNumbebrOfChunks = total / chunk + (total % chunk == 0 ? 0 : 1) ;
        _file = new Slice[totalNumbebrOfChunks];
        _attributes = new Ad[totalNumbebrOfChunks];
        addFileToArray(file, attributes);
        _upr = new UploadResource(this, _path);
    }

    public void addFileToArray(Slice file, Ad attributes){
        int chunkNum = Integer.parseInt(attributes.get("_chunkNumber"));
        _file[chunkNum] = file;
        _attributes[chunkNum] = attributes;
    }

    public UploadResource select(Path path) {
        return _upr ;
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