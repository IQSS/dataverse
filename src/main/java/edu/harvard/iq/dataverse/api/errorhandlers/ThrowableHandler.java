package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.util.JsonResponseBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Produces a generic 500 message for the API, being a fallback handler for not specially treated exceptions.
 *
 * This catches bad exceptions like ArrayOutOfBoundsExceptions, NullPointerExceptions and ServeletExceptions,
 * which had formerly specialised handlers, generating a generic error message. (This is now handled here.)
 */
@Provider
public class ThrowableHandler implements ExceptionMapper<Throwable>{
    
    private static final Logger logger = Logger.getLogger(ThrowableHandler.class.getName());
    
    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(Throwable ex){
        ex.printStackTrace();
        return JsonResponseBuilder.error(Response.Status.INTERNAL_SERVER_ERROR)
            .randomIncidentId()
            .internalError(ex)
            .request(request)
            .log(logger, Level.SEVERE, Optional.of(ex))
            .build();
    }
}
