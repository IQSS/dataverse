package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO.errorResponse;

@Provider
public class NotAllowedExceptionHandler implements ExceptionMapper<NotAllowedException> {

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(NotAllowedException ex) {

        String uri = request.getRequestURI();
        return Response.status(405)
                .entity(errorResponse(405,
                            "'" + uri + "' endpoint does not support method '" + request.getMethod() + "'. Consult our API guide at https://repod.icm.edu.pl/guides/en/4.11/user/index.html."))
                .type("application/json").build();


    }

}
