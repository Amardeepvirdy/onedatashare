package stork.core.handlers;
import io.netty.util.internal.StringUtil;
import stork.ad.Ad;
import stork.core.handlers.CredRequest;
import stork.core.handlers.EndpointRequest;
import stork.core.handlers.RealEndpoint;
import stork.core.server.Server;
import stork.core.server.User;
import stork.feather.Resource;
import stork.feather.Slice;
import stork.feather.URI;

/** Request common to many commands that operate on endpoints. */
public class MultipartRequest extends EndpointRequest {
    public Slice file;
    public Ad attributes;

    /**
     * Get the {@code Resource} identified by this request using {@code name} in
     * error messages.
     */

    private class InnerCredRequest extends CredRequest {
        public User user() { return stork.core.handlers.MultipartRequest.this.user(); }
    }


    /** This may be overridden by subclasses. */
    public Server server() { return server; }

    /** This may be overridden by subclasses. */
    public User user() { return super.user(); }
    // This method might be doing too much, but it's useful to have this in one


    public String toString(){
        StringBuilder buf = new StringBuilder();

        buf.append("File Name:");
        buf.append(file != null && attributes!=null ? attributes.get("filename"): "not found");
        buf.append(StringUtil.NEWLINE);

        buf.append(super.toString());

        return buf.toString();
    }
}