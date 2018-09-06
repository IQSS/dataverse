/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.batch.jobs.importer.filesystem.FileRecordWriter;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * This class is for importing files added to s3 outside of dataverse.
 * Specifically, it is intended to be used along dcm.
 * Most of this code has been ported from FileRecordWriter, pruning out
 * the incomplete sections for importing individual files instead of folder-packages
 * @author matthew
 */

@Named
@Stateless
public class S3PackageImporter extends AbstractApiBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(S3PackageImporter.class.getName());

    private AmazonS3 s3 = null;
    
    @EJB
    DataFileServiceBean dataFileServiceBean;

    @EJB
    EjbDataverseEngine commandEngine;
    
    public void copyFromS3(Dataset dataset, String s3ImportPath) throws IOException {
        try {
            s3 = AmazonS3ClientBuilder.standard().defaultClient();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot instantiate a S3 client using; check your AWS credentials and region",
                    e);
        }

        JsonObjectBuilder bld = jsonObjectBuilder();

        String fileMode = FileRecordWriter.FILE_MODE_PACKAGE_FILE;

        String dcmBucketName = System.getProperty("dataverse.files.dcm-s3-bucket-name");
        String dcmDatasetKey = s3ImportPath;
        String dvBucketName = System.getProperty("dataverse.files.s3-bucket-name");

        String dvDatasetKey = getS3DatasetKey(dataset);
        
        logger.log(Level.INFO, "S3 Import related attributes. dcmBucketName: {0} | dcmDatasetKey: {1} | dvBucketName: {2} | dvDatasetKey: {3} |", 
                new Object[]{dcmBucketName, dcmDatasetKey, dvBucketName, dvDatasetKey});
        
        if (dataset.getVersions().size() != 1) {
            String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - Dataset has more than one version.";
            logger.info(error);
            throw new IllegalStateException(error);
        }

        if (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) {
            String error = "Error creating FilesystemImportJob with dataset with ID: " + dataset.getId() + " - Dataset isn't in DRAFT mode.";
            logger.info(error);
            throw new IllegalStateException(error);
        }

        ListObjectsRequest req = new ListObjectsRequest().withBucketName(dcmBucketName).withPrefix(dcmDatasetKey);
        ObjectListing storedDcmDatsetFilesList;
        try {
            storedDcmDatsetFilesList = s3.listObjects(req);
        } catch (SdkClientException sce) {
            logger.info("Caught an SdkClientException in s3ImportUtil:    " + sce.getMessage());
            throw new IOException ("S3 listAuxObjects: failed to get a listing for "+dcmDatasetKey);
        }
        List<S3ObjectSummary> storedDcmDatasetFilesSummary = storedDcmDatsetFilesList.getObjectSummaries();
        try {
            while (storedDcmDatsetFilesList.isTruncated()) {
                logger.fine("S3 listAuxObjects: going to next page of list");
                storedDcmDatsetFilesList = s3.listNextBatchOfObjects(storedDcmDatsetFilesList);
                if (storedDcmDatsetFilesList != null) {
                    storedDcmDatasetFilesSummary.addAll(storedDcmDatsetFilesList.getObjectSummaries());
                }
            }
        } catch (AmazonClientException ase) {
            logger.info("Caught an AmazonServiceException in s3ImportUtil:    " + ase.getMessage());
            throw new IOException("S3AccessIO: Failed to get aux objects for listing.");
        }
        for (S3ObjectSummary item : storedDcmDatasetFilesSummary) {

            logger.log(Level.INFO, "S3 Import file copy for {0}", new Object[]{item});
            String dcmFileKey = item.getKey();

            String copyFileName = dcmFileKey.substring(dcmFileKey.lastIndexOf('/') + 1);

            logger.log(Level.INFO, "S3 file copy related attributes. dcmBucketName: {0} | dcmFileKey: {1} | dvBucketName: {2} | copyFilePath: {3} |", 
                new Object[]{dcmBucketName, dcmFileKey, dvBucketName, dvDatasetKey+"/"+copyFileName});

            s3.copyObject(new CopyObjectRequest(dcmBucketName, dcmFileKey, dvBucketName, dvDatasetKey+"/"+copyFileName));                
            
            try {
                s3.deleteObject(new DeleteObjectRequest(dcmBucketName, dcmFileKey));
            }  catch (AmazonClientException ase) {
                logger.warning("Caught an AmazonClientException deleting s3 object from dcm bucket: " + ase.getMessage());
                throw new IOException("Failed to delete object" + new Object[]{item});
            }
        }

    }
    
    public DataFile createPackageDataFile(Dataset dataset, String folderName, long totalSize) {
            DataFile packageFile = new DataFile(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
            packageFile.setChecksumType(DataFile.ChecksumType.SHA1);
            
            FileUtil.generateStorageIdentifier(packageFile);
            
            
            String dvBucketName = System.getProperty("dataverse.files.s3-bucket-name");
            String dvDatasetKey = getS3DatasetKey(dataset);
            S3Object s3object = null; 

            s3object = s3.getObject(new GetObjectRequest(dvBucketName, dvDatasetKey+"/files.sha"));
           
            InputStream in = s3object.getObjectContent();
            String checksumVal = FileUtil.CalculateChecksum(in, packageFile.getChecksumType());

            packageFile.setChecksumValue(checksumVal); 

            packageFile.setFilesize(totalSize);
            packageFile.setModificationTime(new Timestamp(new Date().getTime()));
            packageFile.setCreateDate(new Timestamp(new Date().getTime()));
            packageFile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
            packageFile.setOwner(dataset);
            dataset.getFiles().add(packageFile);

            packageFile.setIngestDone();

            // set metadata and add to latest version
            FileMetadata fmd = new FileMetadata();
            fmd.setLabel(folderName.substring(folderName.lastIndexOf('/') + 1));
            
            fmd.setDataFile(packageFile);
            packageFile.getFileMetadatas().add(fmd);
            if (dataset.getLatestVersion().getFileMetadatas() == null) dataset.getLatestVersion().setFileMetadatas(new ArrayList<>());

            dataset.getLatestVersion().getFileMetadatas().add(fmd);
            fmd.setDatasetVersion(dataset.getLatestVersion());
            
            GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(packageFile.getProtocol(), commandEngine.getContext());
            if (packageFile.getIdentifier() == null || packageFile.getIdentifier().isEmpty()) {
                String packageIdentifier = dataFileServiceBean.generateDataFileIdentifier(packageFile, idServiceBean);
                packageFile.setIdentifier(packageIdentifier);
            }
            
            String nonNullDefaultIfKeyNotFound = "";
            String protocol = commandEngine.getContext().settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
            String authority = commandEngine.getContext().settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);

            if (packageFile.getProtocol() == null) {
                packageFile.setProtocol(protocol);
            }
            if (packageFile.getAuthority() == null) {
                packageFile.setAuthority(authority);
            }

            if (!packageFile.isIdentifierRegistered()) {
                String doiRetString = "";
                idServiceBean = GlobalIdServiceBean.getBean(commandEngine.getContext());
                try {
                    doiRetString = idServiceBean.createIdentifier(packageFile);
                } catch (Throwable e) {
                    
                }

                // Check return value to make sure registration succeeded
                if (!idServiceBean.registerWhenPublished() && doiRetString.contains(packageFile.getIdentifier())) {
                    packageFile.setIdentifierRegistered(true);
                    packageFile.setGlobalIdCreateTime(new Date());
                }
            }

        return packageFile;
    }
    
    public String getS3DatasetKey(Dataset dataset) {
        return dataset.getAuthority() + "/" + dataset.getIdentifier();
    }
}