package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Produces custom 503 messages for the API.
 * @author michael
 */
@Provider
public class ServiceUnavailableExceptionHandler implements ExceptionMapper<ServiceUnavailableException>{

    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(ServiceUnavailableException ex){
        String uri = request.getRequestURI();
        String exMessage = ex.getMessage(); 
        String outputMessage;
        if (exMessage != null && exMessage.toLowerCase().startsWith("datafile")) {
            outputMessage = exMessage;
        } else {
            outputMessage = "Requested service or method not available on the requested object";
        }
        return Response.status(503)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", 503)
                             .add("message", "'" + uri + "' " + outputMessage)
                        .build())
                .type("application/json").build();
        
       
    }
    
}

