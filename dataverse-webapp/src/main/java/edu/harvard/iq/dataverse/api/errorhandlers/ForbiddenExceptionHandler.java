package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO.errorResponse;

/**
 * Produces custom 403 messages for the API.
 *
 * @author michael
 */
@Provider
public class ForbiddenExceptionHandler implements ExceptionMapper<ForbiddenException> {

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(ForbiddenException ex) {
        String uri = request.getRequestURI();
        return Response.status(403)
                .entity(errorResponse(403, "'" + uri + "' you are not authorized to access this object via this api endpoint. "
                                            + "Please check your code for typos, or consult our API guide at https://repod.icm.edu.pl/guides/en/4.11/user/index.html."))
                .type("application/json").build();


    }

}

