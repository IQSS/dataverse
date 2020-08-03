/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.util.BundleUtil;
import javax.json.Json;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author qqmyers
 */
@Provider
public class RedirectionExceptionHandler implements ExceptionMapper<RedirectionException> {
    
    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(RedirectionException ex) {
        return ex.getResponse();
    }
    
}
