package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("files")
public class Files extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Files.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataFileServiceBean dataFileService;
        
    @POST
    public Response add(DataFile dataFile, @QueryParam("key") String apiKey) {
        Dataset dataset;
        try {
            dataset = datasetService.find(dataFile.getOwner().getId());
        } catch (EJBException ex) {
            return errorResponse( Status.NOT_FOUND, "Couldn't find dataset to save file to. File was " + dataFile);
        }
        List<DataFile> newListOfFiles = dataset.getFiles();
        newListOfFiles.add(dataFile);
        dataset.setFiles(newListOfFiles);
        try {
            User u = findUserByApiToken(apiKey);
            if (u == null) {
                return badApiKey(apiKey);
            }
            engineSvc.submit(new UpdateDatasetCommand(dataset, u));
            String fileName = "[No name?]";
            if (dataFile.getFileMetadata() != null) {
                fileName = dataFile.getFileMetadata().getLabel(); 
            }
            return okResponse( "file " + fileName + " created/updated with dataset " + dataset.getId() 
                               + " (and probably indexed, check server.log)\n");
            
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex);
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
            return errorResponse(Status.INTERNAL_SERVER_ERROR, "POST failed: " + sb.toString());
        } catch (PermissionException ex) {
            return errorResponse( Status.UNAUTHORIZED, "Can't update dataset: " + ex.getMessage());
        } catch (CommandException ex) {
            return errorResponse( Status.INTERNAL_SERVER_ERROR, "Can't update dataset: " + ex.getMessage());
        }
    }

}
