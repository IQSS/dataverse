package edu.harvard.iq.dataverse.api.auth;

import jakarta.ws.rs.core.Response;

public class WrappedForbiddenAuthErrorResponse extends WrappedAuthErrorResponse {

    public WrappedForbiddenAuthErrorResponse(String message) {
        super(Response.Status.FORBIDDEN, message);
    }
}
