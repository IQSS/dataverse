package edu.harvard.iq.dataverse.api.filters;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import java.io.IOException;

import static edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO.errorResponse;


/**
 * Filter that processes api requests marked with {@link ApiWriteOperation}.
 * <br/>
 * It blocks request processing when Dataverse installation
 * is in readonly mode.
 *
 * @author madryk
 */
@Provider
@ApiWriteOperation
public class ApiReadonlyInstanceFilter implements ContainerRequestFilter {

    @Inject
    private SystemConfig systemConfig;

    // -------------------- LOGIC --------------------

    /**
     * Aborts further request processing with error
     * when Dataverse installation is in readonly mode.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        if (systemConfig.isReadonlyMode()) {
            requestContext.abortWith(
                    Response.status(500)
                    .entity(errorResponse(500, "Instance is readonly"))
                    .type("application/json")
                    .build());
        }
    }
}
