package edu.harvard.iq.dataverse.api;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.*;

import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.globus.AccessToken;
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;


import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.stream.JsonParsingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Path("globus")
public class GlobusApi extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Access.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    GlobusServiceBean globusServiceBean;

    @EJB
    EjbDataverseEngine commandEngine;

    @EJB
    PermissionServiceBean permissionService;

    @Inject
    DataverseRequestServiceBean dvRequestService;


    @POST
    @Path("{id}/add")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response globus(@PathParam("id") String datasetId,
                           @FormDataParam("jsonData") String jsonData
    ) {

        // -------------------------------------
        // (1) Get the user from the API key
        // -------------------------------------
        User authUser;
        try {
            authUser = findUserOrDie();
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN,
                    BundleUtil.getStringFromBundle("file.addreplace.error.auth")
            );
        }

        // -------------------------------------
        // (2) Get the User ApiToken
        // -------------------------------------
        ApiToken token = authSvc.findApiTokenByUser((AuthenticatedUser)authUser);

        // -------------------------------------
        // (3) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        // -------------------------------------
        // (4) Parse JsonData
        // -------------------------------------

        String taskIdentifier = null;

        msgt("******* (api) jsonData: " + jsonData);

        JsonObject jsonObject = null;
        try (StringReader rdr = new StringReader(jsonData)) {
            jsonObject = Json.createReader(rdr).readObject();
        } catch (Exception jpe) {
            jpe.printStackTrace();
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}");
        }

        // -------------------------------------
        // (5) Get taskIdentifier
        // -------------------------------------


        taskIdentifier = jsonObject.getString("taskIdentifier");
        msgt("******* (api) newTaskIdentifier: " + taskIdentifier);

        // -------------------------------------
        // (6) Wait until task completion
        // -------------------------------------

        boolean success = false;

        do {
            try {
                String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
                basicGlobusToken = "ODA0ODBhNzEtODA5ZC00ZTJhLWExNmQtY2JkMzA1NTk0ZDdhOmQvM3NFd1BVUGY0V20ra2hkSkF3NTZMWFJPaFZSTVhnRmR3TU5qM2Q3TjA9";
                msgt("******* (api) basicGlobusToken: " + basicGlobusToken);
                AccessToken clientTokenUser = globusServiceBean.getClientToken(basicGlobusToken);

                success = globusServiceBean.getSuccessfulTransfers(clientTokenUser, taskIdentifier ) ;
                msgt("******* (api) success: " + success);

            } catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex.getMessage());
                return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to get task id" );
            }

        } while (!success);

        // -------------------------------------
        // (6) Parse files information from jsondata and add to dataset
        // -------------------------------------

        try {
            String directory = null;
            StorageIO<Dataset> datasetSIO = DataAccess.getStorageIO(dataset);

            directory = dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage();

            JsonArray filesJson = jsonObject.getJsonArray("files");

            if (filesJson != null) {
                for (JsonObject fileJson : filesJson.getValuesAs(JsonObject.class)) {

                    for (S3ObjectSummary s3ObjectSummary : datasetSIO.listAuxObjects("")) {

                    }

                    String storageIdentifier = fileJson.getString("storageIdentifier");

                    String s = datasetSIO.getStorageLocation();

                    String fullPath = s + "/" + storageIdentifier.replace("s3://", "");

                    StorageIO<DvObject> dataFileStorageIO = DataAccess.getDirectStorageIO(fullPath);
                    InputStream in = dataFileStorageIO.getInputStream();

                    String checksumVal =  FileUtil.calculateChecksum(in, DataFile.ChecksumType.MD5);

                    JsonPatch path = Json.createPatchBuilder().add("/md5Hash",checksumVal).build();
                    fileJson = path.apply(fileJson);

                    String requestUrl = httpRequest.getRequestURL().toString() ;

                    ProcessBuilder processBuilder = new ProcessBuilder();

                    String command = "curl -H \"X-Dataverse-key:" + token.getTokenString() + "\" -X POST " + httpRequest.getProtocol() +"//" + httpRequest.getServerName() + "/api/datasets/:persistentId/add?persistentId=doi:"+ directory + " -F jsonData='"+fileJson.toString() +"'";
                    msgt("*******====command ==== " + command);
                     processBuilder.command("bash", "-c", command);
                    msgt("*******=== Start api/datasets/:persistentId/add call");
                     Process process = processBuilder.start();
                }
            }

        } catch (Exception e) {
            String message = e.getMessage();
            msgt("*******   UNsuccessfully completed "  + message);
            msgt("*******  datasetId :" + dataset.getId() + " ======= GLOBUS  CALL Exception ============== " + message);
            e.printStackTrace();
      }

        msgt("*******   successfully completed " );
        return ok("Async: ====  datasetId :" + dataset.getId() + ": will add files to the table");
    }

    private void msg(String m) {
        //System.out.println(m);
        logger.info(m);
    }

    private void dashes() {
        msg("----------------");
    }

    private void msgt(String m) {
        //dashes();
        msg(m);
        //dashes();
    }

}
