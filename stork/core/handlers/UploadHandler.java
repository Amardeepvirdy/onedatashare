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

import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static stork.scheduler.JobStatus.failed;

/** Handles scheduling jobs. */
public class UploadHandler extends Handler<MultipartRequest> {
    public void handle(MultipartRequest req) {
        req.assertLoggedIn();
        req.assertMayChangeState();
        Resource d = req.resolve();
        final UploadResource s = new UploadSession(req.file, req.attributes).root();

        Transfer t = s.transferTo(d);
        t.start();
        t.onStop().new Promise() {
            public void always() {
                s.session.close();
                d.session.close();
            }
            public void fail(Throwable t) {
                // There was some problem during the transfer. Reschedule if possible.
                Log.warning("Job failed: ", " ", t);
            }
        };






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
