package stork.core.handlers;

import stork.ad.*;
import stork.core.server.*;
import stork.feather.*;
import stork.scheduler.*;

/** Handles scheduling jobs. */
public class UploadHandler extends Handler<JobRequest> {
    public void handle(JobRequest req) {
        req.assertLoggedIn();
        req.assertMayChangeState();

        req.validate();
        String[] srcUriArr = req.getSrcUri().trim().split(",");
        String[] destUriArr = req.getDestUri().trim().split(",");
        for(int i = 0; i < srcUriArr.length; i++) {
            req.setSrcUri(srcUriArr[i]);
            req.setDestUri(destUriArr[i]);
            Job job = req.createJob();
            req.user().saveJob(job);
            req.server.schedule(job);
            req.ring(job);
        }
        server.dumpState();
    }
}
