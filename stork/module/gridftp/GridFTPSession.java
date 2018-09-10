package stork.module.gridftp;

import stork.cred.*;
import stork.feather.*;
import stork.feather.errors.*;
import org.globus.*;

public class GridFTPSession extends Session<GridFTPSession,GridFTPResource> {
    public GridFTPSession(URI uri, Credential cred) {
        super(uri, cred);
    }
}
