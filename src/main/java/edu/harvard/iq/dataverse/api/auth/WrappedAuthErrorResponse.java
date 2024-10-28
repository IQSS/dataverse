package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class WrappedAuthErrorResponse extends Exception {

    private final String message;
    private final Response response;

    public WrappedAuthErrorResponse(String message) {
        this(message, false);
    }

    public WrappedAuthErrorResponse(String message, boolean forbidden) {
        this.message = message;
        this.response = createErrorResponse(
                forbidden ? Response.Status.FORBIDDEN : Response.Status.UNAUTHORIZED,
                message
        );
    }

    private Response createErrorResponse(Response.Status status, String message) {
        return Response.status(status)
                .entity(NullSafeJsonBuilder.jsonObjectBuilder()
                        .add("status", ApiConstants.STATUS_ERROR)
                        .add("message", message).build()
                )
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    public String getMessage() {
        return this.message;
    }

    public Response getResponse() {
        return response;
    }
}
