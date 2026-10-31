package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * Simple unpackaging of the check exception {@link WrappedResponse} to send the packaged response along.
 * Using this wrapper to JAX-RS makes it unnecessary to create try-catch-blocks in endpoints.
 */
@Provider
public class WrappedExceptionHandler implements ExceptionMapper<WrappedResponse> {
    
    private static final Logger logger = Logger.getLogger(WrappedExceptionHandler.class.getName());
    
    @Override
    public Response toResponse(WrappedResponse ex){
        return ex.getResponse();
    }
}