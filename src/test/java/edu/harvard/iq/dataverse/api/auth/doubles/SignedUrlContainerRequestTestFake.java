package edu.harvard.iq.dataverse.api.auth.doubles;

import javax.ws.rs.core.UriInfo;

public class SignedUrlContainerRequestTestFake extends ContainerRequestTestFake {
    private final UriInfo uriInfo;

    public SignedUrlContainerRequestTestFake(String signedUrlToken, String signedUrlUserId, String apiToken) {
        this.uriInfo = new SignedUrlUriInfoTestFake(signedUrlToken, signedUrlUserId, apiToken);
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }
}
