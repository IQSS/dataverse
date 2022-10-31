package edu.harvard.iq.dataverse.api.util;

import edu.harvard.iq.dataverse.api.ApiBlockingFilter;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonResponseBuilder {
    
    private JsonObjectBuilder entityBuilder = Json.createObjectBuilder();
    private Response.ResponseBuilder jerseyResponseBuilder;
    private boolean alreadyLogged = false;
    
    private JsonResponseBuilder() {}
    
    /**
     * Create an error response from an numeric error code (should be >= 400)
     * @param httpStatusCode Numerical HTTP status code
     * @return A builder with a basic JSON body
     * @throws IllegalArgumentException if fromResponse isn't an error response
     */
    public static JsonResponseBuilder error(int httpStatusCode) {
        if (httpStatusCode < 400) {
            throw new IllegalArgumentException("A status code < 400 cannot be an error indicating response.");
        }
        
        JsonResponseBuilder b = new JsonResponseBuilder();
        b.jerseyResponseBuilder = Response.status(httpStatusCode);
        b.entityBuilder.add("status", "ERROR");
        b.entityBuilder.add("code", httpStatusCode);
        // include default message if present
        getDefaultMessage(httpStatusCode).ifPresent(v -> b.entityBuilder.add("message", v));
        
        return b;
    }
    
    /**
     * Create an error response from a Response.Status.
     * @param status A JAX-RS Response.Status object
     * @return A builder with a basic JSON body
     * @throws IllegalArgumentException if fromResponse isn't an error response
     */
    public static JsonResponseBuilder error(Response.Status status) {
        JsonResponseBuilder b = error(status.getStatusCode());
        b.jerseyResponseBuilder = Response.status(status);
        return b;
    }
    
    /**
     * Create an error response from an existing response.
     * @param fromResponse An existing JAX-RS Response
     * @return A builder with a basic JSON body
     * @throws IllegalArgumentException if fromResponse isn't an error response
     */
    public static JsonResponseBuilder error(Response fromResponse) {
        JsonResponseBuilder b = error(fromResponse.getStatus());
        b.jerseyResponseBuilder = Response.fromResponse(fromResponse);
        return b;
    }
    
    /**
     * Add a human friendly message to the response
     * @param message A human readable message
     * @return The enhanced builder
     */
    public JsonResponseBuilder message(String message) {
        this.entityBuilder.add("message", message);
        return this;
    }
    
    /**
     * Set an identifier for this (usually included in logs, too).
     * @param id A String containing an (ideally unique) identifier
     * @return The enhanced builder
     */
    public JsonResponseBuilder incidentId(String id) {
        this.entityBuilder.add("incidentId", id);
        return this;
    }
    
    /**
     * Add a UUID random identifier for errors. Will overwrite existing.
     * @return The enhanced builder
     */
    public JsonResponseBuilder randomIncidentId() {
        this.entityBuilder.add("incidentId", UUID.randomUUID().toString());
        return this;
    }
    
    /**
     * Add more details about the original request: what URL was used,
     * what HTTP method involved?
     * @param request The original request (usually provided from a context)
     * @return The enhanced builder
     */
    public JsonResponseBuilder request(HttpServletRequest request) {
        this.entityBuilder.add("requestUrl", getOriginalURL(request));
        this.entityBuilder.add("requestMethod", request.getMethod());
        return this;
    }
    
    /**
     * Add more details about the original request: what content type was sent?
     * @param request The original request (usually provided from a context)
     * @return The enhanced builder
     */
    public JsonResponseBuilder requestContentType(HttpServletRequest request) {
        String type = request.getContentType();
        this.entityBuilder.add("requestContentType", ((type==null) ? JsonValue.NULL : Json.createValue(type)));
        return this;
    }
    
    /**
     * Add more details about internal errors (exceptions) to the response.
     * Will include a detail about the cause if exception has one.
     * @param ex An exception.
     * @return The enhanced builder
     */
    public JsonResponseBuilder internalError(Throwable ex) {
        this.entityBuilder.add("interalError", ex.getClass().getSimpleName());
        if (ex.getCause() != null) {
            this.entityBuilder.add("internalCause", ex.getCause().getClass().getSimpleName());
        }
        return this;
    }
    
    /**
     * Finish building a Jersey JAX-RS response with JSON message
     * @return JAX-RS response including JSON message
     */
    public Response build() {
        return jerseyResponseBuilder.type(MediaType.APPLICATION_JSON_TYPE)
            .entity(this.entityBuilder.build())
            .build();
    }
    
    /**
     * For usage in non-Jersey areas like servlet filters, blocks, etc.,
     * apply the response to the Servlet provided response object.
     * @param response The ServletResponse from the context
     * @throws IOException
     */
    public void apply(ServletResponse response) throws IOException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        apply(httpServletResponse);
    }
    
    /**
     * For usage in non-Jersey areas like servlet filters, blocks, etc.,
     * apply the response to the Servlet provided response object.
     * @param response The HttpServletResponse from the context
     * @throws IOException
     */
    public void apply(HttpServletResponse response) throws IOException {
        Response jersey = jerseyResponseBuilder.build();
        response.setStatus(jersey.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON);
        response.getWriter().print(entityBuilder.build().toString());
        response.getWriter().flush();
    }
    
    /**
     * Log this JSON response as a useful log message.
     * Should be done before calling build(), but after adding any decorations.
     *
     * The log message will contain a message with the flattened JSON entity:
     * { "status": "ERROR", "code": 401 } -> _status=ERROR;_code=401
     *
     * Will prevent logging the same response twice.
     *
     * @param logger Provide a logger instance to write to
     * @param level Provide a level at which this should be logged
     * @return The unmodified builder.
     */
    public JsonResponseBuilder log(Logger logger, Level level) {
        return this.log(logger, level, Optional.empty());
    }
    
    /**
     * Log this JSON response as a useful log message.
     * Should be done before calling build(), but after adding any decorations.
     *
     * The log message will contain a message with the flattened JSON entity:
     * { "status": "ERROR", "code": 401 } -> _status=ERROR;_code=401
     *
     * If an exception is given, it will be folled by a "|" and the exception message
     * formatted by the logging system itself.
     *
     * Will prevent logging the same response twice.
     *
     * @param logger Provide a logger instance to write to
     * @param level Provide a level at which this should be logged
     * @param ex An optional exception to be included in the log message.
     * @param includeStackTrace whether to also include the stack trace 
     * @return The unmodified builder.
     */
    public JsonResponseBuilder log(Logger logger, Level level, Optional<Throwable> ex) {
        return log(logger, level, ex, false);
    }
    public JsonResponseBuilder log(Logger logger, Level level, Optional<Throwable> ex, boolean includeStackTrace) {
        if ( ! logger.isLoggable(level) || alreadyLogged )
            return this;
        
        StringBuilder metadata = new StringBuilder("");
        this.entityBuilder.build()
            .forEach((k,v) -> metadata.append("_"+k+"="+v.toString()+";"));
        // remove trailing ;
        metadata.deleteCharAt(metadata.length()-1);
        
        if (ex.isPresent()) {
            ex.get().printStackTrace();
            metadata.append("|");
            logger.log(level, metadata.toString(), ex);
            if(includeStackTrace) {
                logger.log(level, ExceptionUtils.getStackTrace(ex.get()));
            }
        } else {
            logger.log(level, metadata.toString());
        }
        this.alreadyLogged = true;
        return this;
    }

    
    /**
     * Build a complete request URL for logging purposes.
     * Masks query parameter "unblock-key" if present to avoid leaking secrets.
     * @param req The request
     * @return The requests URL sent by the client
     */
    public static String getOriginalURL(HttpServletRequest req) {
        // Rebuild the original request URL: http://stackoverflow.com/a/5212336/356408
        String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();   // d=789
        
        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath).append(servletPath);
        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (queryString != null) {
            // filter for unblock-key parameter and mask the key
            String maskedQueryString = queryString.replaceAll(ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM+"=.+?\\b", ApiBlockingFilter.UNBLOCK_KEY_QUERYPARAM+"=****");
            url.append("?").append(maskedQueryString);
        }
        
        return url.toString();
    }
    
    public static Optional<String> getDefaultMessage(int httpStatusCode) {
        switch (httpStatusCode) {
            case 500: return Optional.of("Internal server error. More details available at the server logs.");
            default: return Optional.empty();
        }
    }

}
