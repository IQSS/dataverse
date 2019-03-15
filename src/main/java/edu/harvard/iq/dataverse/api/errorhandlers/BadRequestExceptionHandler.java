/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.util.BundleUtil;
import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author skraffmi
 */
@Provider
public class BadRequestExceptionHandler implements ExceptionMapper<BadRequestException> {
    
    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(BadRequestException ex) {
        System.out.print( ex.getMessage());
        String uri = request.getRequestURI();
        String exMessage = ex.getMessage(); 
        String outputMessage;
        if (exMessage != null && exMessage.toLowerCase().startsWith("tabular data required")) {
            outputMessage = BundleUtil.getStringFromBundle("access.api.exception.metadata.not.available.for.nontabular.file");
        } else {
            outputMessage = "Bad Request. The API request cannot be completed with the parameters supplied. Please check your code for typos, or consult our API guide at http://guides.dataverse.org.";
        }
        return Response.status(400)
                .entity( Json.createObjectBuilder()
                             .add("status", "ERROR")
                             .add("code", 400)
                             .add("message", "'" + uri + "' " + outputMessage)
                        .build())
                .type("application/json").build();
        
       
    }
    
}
