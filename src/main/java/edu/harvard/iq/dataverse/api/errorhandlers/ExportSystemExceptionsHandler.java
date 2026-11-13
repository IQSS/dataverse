package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.util.JsonResponseBuilder;
import edu.harvard.iq.dataverse.export.service.ExportSystemException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handling exceptions bubbling up from the export system.
 */
public abstract class ExportSystemExceptionsHandler<T extends Exception> implements ExceptionMapper<T>{
    
    static final Logger logger = Logger.getLogger(ExportSystemExceptionsHandler.class.getSimpleName());
    
    @Context
    HttpServletRequest request;
    
    protected Response toResponse(T ex, Response.Status status) {
        return JsonResponseBuilder.error(status)
            .log(logger, Level.FINER)
            .message(ex.getMessage())
            .build();
    }
    
    @Provider
    public static final class InternalFailureExceptionMapper extends ExportSystemExceptionsHandler<ExportSystemException.InternalFailure> {
        @Override
        public Response toResponse(ExportSystemException.InternalFailure ex) {
            return JsonResponseBuilder.error(Response.Status.INTERNAL_SERVER_ERROR)
                .randomIncidentId()
                .internalError(ex)
                .request(request)
                .log(logger, Level.SEVERE, Optional.of(ex), true)
                .build();
        }
    }
    
    @Provider
    public static final class InvalidRequestExceptionMapper extends ExportSystemExceptionsHandler<ExportSystemException.InvalidRequest> {
        @Override
        public Response toResponse(ExportSystemException.InvalidRequest ex) {
            return toResponse(ex, Response.Status.BAD_REQUEST);
        }
    }
}
