package edu.harvard.iq.dataverse.api.auth;

import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

import static edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism.DATAVERSE_API_KEY_REQUEST_PARAM_NAME;

public class UriInfoTestFake implements UriInfo {

    private final String apiKey;
    private final String path;

    public UriInfoTestFake(String apiKey, String path) {
        this.apiKey = apiKey;
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getPath(boolean b) {
        return null;
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return null;
    }

    @Override
    public List<PathSegment> getPathSegments(boolean b) {
        return null;
    }

    @Override
    public URI getRequestUri() {
        return null;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return null;
    }

    @Override
    public URI getAbsolutePath() {
        return null;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return null;
    }

    @Override
    public URI getBaseUri() {
        return null;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean b) {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.add(DATAVERSE_API_KEY_REQUEST_PARAM_NAME, apiKey);
        return queryParameters;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean b) {
        return null;
    }

    @Override
    public List<String> getMatchedURIs() {
        return null;
    }

    @Override
    public List<String> getMatchedURIs(boolean b) {
        return null;
    }

    @Override
    public List<Object> getMatchedResources() {
        return null;
    }

    @Override
    public URI resolve(URI uri) {
        return null;
    }

    @Override
    public URI relativize(URI uri) {
        return null;
    }
}
