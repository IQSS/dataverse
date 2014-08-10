package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.User;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("browse")
public class Browse {

    private static final Logger logger = Logger.getLogger(Browse.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseUserServiceBean dataverseUserService;
    @EJB
    PermissionServiceBean permissionService;

    // this is highly experimental
    @GET
    @Path("{user}")
    public String browseByUser(@PathParam("user") String username) {
        User dataverseUser = dataverseUserService.findByIdentifier(username);
        if (dataverseUser != null) {
            List<Dataset> datasetsByUser = new ArrayList<>();
            List<Dataset> allDatasets = datasetService.findAll();
            Permission permission = Permission.Discover;
            for (Dataset dataset : allDatasets) {
                if (permissionService.permissionsFor(dataverseUser, dataset).contains(permission)) {
                    datasetsByUser.add(dataset);
                }
            }
            return username + " has permission \"" + permission + "\" (" + permission.getHumanName() + ") to these datasets: " + datasetsByUser + "\n";
        } else {
            return "User " + username + " could not be found!\n";
        }
    }

    @GET
    public String browse() throws FileNotFoundException {
        try {
            List<Dataverse> dataverses = dataverseService.findAll();
            JsonArrayBuilder filesArrayBuilder = Json.createArrayBuilder();
            JsonArrayBuilder dataversesArrayBuilder = Json.createArrayBuilder();
            JsonArrayBuilder datasetsArrayBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dataverses) {
                logger.info("dataverse: " + dataverse.getAlias());
                JsonObjectBuilder dataverseInfoBuilder = Json.createObjectBuilder().add("alias", dataverse.getAlias());
                dataversesArrayBuilder.add(dataverseInfoBuilder);
                Long ownerId = dataverse.getId();
                List<Dataset> datasets = datasetService.findByOwnerId(ownerId);
                for (Dataset dataset : datasets) {
                    //logger.info("dataset: " + dataset.getTitle());
                    String datasetInfo = dataverse.getAlias();// + "|" + dataset.getTitle();
                    JsonObjectBuilder datasetObjectBuilder = Json.createObjectBuilder().add("datasetInfo", datasetInfo);
                    datasetsArrayBuilder.add(datasetObjectBuilder);
                    List<DataFile> files = dataset.getFiles();
                    for (DataFile file : files) {
                        logger.info("file: " + file.getFileMetadata().getLabel());
                        String fileInfo = dataverse.getAlias();// + "|" + dataset.getTitle() + "|" + file.getName();
                        JsonObjectBuilder fileInfoBuilder = Json.createObjectBuilder().add("fileInfo", fileInfo);
                        filesArrayBuilder.add(fileInfoBuilder);
                    }
                }
            }
            JsonObject jsonObject = Json.createObjectBuilder()
                    .add("dataverses_total_count", dataversesArrayBuilder.build().size())
                    .add("dataverses", dataversesArrayBuilder)
                    .add("datasets_total_count", datasetsArrayBuilder.build().size())
                    .add("datasets", datasetsArrayBuilder)
                    .add("files_total_count", filesArrayBuilder.build().size())
                    .add("files", filesArrayBuilder)
                    .build();
            return Util.jsonObject2prettyString(jsonObject);
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>> for " + violation.getPropertyPath() + " at " + violation.getLeafBean() + " - " + violation.getMessage() + ")");
                    }
                }
            }
            return Util.message2ApiError(sb.toString());
        } catch (Exception ex) {
            StackTraceElement stacktrace = ex.getStackTrace()[0];
            if (stacktrace != null) {
                String javaFile = stacktrace.getFileName();
                String methodName = stacktrace.getMethodName();
                int lineNumber = stacktrace.getLineNumber();
                String error = "Browsing failed. " + ex.getClass().getCanonicalName() + " on line " + javaFile + ":" + lineNumber + " (method: " + methodName + ")";
                logger.info(error);
                /**
                 * @todo use Util.message2ApiError() instead
                 */

                JsonObject jsonObject = Json.createObjectBuilder()
                        .add("message", "Error")
                        .add("documentation_url", "http://thedata.org")
                        .add("errors", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("code", error)))
                        .build();
                logger.info("jsonObject: " + jsonObject);
                return Util.jsonObject2prettyString(jsonObject);
            } else {
                return null;
            }
        }
    }

}
