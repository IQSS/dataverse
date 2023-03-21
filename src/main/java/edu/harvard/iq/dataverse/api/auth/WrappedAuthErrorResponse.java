package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WrappedAuthErrorResponse extends Exception {

    private final String message;
    private final Response response;

    public WrappedAuthErrorResponse(String message) {
        this.message = message;
        this.response = Response.status(Response.Status.UNAUTHORIZED)
                .entity(NullSafeJsonBuilder.jsonObjectBuilder()
                        .add("status", ApiConstants.STATUS_ERROR)
                        .add("message", message).build()
                ).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public String getMessage() {
        return this.message;
    }

    public Response getResponse() {
        return response;
    }
}
