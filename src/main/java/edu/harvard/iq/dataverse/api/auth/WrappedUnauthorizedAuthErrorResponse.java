package edu.harvard.iq.dataverse.api.auth;

import jakarta.ws.rs.core.Response;

public class WrappedUnauthorizedAuthErrorResponse extends WrappedAuthErrorResponse {

    public WrappedUnauthorizedAuthErrorResponse(String message) {
        super(Response.Status.UNAUTHORIZED, message);
    }
}
