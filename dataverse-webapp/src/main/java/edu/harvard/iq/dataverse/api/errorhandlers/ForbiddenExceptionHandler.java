package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Produces custom 403 messages for the API.
 * @author michael
 */
@Provider
public class ForbiddenExceptionHandler implements ExceptionMapper<ForbiddenException>{

    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(ForbiddenException ex){
        String uri = request.getRequestURI();
        return Response.status(403)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", 403)
                             .add("message", "'" + uri + "' you are not authorized to access this object via this api endpoint. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.")
                        .build())
                .type("application/json").build();
        
       
    }
    
}

