package edu.harvard.iq.dataverse.engine.command.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Strings;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.batch.jobs.importer.filesystem.FileRecordWriter;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchRuntime;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@RequiredPermissions(Permission.EditDataset)
public class ImportFromS3Command extends AbstractCommand<JsonObject> {

    private static final Logger logger = Logger.getLogger(ImportFromS3Command.class.getName());

    final Dataset dataset;
    final String s3ImportPath;
    public static String S3_IDENTIFIER_PREFIX = "s3";
    
    private AmazonS3 s3 = null;
    
    public ImportFromS3Command(DataverseRequest aRequest, Dataset theDataset, String theS3ImportPath) {
        super(aRequest, theDataset);
        dataset = theDataset;
        s3ImportPath = theS3ImportPath;
        try {
            s3 = AmazonS3ClientBuilder.standard().defaultClient();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot instantiate a S3 client using; check your AWS credentials and region",
                    e);
        }
    }

    @Override
    public JsonObject execute(CommandContext ctxt) throws CommandException {
        JsonObjectBuilder bld = jsonObjectBuilder();
        /**
         * batch import as-individual-datafiles is disabled in this iteration;
         * only the import-as-a-package is allowed. -- L.A. Feb 2 2017
         */
        String fileMode = FileRecordWriter.FILE_MODE_PACKAGE_FILE;

        String dcmBucketName = "test-dcm"; //MAD: PUT IN CONFIGS
        String dcmDatasetKey = s3ImportPath;
        String dvBucketName = System.getProperty("dataverse.files.s3-bucket-name");
        String dvDatasetKey = dataset.getAuthority() + "/" + dataset.getIdentifier(); //+ "/" + itemTag;
//        File dvBucketKey = new File(dvBucketName
//                + File.separator + dataset.getAuthority() + File.separator + dataset.getIdentifier()); //MAD: this may need another folder for the package... or something else
        
        //MAD: IS THIS LOGGER A SECURITY ISSUE?
        logger.log(Level.INFO, "S3 Import related attributes. dcmBucketName: {0} | dcmDatasetKey: {1} | dvBucketName: {2} | dvDatasetKey: {3} |", 
                new Object[]{dcmBucketName, dcmDatasetKey, dvBucketName, dvDatasetKey});
        
        try {
            /** MAD: CLEAN
             * Current constraints: 1. only supports merge and replace mode 2.
             * valid dataset 3. valid dataset directory 4. valid user & user has
             * edit dataset permission 5. only one dataset version 6. dataset
             * version is draft
             */

            


            if (dataset.getVersions().size() != 1) {
                String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - Dataset has more than one version.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }

            if (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) {
                String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - Dataset isn't in DRAFT mode.";
                logger.info(error);
                throw new IllegalCommandException(error, this);
            }
            
//MAD: MAYBE REFERENCE S3AccessIO for this path instead? Kinda duplicated code

            //dataset.setStorageIdentifier(S3_IDENTIFIER_PREFIX + "://" + key);
            
            ListObjectsRequest req = new ListObjectsRequest().withBucketName(dcmBucketName).withPrefix(dcmDatasetKey);
            ObjectListing storedDcmDatsetFilesList;
            try {
                storedDcmDatsetFilesList = s3.listObjects(req);
                logger.log(Level.INFO, "S3 Import debug 1");
            } catch (SdkClientException sce) {
                throw new IOException ("S3 listAuxObjects: failed to get a listing for "+dcmDatasetKey);
            }
            List<S3ObjectSummary> storedDcmDatasetFilesSummary = storedDcmDatsetFilesList.getObjectSummaries();
            logger.log(Level.INFO, "S3 Import debug 2");
            try {
                while (storedDcmDatsetFilesList.isTruncated()) {
                    logger.fine("S3 listAuxObjects: going to next page of list");
                    storedDcmDatsetFilesList = s3.listNextBatchOfObjects(storedDcmDatsetFilesList);
                    if (storedDcmDatsetFilesList != null) {
                        storedDcmDatasetFilesSummary.addAll(storedDcmDatsetFilesList.getObjectSummaries());
                    }
                }
            } catch (AmazonClientException ase) {
                //logger.warning("Caught an AmazonServiceException in S3AccessIO.listAuxObjects():    " + ase.getMessage());
                throw new IOException("S3AccessIO: Failed to get aux objects for listing.");
            }
            logger.log(Level.INFO, "S3 Import debug 3. Size " + storedDcmDatasetFilesSummary.size() + " |  Contents " + storedDcmDatasetFilesSummary );
            
            for (S3ObjectSummary item : storedDcmDatasetFilesSummary) {
               
                logger.log(Level.INFO, "S3 Import file copy for {0}", new Object[]{item});
                //MAD: In here we do all the copy commands
                String dcmFileKey = item.getKey();
                
                String copyFileName = dcmFileKey.substring(dcmFileKey.lastIndexOf('/') + 1);
                
                logger.log(Level.INFO, "S3 file copy related attributes. dcmBucketName: {0} | dcmFileKey: {1} | dvBucketName: {2} | copyFilePath: {3} |", 
                    new Object[]{dcmBucketName, dcmFileKey, dvBucketName, dvDatasetKey+"/"+copyFileName});
                
                s3.copyObject(new CopyObjectRequest(dcmBucketName, dcmFileKey, dvBucketName, dvDatasetKey+"/"+copyFileName));
                
            }
            

            return null;
        } catch (Exception e) {
            logger.log(Level.INFO, "S3 Import error: " + e.getMessage());

            bld.add("message", "Import Exception - " + e.getMessage());
            return bld.build();
        }
    }

 
}
