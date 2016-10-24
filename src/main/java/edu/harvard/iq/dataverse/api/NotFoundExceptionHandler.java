package edu.harvard.iq.dataverse.api;

import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Produces custom 404 messages for the API.
 * @author michael
 */
@Provider
public class NotFoundExceptionHandler implements ExceptionMapper<NotFoundException>{

    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(NotFoundException ex){
        String uri = request.getRequestURI();
        return Response.status(404)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", 404)
                             .add("message", "'" + uri + "' endpoint does not exist on this server. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.")
                        .build())
                .type("application/json").build();
        
       
    }
    
}

