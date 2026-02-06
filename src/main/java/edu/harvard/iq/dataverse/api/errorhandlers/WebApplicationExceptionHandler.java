/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.util.JsonResponseBuilder;
import edu.harvard.iq.dataverse.util.BundleUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

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
        
        // If this is not a HTTP client or server error, just pass the response.
        if (ex.getResponse().getStatus() < 400)
            return ex.getResponse();
        
        // Otherwise, do stuff.
        JsonResponseBuilder jrb = JsonResponseBuilder.error(ex.getResponse());
        
        // See also https://en.wikipedia.org/wiki/List_of_HTTP_status_codes for a list of status codes.
        switch (ex.getResponse().getStatus()) {
            // BadRequest
            case 400:
                // It's strange to have these "startsWith" conditionals here. They both come from Access.java.
                if ( (ex.getMessage()+"").toLowerCase().startsWith("tabular data required")) {
                    jrb.message(BundleUtil.getStringFromBundle("access.api.exception.metadata.not.available.for.nontabular.file"));
                } else if ((ex.getMessage() + "").toLowerCase().startsWith("no permission to download file")) {
                    jrb.message(BundleUtil.getStringFromBundle("access.api.exception.metadata.restricted.no.permission"));
                } else {
                    String msg = ex.getMessage();
                    msg = StringUtils.isEmpty(msg)
                            ? "Bad Request. The API request cannot be completed with the parameters supplied. Please check your code for typos, or consult our API guide at http://guides.dataverse.org."
                            : "Bad Request. The API request cannot be completed with the parameters supplied. Details: "
                                    + msg
                                    + " - please check your code for typos, or consult our API guide at http://guides.dataverse.org.";

                    jrb.message(msg);
                    jrb.request(request);
                }
                break;
            // Forbidden
            case 403:
                jrb.message("Not authorized to access this object via this API endpoint. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.");
                jrb.request(request);
                break;
            // NotFound
            case 404:
                if ( (ex.getMessage()+"").toLowerCase().startsWith("datafile")) {
                    jrb.message(ex.getMessage());
                } else {
                    jrb.message("API endpoint does not exist on this server. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.");
                    jrb.request(request);
                }
                break;
            // MethodNotAllowed
            case 405:
                jrb.message("API endpoint does not support this method. Consult our API guide at http://guides.dataverse.org.");
                jrb.request(request);
                break;
            // NotAcceptable (might be content type, charset, encoding or language)
            case 406:
                jrb.message("API endpoint does not accept your request. Consult our API guide at http://guides.dataverse.org.");
                jrb.request(request);
                jrb.requestContentType(request);
                break;
            // InternalServerError
            case 500:
                jrb.randomIncidentId();
                jrb.internalError(ex);
                jrb.request(request);
                jrb.log(logger, Level.SEVERE, Optional.of(ex));
                break;
            // ServiceUnavailable
            case 503:
                if ( (ex.getMessage()+"").toLowerCase().startsWith("datafile")) {
                    jrb.message(ex.getMessage());
                } else {
                    jrb.message("Requested service or method not available on the requested object");
                }
                break;
            default:
                jrb.message(ex.getMessage());
                break;
        }
    
        // Logging for debugging. Will not double-log messages.
        jrb.log(logger, Level.FINEST, Optional.of(ex));
        return jrb.build();
    }
    
}
