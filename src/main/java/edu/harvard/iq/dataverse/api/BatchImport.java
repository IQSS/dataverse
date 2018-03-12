package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.io.PrintWriter;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Stateless
@Path("batch")
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

    /**
     * migrate - only needed for importing studies from old DVN installations
     * into Dataverse 4.0 read ddi files from the filesystem, and import them in
     * "migrate" mode
     *
     * @param fileDir - the full path of the parent directory where the files
     * are located. If there are subdirectories, then ddi's will be imported
     * into the dataverse matching the subdirectory name (alias)
     * @param parentIdtf - the dataverse that the top-level files should be
     * imported to - if null, then use root dataverse.
     * @param apiKey - users's apiKey
     * @return
     */
    @GET
    @Path("migrate")
    public Response migrate(@QueryParam("path") String fileDir, @QueryParam("dv") String parentIdtf, @QueryParam("createDV") Boolean createDV, @QueryParam("key") String apiKey) throws IOException {
        return startBatchJob(fileDir, parentIdtf, apiKey, ImportType.MIGRATION, createDV);
    }

    @GET
    @Path("harvest")
    public Response harvest(@QueryParam("path") String fileDir, @QueryParam("dv") String parentIdtf, @QueryParam("createDV") Boolean createDV, @QueryParam("key") String apiKey) throws IOException {
        return startBatchJob(fileDir, parentIdtf, apiKey, ImportType.HARVEST, createDV);

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
    @Path("import")
    public Response postImport(String body, @QueryParam("dv") String parentIdtf, @QueryParam("key") String apiKey) {

        DataverseRequest dataverseRequest;
        try {
            dataverseRequest = createDataverseRequest(findAuthenticatedUserOrDie());
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
    @Path("import")
    public Response getImport(@QueryParam("path") String fileDir, @QueryParam("dv") String parentIdtf, @QueryParam("createDV") Boolean createDV, @QueryParam("key") String apiKey) {

        return startBatchJob(fileDir, parentIdtf, apiKey, ImportType.NEW, createDV);

    }

    private Response startBatchJob(String fileDir, String parentIdtf, String apiKey, ImportType importType, Boolean createDV) {
        if (createDV == null) {
            createDV = Boolean.FALSE;
        }
        try {
            DataverseRequest dataverseRequest;
            try {
                dataverseRequest = createDataverseRequest(findAuthenticatedUserOrDie());
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }
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
