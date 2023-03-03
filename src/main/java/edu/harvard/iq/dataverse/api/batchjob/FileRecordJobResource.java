package edu.harvard.iq.dataverse.api.batchjob;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.engine.command.impl.ImportFromFileSystemCommand;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

@Stateless
@Path("batch/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class FileRecordJobResource extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(FileRecordJobResource.class.getName());

    @EJB
    PermissionServiceBean permissionServiceBean;

    @EJB
    DatasetServiceBean datasetService;

    @POST
    @AuthRequired
    @Path("import/datasets/files/{identifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilesystemImport(@Context ContainerRequestContext crc,
                                        @PathParam("identifier") String identifier,
                                        @QueryParam("mode") @DefaultValue("MERGE") String mode,
                                        /*@QueryParam("fileMode") @DefaultValue("package_file") String fileMode*/
                                        @QueryParam("uploadFolder") String uploadFolder,
                                        @QueryParam("totalSize") Long totalSize) {
        return response(req -> {
            ImportMode importMode = ImportMode.MERGE;
            // Switch to this if you ever need to use something other than MERGE.
            //ImportMode importMode = ImportMode.valueOf(mode);
            JsonObject jsonObject = execCommand(new ImportFromFileSystemCommand(req, findDatasetOrDie(identifier), uploadFolder, totalSize, importMode));
            String returnString = jsonObject.getString("message");
            if (!returnString.equals("FileSystemImportJob in progress")) {
                return error(Response.Status.INTERNAL_SERVER_ERROR, returnString);
            }
            return ok(Json.createObjectBuilder()
                    .add("message", returnString)
                    .add("executionId", jsonObject.getInt("executionId"))
            );
        }, getRequestUser(crc));
    }

}
