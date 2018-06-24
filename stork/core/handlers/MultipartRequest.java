package stork.core.handlers;

import stork.core.server.Request;
import stork.core.server.Server;
import stork.core.server.User;
import stork.feather.Credential;
import stork.feather.Resource;
import stork.feather.URI;

import java.io.InputStream;

/** Request common to many commands that operate on endpoints. */
public class MultipartRequest extends Request {
  public InputStream data;


  /** Get the {@code Resource} identified by this request. */
  public Resource resolve() { return resolveAs(null); }

  /**
   * Get the {@code Resource} identified by this request using {@code name} in
   * error messages.
   */
  public Resource resolveAs(String name) {
    return new RealEndpoint().resolve();
  }

  /** This may be overridden by subclasses. */
  public Server server() { return server; }

  /** This may be overridden by subclasses. */
  public User user() { return super.user(); }
}

