package stork.module.gridftp;

import stork.feather.*;
import stork.module.*;

public class GridFTPModule extends Module<GridFTPResource> {
    {
        name("Stork GridFTP Module");
        protocols("gridftp");
        description("Experimental GridFTP module.");
    }

    public GridFTPResource select(URI uri, Credential credential, String id) {
        URI endpoint = uri.endpointURI(), resource = uri.resourceURI();
        return new GridFTPSession(endpoint, credential).select(resource.path(), id);
    }

}
