package stork.core.handlers;

import java.util.*;

import stork.ad.*;
import stork.core.server.*;
import stork.feather.util.MethodLogs;
import stork.scheduler.*;
import stork.util.*;

/** Handle removal of a job. */
public class CancelHandler extends Handler<CancelRequest> {
  public void handle(CancelRequest req) {
    MethodLogs.logMessage("Info","Cancel Handler was invoked");
    req.assertLoggedIn();
    req.assertMayChangeState();

    if (req.job_id <= 0)
      throw new RuntimeException("No job specified.");

    Job job = req.user().getJob(req.job_id);
    job.remove("Job canceled by user.");
    req.ring();
  }
}

class CancelRequest extends Request {
  int job_id;
}
