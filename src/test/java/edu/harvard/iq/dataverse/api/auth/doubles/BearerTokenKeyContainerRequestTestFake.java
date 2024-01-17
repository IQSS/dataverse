package edu.harvard.iq.dataverse.api.auth.doubles;

import jakarta.ws.rs.core.HttpHeaders;

public class BearerTokenKeyContainerRequestTestFake extends ContainerRequestTestFake {

    private final String apiKey;

    public BearerTokenKeyContainerRequestTestFake(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getHeaderString(String s) {
        if (s.equals(HttpHeaders.AUTHORIZATION)) {
            return this.apiKey;
        }
        return null;
    }
}
