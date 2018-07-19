package stork.core.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.util.internal.StringUtil;
import stork.ad.*;
import stork.core.server.*;
import stork.feather.*;
import stork.feather.util.*;
import stork.scheduler.*;
import stork.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static stork.scheduler.JobStatus.failed;

/** Handles scheduling jobs. */
public class UploadHandler extends Handler<MultipartRequest> {
    public static HashMap<String, Resource> sessions = new HashMap<String, Resource>();
    public void handle(MultipartRequest req) {
        req.assertLoggedIn();
        req.assertMayChangeState();
        final UploadResource s;
        if(sessions.get(req.uri) == null) {
            System.out.println("before Start1");
            s = new UploadSession(req.file, req.attributes).root();
            final Resource d = req.resolveAs("destination");
            final Transfer t = s.transferTo(d);
            System.out.println("before Start");
            t.start();
            t.onStop().new Promise() {
                public void always() {
                    sessions.remove(req.uri);
                }
                public void fail(Throwable t) {
                    // There was some problem during the transfer. Reschedule if possible.
                    Log.warning("Job failed: ", " ", t);
                }
            };
            sessions.put(req.uri, s);
        }else{
            s = (UploadResource) sessions.get(req.uri);
            s.session.addFileToArray(req.file, req.attributes);
        }
        /*try (FileChannel inputChannel = new FileInputStream(fileUpload.getFile()).getChannel();
             FileChannel outputChannel = new FileOutputStream(file, true).getChannel()) {
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            sendResponse(ctx, CREATED, "file name: " + file.getAbsolutePath());
            filePos += inputChannel.size();
        }catch(Exception e){
            System.out.println("Error on read chunk");
        }*/

    }
}