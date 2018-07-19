package stork.core.handlers;

import java.util.*;

import stork.ad.*;
import stork.core.server.*;
import stork.scheduler.*;

public class QHandler extends Handler<QRequest> {
  public void handle(QRequest req) {
    req.assertLoggedIn();
    if(server.administrators.contains(req.user().email)) {
      final Map allJobs = new HashMap<String, List>();
      List list = new ArrayList();
      for (String user : server.users.keySet()) {
        //allJobs.put(user, ((User) server.users.get(user)).jobs());
        list.addAll(((User) server.users.get(user)).jobs());
      }
      req.ring(!req.count ? list : new Object() {
        int count = list.size();
      });
    }
    else{
      final List list = req.user().jobs();
      req.ring(!req.count ? list : new Object() {
        int count = list.size();
      });
    }
  }
}

class QRequest extends Request {
  boolean count = false;
}