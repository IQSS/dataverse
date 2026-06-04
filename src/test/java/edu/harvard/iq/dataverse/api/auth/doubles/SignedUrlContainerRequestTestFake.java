package edu.harvard.iq.dataverse.api.auth.doubles;

import jakarta.ws.rs.core.UriInfo;

public class SignedUrlContainerRequestTestFake extends ContainerRequestTestFake {
    private final UriInfo uriInfo;

    public SignedUrlContainerRequestTestFake(String signedUrlToken, String signedUrlUserId) {
        this(signedUrlToken, signedUrlUserId, null);
    }

    public SignedUrlContainerRequestTestFake(String signedUrlToken, String signedUrlUserId, String requestUriOverride) {
        this.uriInfo = new SignedUrlUriInfoTestFake(signedUrlToken, signedUrlUserId, requestUriOverride);
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }
}
