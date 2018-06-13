package stork.core.handlers;

import stork.feather.Resource;
import stork.feather.Transfer;

import java.util.UUID;

/** Handles retrieving files. */
public class UploadHandler extends Handler<UploadRequest> {
  public void handle(final UploadRequest req) {
    req.assertLoggedIn();
  }
}

/** Request for either an endpoint or a shared endpoint. */
class UploadRequest extends EndpointRequest {
  /** UUID for shared endpoints. */
  public UUID uuid;

  private EndpointRequest findActualEndpoint(String name) {
    if (name == null)
      name = "";
    EndpointRequest req = server().findSharedEndpoint(uuid);
    if (req == null)
      throw new RuntimeException("Invalid share ID for "+name+"endpoint.");
    return req;
  }

  public EndpointRequest validateAs(String name) {
    if (uuid == null)
      return super.validateAs(name);
    return findActualEndpoint(name).validateAs(name);
  }

  public Resource<?,?> resolveAs(String name) {
    if (uuid == null)
      return super.resolveAs(name);
    return findActualEndpoint(name).resolveAs(name);
  }
}

