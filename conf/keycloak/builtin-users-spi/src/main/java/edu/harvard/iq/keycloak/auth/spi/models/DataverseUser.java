package edu.harvard.iq.keycloak.auth.spi.models;

public class DataverseUser {

    private final DataverseAuthenticatedUser authenticatedUser;
    private final DataverseBuiltinUser builtinUser;

    public DataverseUser(DataverseAuthenticatedUser authenticatedUser, DataverseBuiltinUser builtinUser) {
        this.authenticatedUser = authenticatedUser;
        this.builtinUser = builtinUser;
    }

    public DataverseAuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public DataverseBuiltinUser getBuiltinUser() {
        return builtinUser;
    }
}
