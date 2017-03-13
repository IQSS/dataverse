package edu.harvard.iq.dataverse.api.errorhandlers;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Produces custom 500 messages for the API.
 * @author michael
 */
@Provider
public class NullPointerExceptionHandler implements ExceptionMapper<java.lang.NullPointerException>{
    
    private static final Logger logger = Logger.getLogger(ServeletExceptionHandler.class.getName());
    
    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(java.lang.NullPointerException ex){
        String incidentId = UUID.randomUUID().toString();
        logger.log(Level.SEVERE, "API internal error " + incidentId +": Null Pointer", ex);
        return Response.status(500)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", 500)
                             .add("message", "Internal server error. More details available at the server logs.")
                             .add("incidentId", incidentId)
                        .build())
                .type("application/json").build();
    }
}
