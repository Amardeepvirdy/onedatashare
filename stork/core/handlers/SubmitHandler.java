package stork.core.handlers;

import io.netty.util.internal.StringUtil;
import stork.ad.*;
import stork.core.server.*;
import stork.feather.*;
import stork.scheduler.*;

/** Handles scheduling jobs. */
public class SubmitHandler extends Handler<JobRequest> {
  public void handle(JobRequest req) {
    req.assertLoggedIn();
    req.assertMayChangeState();

    req.validate();
    String[] srcUriArr = req.getSrcUri().trim().split(",");
    String[] destUriArr = req.getDestUri().trim().split(",");
/*<<<<<<< HEAD
    for(int i = 0; i < srcUriArr.length; i++) {
      req.setSrcUri(srcUriArr[i]);
      req.setDestUri(destUriArr[i]);
=======*/
    String[] srcFolderIdArr = null;
    if(req.getSrcFolderIds() != null) {
      srcFolderIdArr = req.getSrcFolderIds().trim().split(",");
    }
    for(int i = 0; i < srcUriArr.length; i++) {
      req.setSrcUri(srcUriArr[i]);
      req.setDestUri(destUriArr[i]);
      if(srcFolderIdArr != null) {
        req.setSrcFolderId(srcFolderIdArr[i]);
      }
//>>>>>>> mythri
      Job job = req.createJob();
      req.user().saveJob(job);
      req.server.schedule(job);
      req.ring(job);
    }
    server.dumpState();
  }
}

class JobRequest extends Request {
  private JobEndpointRequest src, dest;

  public String getDestUri() {
    return dest.uri;
  }
  public String getSrcUri(){
    return src.uri;
  }
//<<<<<<< HEAD
//=======
  public String getSrcFolderIds() { return src.selectedFolderIds; }
  public void setSrcFolderId(String s) {src.selectedFolderIds = s;}
//>>>>>>> mythri
  public void setDestUri(String s) {
    dest.uri = s;
  }
  public void setSrcUri(String s){
    src.uri = s;
  }

  // Hack to get around marshalling limitations.
  private class JobEndpointRequest extends EndpointRequest {
    public Server server() { return user().server(); }
    public User user() { return JobRequest.this.user(); }
  };

  // TODO: More validations.
  public JobRequest validate() {
    src.validateAs("source");
    dest.validateAs("destination");
    return this;
  }
  @Override
  public String toString(){
    StringBuilder buf = new StringBuilder();

    buf.append("(");
    buf.append(StringUtil.simpleClassName(this));
    buf.append(')');
    buf.append(StringUtil.NEWLINE);

    buf.append("Source Endpoint: ");
    buf.append(src);
    buf.append(StringUtil.NEWLINE);

    buf.append("Dest Endpoint: ");
    buf.append(dest);
    buf.append(StringUtil.NEWLINE);

    buf.append(super.toString());
    buf.append(StringUtil.NEWLINE);
    return buf.toString();
  }
  /** Create a {@code Job} from this request. */
  public Job createJob() {
    Job job = Ad.marshal(this).unmarshal(new Job());
    return job;
  }
}