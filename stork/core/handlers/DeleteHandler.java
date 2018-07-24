package stork.core.handlers;

import stork.core.server.*;
import stork.feather.util.MethodLogs;

public class DeleteHandler extends Handler<EndpointRequest> {
  public void handle(EndpointRequest req) {
    req.assertLoggedIn();
    req.assertMayChangeState();
    req.resolve().delete().promise(req);
  }
}
