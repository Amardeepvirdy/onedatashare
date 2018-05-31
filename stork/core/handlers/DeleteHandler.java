package stork.core.handlers;

import stork.core.server.*;
import stork.feather.util.MethodLogs;

public class DeleteHandler extends Handler<EndpointRequest> {
  public void handle(EndpointRequest req) {
    MethodLogs.logMessage("Info","Delete Handler was invoked");
    req.assertLoggedIn();
    req.assertMayChangeState();
    req.resolve().delete().promise(req);
  }
}
