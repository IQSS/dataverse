package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.util.json.JsonParseException;

import jakarta.json.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Make a failing JSON parsing request appear to be a BadRequest (error code 400)
 * and send a message what just failed...
 */
@Provider
public class JsonParseExceptionHandler implements ExceptionMapper<JsonParseException>{
    
    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(JsonParseException ex){
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity( Json.createObjectBuilder()
                           .add("status", "ERROR")
                           .add("code", Response.Status.BAD_REQUEST.getStatusCode())
                           .add("message", ex.getMessage())
                           .build())
                       .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
