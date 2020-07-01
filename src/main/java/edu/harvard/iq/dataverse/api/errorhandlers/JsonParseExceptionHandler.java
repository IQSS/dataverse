package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.util.json.JsonParseException;

import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
