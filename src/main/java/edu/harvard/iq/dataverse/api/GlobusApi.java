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
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.globus.AccessToken;
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;


import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.*;
import javax.json.stream.JsonParsingException;
import javax.persistence.NoResultException;
import javax.persistence.Query;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


import  edu.harvard.iq.dataverse.api.Datasets;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

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

    @EJB
    IngestServiceBean ingestService;


    @Inject
    DataverseRequestServiceBean dvRequestService;


    @POST
    @Path("{id}/add")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response globus(@PathParam("id") String datasetId,
                           @FormDataParam("jsonData") String jsonData
    )
    {
        JsonArrayBuilder jarr = Json.createArrayBuilder();

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
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }


        // -------------------------------------
        // (3) Parse JsonData
        // -------------------------------------

        String taskIdentifier = null;

        msgt("******* (api) jsonData 1: " + jsonData);

        JsonObject jsonObject = null;
        try (StringReader rdr = new StringReader(jsonData)) {
            jsonObject = Json.createReader(rdr).readObject();
        } catch (Exception jpe) {
            jpe.printStackTrace();
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}");
        }

        // -------------------------------------
        // (4) Get taskIdentifier
        // -------------------------------------


        taskIdentifier = jsonObject.getString("taskIdentifier");
        msgt("******* (api) newTaskIdentifier: " + taskIdentifier);

        // -------------------------------------
        // (5) Wait until task completion
        // -------------------------------------

        boolean success = false;

        do {
            try {
                String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
                basicGlobusToken = "ODA0ODBhNzEtODA5ZC00ZTJhLWExNmQtY2JkMzA1NTk0ZDdhOmQvM3NFd1BVUGY0V20ra2hkSkF3NTZMWFJPaFZSTVhnRmR3TU5qM2Q3TjA9";
                msgt("******* (api) basicGlobusToken: " + basicGlobusToken);
                AccessToken clientTokenUser = globusServiceBean.getClientToken(basicGlobusToken);

                success = globusServiceBean.getSuccessfulTransfers(clientTokenUser, taskIdentifier);
                msgt("******* (api) success: " + success);

            } catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex.getMessage());
                return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to get task id");
            }

        } while (!success);


        try
        {
            StorageIO<Dataset> datasetSIO = DataAccess.getStorageIO(dataset);

            DataverseRequest dvRequest2 = createDataverseRequest(authUser);
            AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                    ingestService,
                    datasetService,
                    fileService,
                    permissionSvc,
                    commandEngine,
                    systemConfig);

            // -------------------------------------
            // (6) Parse files information from jsondata
            //  calculate checksum
            //  determine mimetype
            // -------------------------------------

            JsonArray filesJson = jsonObject.getJsonArray("files");

            if (filesJson != null) {
                for (JsonObject fileJson : filesJson.getValuesAs(JsonObject.class)) {

                    for (S3ObjectSummary s3ObjectSummary : datasetSIO.listAuxObjects("")) {

                    }

                    String storageIdentifier = fileJson.getString("storageIdentifier");
                    String suppliedContentType = fileJson.getString("contentType");
                    String fileName = fileJson.getString("fileName");

                    String fullPath = datasetSIO.getStorageLocation() + "/" + storageIdentifier.replace("s3://", "");

                    String bucketName = System.getProperty("dataverse.files." + storageIdentifier.split(":")[0] + ".bucket-name");

                    String dbstorageIdentifier = storageIdentifier.split(":")[0] + "://" + bucketName + ":" + storageIdentifier.replace("s3://", "");

                    Query query = em.createQuery("select object(o) from DvObject as o where o.storageIdentifier = :storageIdentifier");
                    query.setParameter("storageIdentifier", dbstorageIdentifier);

                    msgt("*******  dbstorageIdentifier :" + dbstorageIdentifier + " ======= query.getResultList().size()============== " + query.getResultList().size());


                    if (query.getResultList().size() > 0) {

                        JsonObjectBuilder fileoutput= Json.createObjectBuilder()
                                .add("storageIdentifier " , storageIdentifier)
                                .add("message " , " The datatable is not updated since the Storage Identifier already exists in dvObject. ");

                        jarr.add(fileoutput);
                    } else {

                        // Default to suppliedContentType if set or the overall undetermined default if a contenttype isn't supplied
                        String finalType = StringUtils.isBlank(suppliedContentType) ? FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT : suppliedContentType;
                        String type = FileUtil.determineFileTypeByExtension(fileName);
                        if (!StringUtils.isBlank(type)) {
                            //Use rules for deciding when to trust browser supplied type
                            if (FileUtil.useRecognizedType(finalType, type)) {
                                finalType = type;
                            }
                            logger.info("Supplied type: " + suppliedContentType + ", finalType: " + finalType);
                        }

                        JsonPatch path = Json.createPatchBuilder().add("/mimeType", finalType).build();
                        fileJson = path.apply(fileJson);

                        StorageIO<DvObject> dataFileStorageIO = DataAccess.getDirectStorageIO(fullPath);
                        InputStream in = dataFileStorageIO.getInputStream();
                        String checksumVal = FileUtil.calculateChecksum(in, DataFile.ChecksumType.MD5);

                        path = Json.createPatchBuilder().add("/md5Hash", checksumVal).build();
                        fileJson = path.apply(fileJson);

                        //addGlobusFileToDataset(dataset, fileJson.toString(), addFileHelper, fileName, finalType, storageIdentifier);


                        if (!systemConfig.isHTTPUpload()) {
                            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
                        }


                        //------------------------------------
                        // (1) Make sure dataset does not have package file
                        // --------------------------------------

                        for (DatasetVersion dv : dataset.getVersions()) {
                            if (dv.isHasPackageFile()) {
                                return error(Response.Status.FORBIDDEN,
                                        BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                                );
                            }
                        }

                        //---------------------------------------
                        // (2) Load up optional params via JSON
                        //---------------------------------------

                        OptionalFileParams optionalFileParams = null;
                        msgt("(api) jsonData 2: " +  fileJson.toString());

                        try {
                            optionalFileParams = new OptionalFileParams(fileJson.toString());
                        } catch (DataFileTagException ex) {
                            return error( Response.Status.BAD_REQUEST, ex.getMessage());
                        }


                        //-------------------
                        // (3) Create the AddReplaceFileHelper object
                        //-------------------
                        msg("ADD!");

                        //-------------------
                        // (4) Run "runAddFileByDatasetId"
                        //-------------------
                        addFileHelper.runAddFileByDataset(dataset,
                                fileName,
                                finalType,
                                storageIdentifier,
                                null,
                                optionalFileParams);


                        if (addFileHelper.hasError()){

                            JsonObjectBuilder fileoutput= Json.createObjectBuilder()
                                    .add("storageIdentifier " , storageIdentifier)
                                    .add("error Code: " ,addFileHelper.getHttpErrorCode().toString())
                                    .add("message " ,  addFileHelper.getErrorMessagesAsString("\n"));

                            jarr.add(fileoutput);

                        }else{
                            String successMsg = BundleUtil.getStringFromBundle("file.addreplace.success.add");

                            JsonObject a1 = addFileHelper.getSuccessResultAsJsonObjectBuilder().build();

                            JsonArray f1 = a1.getJsonArray("files");
                            JsonObject file1 = f1.getJsonObject(0);

                            try {
                                //msgt("as String: " + addFileHelper.getSuccessResult());

                                logger.fine("successMsg: " + successMsg);
                                String duplicateWarning = addFileHelper.getDuplicateFileWarning();
                                if (duplicateWarning != null && !duplicateWarning.isEmpty()) {
                                   // return ok(addFileHelper.getDuplicateFileWarning(), addFileHelper.getSuccessResultAsJsonObjectBuilder());
                                    JsonObjectBuilder fileoutput= Json.createObjectBuilder()
                                            .add("storageIdentifier " , storageIdentifier)
                                            .add("warning message: " ,addFileHelper.getDuplicateFileWarning())
                                            .add("message " ,  file1);
                                    jarr.add(fileoutput);

                                } else {
                                    JsonObjectBuilder fileoutput= Json.createObjectBuilder()
                                            .add("storageIdentifier " , storageIdentifier)
                                            .add("message " ,  file1);
                                    jarr.add(fileoutput);
                                }

                                //"Look at that!  You added a file! (hey hey, it may have worked)");
                            } catch (Exception ex) {
                                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
                                return error(Response.Status.BAD_REQUEST, "NoFileException!  Serious Error! See administrator!");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            String message = e.getMessage();
            msgt("*******   Exception from globus API call " + message);
            msgt("*******  datasetId :" + dataset.getId() + " ======= GLOBUS  CALL Exception ============== " + message);
            e.printStackTrace();
        }
        return ok(Json.createObjectBuilder().add("Files", jarr));

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

    public  Response addGlobusFileToDataset( Dataset dataset,
                                          String jsonData, AddReplaceFileHelper addFileHelper,String fileName,
                                            String finalType,
                                            String storageIdentifier
    ){


        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }


        //------------------------------------
        // (1) Make sure dataset does not have package file
        // --------------------------------------

        for (DatasetVersion dv : dataset.getVersions()) {
            if (dv.isHasPackageFile()) {
                return error(Response.Status.FORBIDDEN,
                        BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                );
            }
        }

        //---------------------------------------
        // (2) Load up optional params via JSON
        //---------------------------------------

        OptionalFileParams optionalFileParams = null;
        msgt("(api) jsonData 2: " +  jsonData);

        try {
            optionalFileParams = new OptionalFileParams(jsonData);
        } catch (DataFileTagException ex) {
            return error( Response.Status.BAD_REQUEST, ex.getMessage());
        }


        //-------------------
        // (3) Create the AddReplaceFileHelper object
        //-------------------
        msg("ADD!");

        //-------------------
        // (4) Run "runAddFileByDatasetId"
        //-------------------
        addFileHelper.runAddFileByDataset(dataset,
                fileName,
                finalType,
                storageIdentifier,
                null,
                optionalFileParams);


        if (addFileHelper.hasError()){
            return error(addFileHelper.getHttpErrorCode(), addFileHelper.getErrorMessagesAsString("\n"));
        }else{
            String successMsg = BundleUtil.getStringFromBundle("file.addreplace.success.add");
            try {
                //msgt("as String: " + addFileHelper.getSuccessResult());

                logger.fine("successMsg: " + successMsg);
                String duplicateWarning = addFileHelper.getDuplicateFileWarning();
                if (duplicateWarning != null && !duplicateWarning.isEmpty()) {
                    return ok(addFileHelper.getDuplicateFileWarning(), addFileHelper.getSuccessResultAsJsonObjectBuilder());
                } else {
                    return ok(addFileHelper.getSuccessResultAsJsonObjectBuilder());
                }

                //"Look at that!  You added a file! (hey hey, it may have worked)");
            } catch (NoFilesException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
                return error(Response.Status.BAD_REQUEST, "NoFileException!  Serious Error! See administrator!");

            }
        }



    } // end: addFileToDataset

}
