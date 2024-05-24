package edu.harvard.iq.dataverse.api.auth.doubles;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import static edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism.DATAVERSE_API_KEY_REQUEST_PARAM_NAME;

public class ApiKeyUriInfoTestFake extends UriInfoTestFake {

    private final String apiKey;
    private final String path;

    public ApiKeyUriInfoTestFake(String apiKey, String path) {
        this.apiKey = apiKey;
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.add(DATAVERSE_API_KEY_REQUEST_PARAM_NAME, apiKey);
        return queryParameters;
    }
}
