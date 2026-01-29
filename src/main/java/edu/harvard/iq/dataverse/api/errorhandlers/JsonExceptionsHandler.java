package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.util.JsonResponseBuilder;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Make a failing JSON parsing request appear to be a BadRequest (error code 400)
 * and send a message what just failed...
 */
public abstract class JsonExceptionsHandler<T extends Exception> implements ExceptionMapper<T>{
    
    static final Logger logger = Logger.getLogger(JsonExceptionsHandler.class.getSimpleName());
    
    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(T ex) {
        return JsonResponseBuilder.error(Response.Status.BAD_REQUEST)
            .log(logger, Level.FINER)
            .message(ex.getMessage())
            .build();
    }
    
    /**
     * Handler for jakarta.json.stream.JsonParsingException
     */
    @Provider
    public static class JsonParsingExceptionMapper extends JsonExceptionsHandler<JsonParsingException> {
    }
    
    /**
     * Handler for jakarta.json.stream.JsonParsingException
     */
    @Provider
    public static class DvUtilJsonParseExceptionMapper extends JsonExceptionsHandler<JsonParseException> {
    }
    
    // Add more handlers as needed (e.g., for Jackson, GSON, etc.)
    // @Provider
    // public static class JsonProcessingExceptionMapper extends JsonExceptionHandler<JsonProcessingException> {
    // }
}
