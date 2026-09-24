package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.util.JsonResponseBuilder;
import jakarta.json.JsonException;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ProcessingExceptionHandler implements ExceptionMapper<ProcessingException>  {
    static final Logger logger = Logger.getLogger(ProcessingExceptionHandler.class.getCanonicalName());
    
    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(ProcessingException ex) {
        Optional<Throwable> jsonError = findJsonError(ex);
        
        if (jsonError.isEmpty()) {
            return JsonResponseBuilder.error(Response.Status.INTERNAL_SERVER_ERROR)
                .randomIncidentId()
                .internalError(ex)
                .request(request)
                .log(logger, Level.SEVERE, Optional.of(ex), true)
                .build();
        }
        
        // Use the JSON cause's message: the wrapper only says
        // "Error deserializing object from entity stream."
        return JsonResponseBuilder.error(Response.Status.BAD_REQUEST)
            .log(logger, Level.FINER)
            .message(jsonError.get().getMessage())
            .build();
    }
    
    /** First matching JSON-related failure in the cause chain, if any. */
    private static Optional<Throwable> findJsonError(Throwable ex) {
        for (Throwable t = ex.getCause(); t != null && t.getCause() != t; t = t.getCause()) {
            // Covers JSON-B and JSON-P exceptions
            if (t instanceof JsonException || t instanceof JsonbException) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
