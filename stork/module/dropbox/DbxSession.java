package stork.module.dropbox;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.*;

import stork.cred.*;
import stork.feather.*;
import stork.feather.errors.*;

public class DbxSession extends Session<DbxSession, DbxResource> {
  DbxClientV2 client;

  public DbxSession(URI uri, Credential cred) {
    super(uri, cred);
  }

  public DbxResource select(Path path, String id) {
    return new DbxResource(this, path, id);
  }

  public Bell<DbxSession> initialize() {
    // If an OAuth token is provided, use it.
    if (credential instanceof StorkOAuthCred) {
      StorkOAuthCred oauth = (StorkOAuthCred) credential;
      DbxRequestConfig config =
        DbxRequestConfig.newBuilder("OneDataShare-DIDCLab").build();
      client = new DbxClientV2(config, oauth.data());
      return Bell.wrap(this);
    }

    throw new AuthenticationRequired("oauth");
  }
}
