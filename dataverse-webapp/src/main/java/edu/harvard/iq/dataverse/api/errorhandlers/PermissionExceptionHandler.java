package edu.harvard.iq.dataverse.api.errorhandlers;
import edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Provider
public class PermissionExceptionHandler implements ExceptionMapper<PermissionException> {

    @Override
    public Response toResponse(PermissionException exception) {
        return Response.status(FORBIDDEN)
                .entity(ApiErrorResponseDTO.errorResponse(FORBIDDEN.getStatusCode(),
                        "You are not permitted to execute that operation."))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
