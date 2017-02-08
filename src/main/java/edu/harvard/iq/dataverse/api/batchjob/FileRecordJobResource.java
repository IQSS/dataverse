package edu.harvard.iq.dataverse.api.batchjob;

import com.google.common.base.Strings;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.jobs.importer.filesystem.FileRecordWriter;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchRuntime;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

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
    @Path("import/datasets/files/{doi1}/{doi2}/{doi3}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilesystemImport(@PathParam("doi1") String doi1, 
                                        @PathParam("doi2") String doi2,
                                        @PathParam("doi3") String doi3,
                                        @QueryParam("mode") @DefaultValue("MERGE") String mode,
                                        /*@QueryParam("fileMode") @DefaultValue("package_file") String fileMode*/
                                        @QueryParam("uploadFolder") String uploadFolder,
                                        @QueryParam("totalSize") Long totalSize) {

        /* 
           batch import as-individual-datafiles is disabled in this iteration; 
           only the import-as-a-package is allowed. -- L.A. Feb 2 2017
        */
        String fileMode = FileRecordWriter.FILE_MODE_PACKAGE_FILE;
        try {
            
            String doi = "doi:" + doi1 + "/" + doi2 + "/" + doi3;
            try {
                Dataset dataset = datasetService.findByGlobalId(doi);
                AuthenticatedUser user = findAuthenticatedUserOrDie();
                /**
                 * Current constraints:
                 * 1. only supports merge and replace mode
                 * 2. valid dataset
                 * 3. valid dataset directory
                 * 4. valid user & user has edit dataset permission
                 * 5. only one dataset version
                 * 6. dataset version is draft
                 */

                if (!mode.equalsIgnoreCase("MERGE") && !mode.equalsIgnoreCase("REPLACE")) {
                    return error(Response.Status.NOT_IMPLEMENTED, "Import mode: " + mode + " is not currently supported.");
                }
                if (!fileMode.equals(FileRecordWriter.FILE_MODE_INDIVIDUAL_FILES) && !fileMode.equals(FileRecordWriter.FILE_MODE_PACKAGE_FILE)) {
                    return error(Response.Status.NOT_IMPLEMENTED, "File import mode: " + fileMode + " is not supported.");
                }
                if (dataset == null) {
                    return error(Response.Status.BAD_REQUEST, "Can't find dataset with ID: " + doi);
                }
                File directory = new File(System.getProperty("dataverse.files.directory")
                        + File.separator + dataset.getAuthority() + File.separator + dataset.getIdentifier());
                if (!isValidDirectory(directory)) {
                    return error(Response.Status.BAD_REQUEST, "Dataset directory is invalid.");    
                }
                
                if (Strings.isNullOrEmpty(uploadFolder)) {
                    return error(Response.Status.BAD_REQUEST, "No uploadFolder specified");
                }
                
                File uploadDirectory = new File(System.getProperty("dataverse.files.directory")
                        + File.separator + dataset.getAuthority() + File.separator + dataset.getIdentifier()
                        + File.separator + uploadFolder);
                if (!isValidDirectory(uploadDirectory)) {
                    return error(Response.Status.BAD_REQUEST, "Upload folder is not a valid directory.");
                }

                // check if user has permission to update the dataset
                boolean canIssueCommand = permissionServiceBean
                        .requestOn(this.createDataverseRequest(user), 
                                dataset).canIssue(UpdateDatasetCommand.class);
                if (!canIssueCommand) {
                    logger.log(Level.SEVERE, "User doesn't have permission to import files into this dataset.");
                    return error(Response.Status.FORBIDDEN, "User is not authorized.");                    }

                if (dataset.getVersions().size() != 1) {
                    logger.log(Level.SEVERE, "File system import is currently only supported for datasets with one version.");
                    return error(Response.Status.BAD_REQUEST, "Error creating FilesystemImportJob with dataset with ID: " + doi
                                + " - Dataset has more than one version.");                        
                }

                if (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) {
                    logger.log(Level.SEVERE, "File system import is currently only supported for DRAFT versions.");
                    return error(Response.Status.BAD_REQUEST, "Error creating FilesystemImportJob with dataset with ID: " + doi
                                            + " - Dataset isn't in DRAFT mode.");
                }
                
                try {
                    long jid;
                    Properties props = new Properties();
                    props.setProperty("datasetId", doi);
                    props.setProperty("userId", user.getIdentifier().replace("@", ""));
                    props.setProperty("mode", mode);
                    props.setProperty("fileMode", fileMode);
                    props.setProperty("uploadFolder", uploadFolder);
                    if (totalSize != null && totalSize > 0) {
                        props.setProperty("totalSize", totalSize.toString());
                    }
                    JobOperator jo = BatchRuntime.getJobOperator();
                    jid = jo.start("FileSystemImportJob", props);
                    if (jid > 0) {
                        // build success json response
                        JsonObjectBuilder bld = jsonObjectBuilder();
                        return this.ok(bld.add("executionId", jid).add("message", "FileSystemImportJob in progress"));
                    } else {
                        return error(Response.Status.BAD_REQUEST, "Error creating FilesystemImportJob with dataset with ID: " + doi);
                    }

                } catch (JobStartException | JobSecurityException ex) {
                    logger.log(Level.SEVERE, "Job Error: " + ex.getMessage());
                    return error(Response.Status.BAD_REQUEST,
                            "Error creating FilesystemImportJob with dataset with ID: " + doi + " - " + ex.getMessage());
                }

            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }
            
        } catch (Exception e) {
            return error(Response.Status.BAD_REQUEST, "Import Exception - " + e.getMessage());
        }
    }

    /**
     * Make sure the directory path is truly a directory, exists and we can read it.
     * @return isValid
     */
    private boolean isValidDirectory(File directory) {
        String path = directory.getAbsolutePath();
        if (!directory.exists()) {
            logger.log(Level.SEVERE, "Directory " + path + " does not exist.");
            return false;
        }
        if (!directory.isDirectory()) {
            logger.log(Level.SEVERE, path + " is not a directory.");
            return false;
        }
        if (!directory.canRead()) {
            logger.log(Level.SEVERE, "Unable to read files from directory " + path + ". Permission denied.");
            return false;
        }
        return true;
    }
    
}