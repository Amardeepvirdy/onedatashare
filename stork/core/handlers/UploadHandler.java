package stork.core.handlers;

import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.util.internal.StringUtil;
import stork.ad.*;
import stork.core.server.*;
import stork.feather.*;
import stork.scheduler.*;
import stork.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;

/** Handles scheduling jobs. */
public class UploadHandler extends Handler<MultipartRequest> {
    public void handle(MultipartRequest req) {
        System.out.println(req);
        req.assertLoggedIn();
        req.assertMayChangeState();
        Resource res = req.resolve();
        Sink s = res.sink();




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
/** Request common to many commands that operate on endpoints. */
class MultipartRequest extends EndpointRequest {
    public File file;
    public InnerCredRequest credential;
    public Ad attributes;

    /**
     * Get the {@code Resource} identified by this request using {@code name} in
     * error messages.
     */

    private class InnerCredRequest extends CredRequest {
        public User user() { return MultipartRequest.this.user(); }
    }

    public Resource resolveAs(String name) {
        return validateAndResolve(name).resolve();
    }

    /** This may be overridden by subclasses. */
    public Server server() { return server; }

    /** This may be overridden by subclasses. */
    public User user() { return super.user(); }
    // This method might be doing too much, but it's useful to have this in one
    // place.
    private RealEndpoint validateAndResolve(String name) {
        name = (name == null) ? "" : name+" ";
        RealEndpoint result = new RealEndpoint();

        if (uri != null)
            result.uri = URI.create(uri);  // Takes care of syntax errors.
        if (result.uri == null)
            throw new RuntimeException("No URI provided for "+name+"endpoint.");
        if (result.uri.scheme() == null)
            throw new RuntimeException("No URI scheme for "+name+"endpoint.");
        if (credential != null)
            result.credential = credential.resolve();
        if (credential != null && result.credential == null)
            throw new RuntimeException("Invalid credential for "+name+"endpoint.");
        if (module != null)
            result.module = server().modules.byHandle(module);
        else
            result.module = server().modules.byProtocol(result.uri.scheme());
        return result;
    }

    public String toString(){
        StringBuilder buf = new StringBuilder();

        buf.append("(");
        buf.append(StringUtil.simpleClassName(this));
        buf.append(')');
        buf.append(StringUtil.NEWLINE);

        buf.append("File Name:");
        buf.append(file != null ? file.getName(): "not found");
        buf.append(StringUtil.NEWLINE);

        buf.append(super.toString());

        return buf.toString();
    }
}