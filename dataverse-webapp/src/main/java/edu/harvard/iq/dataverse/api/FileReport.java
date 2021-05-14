package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.dataset.DatasetReportService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.time.LocalDateTime;

@Path("reports/files")
public class FileReport extends AbstractApiBean {

    @Inject
    private DatasetReportService datasetReportService;

    // -------------------- LOGIC --------------------

    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response createFileReport() {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
        } catch (WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }

        StreamingOutput reportStreamer = output -> datasetReportService.createReport(output);
        String fileName = "\"report-" + LocalDateTime.now() + ".csv\"";

        return Response.ok(reportStreamer, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .build();

    }
}
