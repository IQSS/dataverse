/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.util.BundleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

/**
 * Catches all types of web application exceptions like NotFoundException, etc etc and handles them properly.
 */
@Provider
public class WebApplicationExceptionHandler implements ExceptionMapper<WebApplicationException> {
    
    static final Logger logger = Logger.getLogger(WebApplicationExceptionHandler.class.getSimpleName());
    
    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(WebApplicationException ex) {
        
        String requestMethod = request.getMethod();
        String requestUrl = ThrowableHandler.getOriginalURL(request);
        String message = createMessage(ex);
        
        String incidentId = ThrowableHandler.handleLogging(message,
            requestMethod,
            requestUrl,
            ex.getResponse().getStatus(),
            ex);
        
        return ThrowableHandler.createErrorResponse(ex.getResponse(),
            requestMethod,
            requestUrl,
            ex.getClass().getSimpleName(),
            message,
            incidentId);
    }
    
    /**
     * Analyse the exception and generate a human readable (and helpful) error message.
     * @param ex The exception thrown
     * @return A human readable error message.
     */
    String createMessage(WebApplicationException ex) {
        
        String message = "";
        String exMessage = ex.getMessage();
        
        // See also https://en.wikipedia.org/wiki/List_of_HTTP_status_codes for a list of status codes.
        switch (ex.getResponse().getStatus()) {
            // Redirects (permanent & temporary)
            case 302:
            case 307:
                message = ex.getResponse().getLocation().toString();
                break;
            // BadRequest
            case 400:
                if (exMessage != null && exMessage.toLowerCase().startsWith("tabular data required")) {
                    message = BundleUtil.getStringFromBundle("access.api.exception.metadata.not.available.for.nontabular.file");
                } else {
                    message = "Bad Request. The API request cannot be completed with the parameters supplied. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.";
                }
                break;
            // Forbidden
            case 403:
                message = "Not authorized to access this object via this API endpoint. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.";
                break;
            // NotFound
            case 404:
                if (exMessage != null && exMessage.toLowerCase().startsWith("datafile")) {
                    message = exMessage;
                } else {
                    message = "API endpoint does not exist on this server. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.";
                }
                break;
            // MethodNotAllowed
            case 405:
                message = "API endpoint does not support this method. Consult our API guide at http://guides.dataverse.org.";
                break;
            // InternalServerError
            case 500:
                message = "Internal server error. More details available at the server logs. Please contact your dataverse administrator.";
                break;
            // ServiceUnavailable
            case 503:
                if (exMessage != null && exMessage.toLowerCase().startsWith("datafile")) {
                    message = exMessage;
                } else {
                    message = "Requested service or method not available on the requested object";
                }
                break;
        }
        
        return message;
    }
    
}
