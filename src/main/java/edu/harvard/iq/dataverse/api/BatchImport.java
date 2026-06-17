package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Stateless
@Path("batch")
@Tag(name = "Admin", description = "Administrative Dataverse operations.")
public class BatchImport extends AbstractApiBean {

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetFieldServiceBean datasetfieldService;
    @EJB
    MetadataBlockServiceBean metadataBlockService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    ImportServiceBean importService;
    @EJB
    BatchServiceBean batchService;

    @GET
    @AuthRequired
    @Path("harvest")
    @Operation(summary = "Starts a harvest batch import",
            description = "Starts a background harvest import from a server-side file or directory into the specified dataverse.")
    public Response harvest(@Context ContainerRequestContext crc,
            @Parameter(description = "Server-side file or directory path to import.", required = true)
            @QueryParam("path") String fileDir,
            @Parameter(description = "Target dataverse id or alias; defaults to root when omitted.")
            @QueryParam("dv") String parentIdtf,
            @Parameter(description = "Create the target dataverse when it does not exist.")
            @QueryParam("createDV") Boolean createDV,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) throws IOException {
        try {
            return startBatchJob(getRequestAuthenticatedUserOrDie(crc), fileDir, parentIdtf, apiKey, ImportType.HARVEST, createDV);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    /**
     * Import a new Dataset with DDI xml data posted in the request
     *
     * @param body the xml
     * @param parentIdtf the dataverse to import into (id or alias)
     * @param apiKey user's api key
     * @return import status (including id of the dataset created)
     */
    @POST
    @AuthRequired
    @Path("import")
    @Operation(summary = "Imports a dataset from DDI XML",
            description = "Imports a new dataset into the specified dataverse from DDI XML supplied in the request body.")
    public Response postImport(@Context ContainerRequestContext crc,
            @RequestBody(description = "DDI XML dataset metadata to import as a new dataset.")
            String body,
            @Parameter(description = "Target dataverse id or alias; defaults to root when omitted.")
            @QueryParam("dv") String parentIdtf,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) {

        DataverseRequest dataverseRequest;
        try {
            dataverseRequest = createDataverseRequest(getRequestAuthenticatedUserOrDie(crc));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        if (parentIdtf == null) {
            parentIdtf = "root";
        }
        Dataverse owner = findDataverse(parentIdtf);
        if (owner == null) {
            return error(Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
        }
        try {
            PrintWriter cleanupLog = null; // Cleanup log isn't needed for ImportType == NEW. We don't do any data cleanup in this mode.
            String filename = null;  // Since this is a single input from a POST, there is no file that we are reading from.
            JsonObjectBuilder status = importService.doImport(dataverseRequest, owner, body, filename, ImportType.NEW, cleanupLog);
            return this.ok(status);
        } catch (ImportException | IOException e) {
            return this.error(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Import single or multiple datasets that are in the local filesystem
     *
     * @param fileDir the absolute path of the file or directory (all files
     * within the directory will be imported
     * @param parentIdtf the dataverse to import into (id or alias)
     * @param apiKey user's api key
     * @return import status (including id's of the datasets created)
     */
    @GET
    @AuthRequired
    @Path("import")
    @Operation(summary = "Starts a dataset batch import",
            description = "Starts a background import of one or more datasets from a server-side file or directory into the specified dataverse.")
    public Response getImport(@Context ContainerRequestContext crc,
            @Parameter(description = "Server-side file or directory path to import.", required = true)
            @QueryParam("path") String fileDir,
            @Parameter(description = "Target dataverse id or alias; defaults to root when omitted.")
            @QueryParam("dv") String parentIdtf,
            @Parameter(description = "Create the target dataverse when it does not exist.")
            @QueryParam("createDV") Boolean createDV,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) {
        try {
            return startBatchJob(getRequestAuthenticatedUserOrDie(crc), fileDir, parentIdtf, apiKey, ImportType.NEW, createDV);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    private Response startBatchJob(User user, String fileDir, String parentIdtf, String apiKey, ImportType importType, Boolean createDV) {
        if (createDV == null) {
            createDV = Boolean.FALSE;
        }
        try {
            DataverseRequest dataverseRequest;
            dataverseRequest = createDataverseRequest(user);
            if (parentIdtf == null) {
                parentIdtf = "root";
            }
            Dataverse owner = findDataverse(parentIdtf);
            if (owner == null) {
                if (createDV) {
                    owner = importService.createDataverse(parentIdtf, dataverseRequest);
                } else {
                    return error(Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
                }
            }
            batchService.processFilePath(fileDir, parentIdtf, dataverseRequest, owner, importType, createDV);

        } catch (ImportException e) {
            return this.error(Response.Status.BAD_REQUEST, "Import Exception, " + e.getMessage());
        }
        return this.accepted();
    }

}
