package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.common.BundleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO.errorResponse;

/**
 * @author skraffmi
 */
@Provider
public class BadRequestExceptionHandler implements ExceptionMapper<BadRequestException> {

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(BadRequestException ex) {
        System.out.print(ex.getMessage());
        String uri = request.getRequestURI();
        String exMessage = ex.getMessage();
        String outputMessage;
        if (exMessage != null && exMessage.toLowerCase().startsWith("tabular data required")) {
            outputMessage = BundleUtil.getStringFromBundle("access.api.exception.metadata.not.available.for.nontabular.file");
        } else {
            outputMessage = "Bad Request. The API request cannot be completed with the parameters supplied. Please check your code for typos, or consult our API guide at https://repod.icm.edu.pl/guides/en/4.11/user/index.html.";
        }
        return Response.status(400)
                .entity(errorResponse(400, "'" + uri + "' " + outputMessage))
                .type("application/json").build();


    }

}
