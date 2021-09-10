package edu.harvard.iq.dataverse.api.errorhandlers;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO.errorResponse;

/**
 * Produces custom 404 messages for the API.
 *
 * @author michael
 */
@Provider
public class NotFoundExceptionHandler implements ExceptionMapper<NotFoundException> {

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(NotFoundException ex) {
        String uri = request.getRequestURI();
        String exMessage = ex.getMessage();
        String outputMessage;
        if (exMessage != null && exMessage.toLowerCase().startsWith("datafile")) {
            outputMessage = exMessage;
        } else {
            outputMessage = "endpoint does not exist on this server. Please check your code for typos, or consult our API guide at https://repod.icm.edu.pl/guides/en/4.11/user/index.html.";
        }
        return Response.status(404)
                .entity(errorResponse(404, "'" + uri + "' " + outputMessage))
                .type("application/json").build();


    }

}

