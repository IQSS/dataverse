package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Produces a generic 500 message for the API, being a fallback handler for not specially treated exceptions.
 */
@Provider
public class ThrowableHandler implements ExceptionMapper<Throwable>{
    
    private static final Logger logger = Logger.getLogger(ThrowableHandler.class.getName());
    
    @Context
    HttpServletRequest request;
    
    @Override
    public Response toResponse(Throwable ex){
        String incidentId = UUID.randomUUID().toString();
        logger.log(Level.SEVERE, "Uncaught REST API exception:\n"+
                                 "    Incident: " + incidentId +"\n"+
                                 "    URL: "+getOriginalURL(request)+"\n"+
                                 "    Method: "+request.getMethod(), ex);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                             .add("type", ex.getClass().getSimpleName())
                             .add("message", "Internal server error. More details available at the server logs.")
                             .add("incidentId", incidentId)
                        .build())
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }
    
    /**
     * Some HTTP return codes should be logged at higher levels than FINEST (which is for debugging).
     * Match the code and a log level here.
     */
    static Map<Integer,Level> levels = Stream.of(new Object[][] {
        {500, Level.SEVERE}
    }).collect(Collectors.toMap(data -> (Integer) data[0], data -> (Level) data[1]));
    
    /**
     * Create a useful log message with parseable metadata and log it.
     * Log level of message is determined by the levels map and defaults to FINEST (=debugging).
     * Log messages will only be logged if the level is enabled for the logger.
     * @param header A useful, human readable error message.
     * @param requestMethod The HTTP method that triggered the error
     * @param requestUrl The HTTP request URL that triggered the error
     * @param httpStatus The response HTTP status code
     * @param ex The real exception that has been thrown
     * @return A random incident id used in the log message, may be forwarded to the client response message.
     */
    static String handleLogging(String header, String requestMethod, String requestUrl, int httpStatus, Throwable ex) {
        String incidentId = UUID.randomUUID().toString();
        String message = "error="+ex.getClass().getSimpleName()+
                         ";incident="+incidentId+
                         ";method="+requestMethod+
                         ";url="+requestUrl+
                         ";status="+httpStatus+"|\n"+
                         header+"|";
        
        Level level = levels.getOrDefault(httpStatus, Level.FINEST);
        if (logger.isLoggable(level))
            logger.log(level, message, ex);
        
        return incidentId;
    }
    
    /**
     * Create a new (error) response with a human readable yet machine actionable JSON error message.
     * @param status HTTP return code
     * @param requestMethod The HTTP method used in the original request
     * @param requestUrl The HTTP URL used in the original request
     * @param errorType Ideally a exception class name, but maybe free text
     * @param message A human readable message
     * @param incidentId A random incident id. Should be the same as the logged id for easier error analysis
     * @return A response ready to be send to the client
     */
    static Response createErrorResponse(int status, String requestMethod, String requestUrl, String errorType, String message, String incidentId) {
        return createErrorResponse(Response.status(status).build(), requestMethod, requestUrl, errorType, message, incidentId);
    }
    
    /**
     * Create an error response with a human readable yet machine actionable JSON error message, based on an existing
     * response (necessary for things like a redirect).
     * @param fromResponse The original response to enhance
     * @param requestMethod The HTTP method used in the original request
     * @param requestUrl The HTTP URL used in the original request
     * @param errorType Ideally a exception class name, but maybe free text
     * @param message A human readable message
     * @param incidentId A random incident id. Should be the same as the logged id for easier error analysis
     * @return A response ready to be send to the client
     */
    static Response createErrorResponse(Response fromResponse, String requestMethod, String requestUrl, String errorType, String message, String incidentId) {
        return Response.fromResponse(fromResponse)
                       .entity(Json.createObjectBuilder()
                           .add("status", "ERROR")
                           .add("code", fromResponse.getStatus())
                           .add("method", requestMethod)
                           .add("url", requestUrl)
                           .add("errorType", errorType)
                           .add("message", message)
                           .add("incidentId", incidentId)
                           .build()
                       )
                       .type(MediaType.APPLICATION_JSON_TYPE)
                       .build();
    }
    
    /**
     * Build a complete request URL for logging purposes
     * @param req The request
     * @return The requests URL sent by the client
     */
    static String getOriginalURL(HttpServletRequest req) {
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
            url.append("?").append(queryString);
        }
        
        return url.toString();
    }
}
