package edu.harvard.iq.dataverse.api.errorhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.UUID;

import static edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO.errorResponse;

@Provider
public class GeneralExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger logger = LoggerFactory.getLogger(GeneralExceptionHandler.class);

    @Context
    HttpServletRequest request;


    @Override
    public Response toResponse(Exception exception) {
        String incidentId = UUID.randomUUID().toString();
        logger.error("API internal error [incidentId: " + incidentId + "]" , exception);

        return Response.status(500)
                .entity(errorResponse(500, "Internal server error. More details available at the server logs.")
                            .withIncidentId(incidentId))
                .type("application/json").build();
    }

}
