package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.annotation.Priority;
import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private String getOriginalURL(HttpServletRequest req) {
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
