package edu.harvard.iq.dataverse.api.auth.doubles;

import jakarta.ws.rs.core.UriInfo;

import static edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism.DATAVERSE_API_KEY_REQUEST_HEADER_NAME;

public class ApiKeyContainerRequestTestFake extends ContainerRequestTestFake {

    private final String apiKey;
    private final UriInfo uriInfo;

    public ApiKeyContainerRequestTestFake(String apiKey, String path) {
        this.apiKey = apiKey;
        this.uriInfo = new ApiKeyUriInfoTestFake(apiKey, path);
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    @Override
    public String getHeaderString(String s) {
        if (s.equals(DATAVERSE_API_KEY_REQUEST_HEADER_NAME)) {
            return this.apiKey;
        }
        return null;
    }
}
