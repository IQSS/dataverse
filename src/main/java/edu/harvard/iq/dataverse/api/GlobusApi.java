package edu.harvard.iq.dataverse.api;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.*;

import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;



import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
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
    @Path("{datasetId}")
    public Response globus(@PathParam("datasetId") String datasetId ) {

        logger.info("Async:======Start Async Tasklist == dataset id :"+ datasetId  );
        Dataset dataset = null;
        try {
            dataset = findDatasetOrDie(datasetId);

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        User apiTokenUser = checkAuth(dataset);

        if (apiTokenUser == null) {
            return unauthorized("Access denied");
        }

        try {


            /*
            String lockInfoMessage = "Globus upload in progress";
            DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.GlobusUpload, apiTokenUser != null ? ((AuthenticatedUser)apiTokenUser).getId() : null, lockInfoMessage);
            if (lock != null) {
                dataset.addLock(lock);
            } else {
                logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
            }
            */

            List<FileMetadata> fileMetadatas = new ArrayList<>();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            StorageIO<Dataset> datasetSIO = DataAccess.getStorageIO(dataset);


            String task_id = null;

            String timeWhenAsyncStarted = sdf.format(new Date(System.currentTimeMillis() + (5 * 60 * 60 * 1000)));  // added 5 hrs to match output from globus api

            String endDateTime = sdf.format(new Date(System.currentTimeMillis() + (4 * 60 * 60 * 1000))); // the tasklist will be monitored for 4 hrs
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(sdf.parse(endDateTime));


            do {
                try {
                    String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");

                    task_id = globusServiceBean.getTaskList(basicGlobusToken, dataset.getIdentifierForFileStorage(), timeWhenAsyncStarted);
                    //Thread.sleep(10000);
                    String currentDateTime = sdf.format(new Date(System.currentTimeMillis()));
                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTime(sdf.parse(currentDateTime));

                    if (cal2.after(cal1)) {
                        logger.info("Async:======Time exceeded " + endDateTime + " ====== " + currentDateTime + " ====  datasetId :" + datasetId);
                        break;
                    } else if (task_id != null) {
                        break;
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.info(ex.getMessage());
                    return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to get task id" );
                }

            } while (task_id == null);


            logger.info("Async:======Found matching task id " + task_id + " ====  datasetId :" + datasetId);


            DatasetVersion workingVersion = dataset.getEditVersion();

            if (workingVersion.getCreateTime() != null) {
                workingVersion.setCreateTime(new Timestamp(new Date().getTime()));
            }


            String directory = dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage();

            System.out.println("Async:======= directory ==== " + directory+ " ====  datasetId :" + datasetId);
            Map<String, Integer> checksumMapOld = new HashMap<>();

            Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();

            while (fmIt.hasNext()) {
                FileMetadata fm = fmIt.next();
                if (fm.getDataFile() != null && fm.getDataFile().getId() != null) {
                    String chksum = fm.getDataFile().getChecksumValue();
                    if (chksum != null) {
                        checksumMapOld.put(chksum, 1);
                    }
                }
            }

            List<DataFile> dFileList = new ArrayList<>();
            for (S3ObjectSummary s3ObjectSummary : datasetSIO.listAuxObjects("")) {

                String s3ObjectKey = s3ObjectSummary.getKey();

                String t = s3ObjectKey.replace(directory, "");

                if (t.indexOf(".") > 0) {
                    long totalSize = s3ObjectSummary.getSize();
                    String filePath = s3ObjectKey;
                    String checksumVal = s3ObjectSummary.getETag();

                    if ((checksumMapOld.get(checksumVal) != null)) {
                        logger.info("Async: ====  datasetId :" + datasetId + "======= filename ==== " + filePath + " == file already exists ");
                    } else if (!filePath.contains("cached")) {

                        logger.info("Async: ====  datasetId :" + datasetId + "======= filename ==== " + filePath + " == new file   ");
                        try {

                            DataFile datafile = new DataFile(DataFileServiceBean.MIME_TYPE_GLOBUS_FILE);  //MIME_TYPE_GLOBUS
                            datafile.setModificationTime(new Timestamp(new Date().getTime()));
                            datafile.setCreateDate(new Timestamp(new Date().getTime()));
                            datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));

                            FileMetadata fmd = new FileMetadata();

                            String fileName = filePath.split("/")[filePath.split("/").length - 1];
                            fmd.setLabel(fileName);
                            fmd.setDirectoryLabel(filePath.replace(directory, "").replace(File.separator + fileName, ""));

                            fmd.setDataFile(datafile);

                            datafile.getFileMetadatas().add(fmd);

                            FileUtil.generateS3PackageStorageIdentifier(datafile);
                            logger.info("Async: ====  datasetId :" + datasetId + "======= filename ==== " + filePath + " == added to datafile, filemetadata   ");

                            try {
                                // We persist "SHA1" rather than "SHA-1".
                                datafile.setChecksumType(DataFile.ChecksumType.SHA1);
                                datafile.setChecksumValue(checksumVal);
                            } catch (Exception cksumEx) {
                                logger.info("Async: ====  datasetId :" + datasetId + "======Could not calculate  checksumType signature for the new file ");
                            }

                            datafile.setFilesize(totalSize);

                            dFileList.add(datafile);

                        } catch (Exception ioex) {
                            logger.info("Async: ====  datasetId :" + datasetId + "======Failed to process and/or save the file " + ioex.getMessage());
                            return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to do task_list" );

                        }
                    }
                }
            }

/*
            DatasetLock dcmLock = dataset.getLockFor(DatasetLock.Reason.GlobusUpload);
            if (dcmLock == null) {
                logger.info("Dataset not locked for DCM upload");
            } else {
                datasetService.removeDatasetLocks(dataset, DatasetLock.Reason.GlobusUpload);
                dataset.removeLock(dcmLock);
            }
            logger.info(" ======= Remove Dataset Lock ");
*/

            List<DataFile> filesAdded = new ArrayList<>();

            if (dFileList != null && dFileList.size() > 0) {

                // Dataset dataset = version.getDataset();

                for (DataFile dataFile : dFileList) {

                    if (dataFile.getOwner() == null) {
                        dataFile.setOwner(dataset);

                        workingVersion.getFileMetadatas().add(dataFile.getFileMetadata());
                        dataFile.getFileMetadata().setDatasetVersion(workingVersion);
                        dataset.getFiles().add(dataFile);

                    }

                    filesAdded.add(dataFile);

                }

                logger.info("Async: ====  datasetId :" + datasetId + " ===== Done! Finished saving new files to the dataset.");
            }

            fileMetadatas.clear();
            for (DataFile addedFile : filesAdded) {
                fileMetadatas.add(addedFile.getFileMetadata());
            }
            filesAdded = null;

            if (workingVersion.isDraft()) {

                logger.info("Async: ====  datasetId :" + datasetId + " ==== inside draft version ");

                Timestamp updateTime = new Timestamp(new Date().getTime());

                workingVersion.setLastUpdateTime(updateTime);
                dataset.setModificationTime(updateTime);


                for (FileMetadata fileMetadata : fileMetadatas) {

                    if (fileMetadata.getDataFile().getCreateDate() == null) {
                        fileMetadata.getDataFile().setCreateDate(updateTime);
                        fileMetadata.getDataFile().setCreator((AuthenticatedUser) apiTokenUser);
                    }
                    fileMetadata.getDataFile().setModificationTime(updateTime);
                }


            } else {
                logger.info("Async: ====  datasetId :" + datasetId + " ==== inside released version ");

                for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) {
                    for (FileMetadata fileMetadata : fileMetadatas) {
                        if (fileMetadata.getDataFile().getStorageIdentifier() != null) {

                            if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
                                workingVersion.getFileMetadatas().set(i, fileMetadata);
                            }
                        }
                    }
                }


            }


            try {
                Command<Dataset> cmd;
                logger.info("Async: ====  datasetId :" + datasetId + " ======= UpdateDatasetVersionCommand START in globus function ");
                cmd = new UpdateDatasetVersionCommand(dataset,new DataverseRequest(apiTokenUser, (HttpServletRequest) null));
                ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);
                //new DataverseRequest(authenticatedUser, (HttpServletRequest) null)
                //dvRequestService.getDataverseRequest()
                commandEngine.submit(cmd);
            } catch (CommandException ex) {
                logger.log(Level.WARNING, "Async: ====  datasetId :" + datasetId + "======CommandException updating DatasetVersion from batch job: " + ex.getMessage());
                return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to do task_list" );
            }

            logger.info("Async: ====  datasetId :" + datasetId + " ======= GLOBUS ASYNC CALL COMPLETED SUCCESSFULLY ");

            return ok("Async: ====  datasetId :" + datasetId + ": Finished task_list");
        }  catch(Exception e) {
            String message = e.getMessage();

            logger.info("Async: ====  datasetId :" + datasetId + " ======= GLOBUS ASYNC CALL Exception ============== " + message);
            e.printStackTrace();
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to do task_list" );
            //return error(Response.Status.INTERNAL_SERVER_ERROR, "Uploaded files have passed checksum validation but something went wrong while attempting to move the files into Dataverse. Message was '" + message + "'.");
        }


    }

    private User checkAuth(Dataset dataset) {

        User apiTokenUser = null;

        try {
            apiTokenUser = findUserOrDie();
        } catch (WrappedResponse wr) {
            apiTokenUser = null;
            logger.log(Level.FINE, "Message from findUserOrDie(): {0}", wr.getMessage());
        }

        if (apiTokenUser != null) {
            // used in an API context
            if (!permissionService.requestOn(createDataverseRequest(apiTokenUser), dataset.getOwner()).has(Permission.EditDataset)) {
                apiTokenUser = null;
            }
        }

        return apiTokenUser;

    }
}
