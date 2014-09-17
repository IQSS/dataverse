package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.DvObject;

public class AccessRequest {

    private final AuthenticatedUser authenticatedUser;
    private final DvObject dvObject;

    public AccessRequest(AuthenticatedUser authenticatedUser, DvObject dvObject) {
        this.authenticatedUser = authenticatedUser;
        this.dvObject = dvObject;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public DvObject getDvObject() {
        return dvObject;
    }
}
