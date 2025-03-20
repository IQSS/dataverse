package edu.harvard.iq.dataverse;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

/**
 * This class is for importing files added to s3 outside of dataverse.
 * Specifically, it is intended to be used along dcm.
 * Most of this code has been ported from FileRecordWriter, pruning out
 * the incomplete sections for importing individual files instead of folder-packages
 * @author matthew
 */

@Named
@Stateless
public class S3PackageImporter extends AbstractApiBean implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(S3PackageImporter.class.getName());

    private S3Client s3;
    
    @EJB
    DataFileServiceBean dataFileServiceBean;

    @EJB
    EjbDataverseEngine commandEngine;
    
    public void copyFromS3(Dataset dataset, String s3ImportPath) throws IOException {
        try {
            s3 = S3Client.create();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot instantiate a S3 client; check your AWS credentials and region",
                    e);
        }
        
        String dcmBucketName = System.getProperty("dataverse.files.dcm-s3-bucket-name");
        String dcmDatasetKey = s3ImportPath;
        String dvBucketName = System.getProperty("dataverse.files.s3.bucket-name");

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

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(dcmBucketName)
                .prefix(dcmDatasetKey)
                .build();

        ListObjectsV2Response listRes;
        try {
            listRes = s3.listObjectsV2(listReq);
        } catch (S3Exception se) {
            logger.info("Caught an S3Exception in s3ImportUtil: " + se.getMessage());
            throw new IOException("S3 listAuxObjects: failed to get a listing for " + dcmDatasetKey);
        }

        List<S3Object> storedDcmDatasetFilesSummary = new ArrayList<>(listRes.contents());

        while (listRes.isTruncated()) {
            logger.fine("S3 listAuxObjects: going to next page of list");
            listReq = listReq.toBuilder().continuationToken(listRes.nextContinuationToken()).build();
            listRes = s3.listObjectsV2(listReq);
            storedDcmDatasetFilesSummary.addAll(listRes.contents());
        }

        for (S3Object item : storedDcmDatasetFilesSummary) {
            logger.log(Level.INFO, "S3 Import file copy for {0}", item);
            String dcmFileKey = item.key();

            String copyFileName = dcmFileKey.substring(dcmFileKey.lastIndexOf('/') + 1);

            logger.log(Level.INFO, "S3 file copy related attributes. dcmBucketName: {0} | dcmFileKey: {1} | dvBucketName: {2} | copyFilePath: {3} |", 
                new Object[]{dcmBucketName, dcmFileKey, dvBucketName, dvDatasetKey + "/" + copyFileName});

            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(dcmBucketName)
                    .sourceKey(dcmFileKey)
                    .destinationBucket(dvBucketName)
                    .destinationKey(dvDatasetKey + "/" + copyFileName)
                    .build();
            s3.copyObject(copyReq);

            try {
                DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                        .bucket(dcmBucketName)
                        .key(dcmFileKey)
                        .build();
                s3.deleteObject(deleteReq);
            } catch (S3Exception se) {
                logger.warning("Caught an S3Exception deleting s3 object from dcm bucket: " + se.getMessage());
                throw new IOException("Failed to delete object " + item);
            }
        }
    }
    
    public DataFile createPackageDataFile(Dataset dataset, String folderName, long totalSize) throws IOException {
        DataFile packageFile = new DataFile(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
        packageFile.setChecksumType(DataFile.ChecksumType.SHA1);

        String rootPackageName = "package_" + folderName.replace("/", "");

        String dvBucketName = System.getProperty("dataverse.files.s3.bucket-name");
        String dvDatasetKey = getS3DatasetKey(dataset);

        logger.log(Level.INFO, "shaname {0}", rootPackageName + ".sha");

        HeadObjectRequest headReq = HeadObjectRequest.builder()
                .bucket(dvBucketName)
                .key(dvDatasetKey + "/" + rootPackageName + ".zip")
                .build();
        if (!s3.headObject(headReq).sdkHttpResponse().isSuccessful()) {
            throw new IOException("S3 Package data file could not be found after copy from dcm. Name: " + dvDatasetKey + "/" + rootPackageName + ".zip");
        }

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(dvBucketName)
                .key(dvDatasetKey + "/" + rootPackageName + ".sha")
                .build();
        ResponseInputStream<GetObjectResponse> s3FilesSha = s3.getObject(getReq);

        InputStreamReader str = new InputStreamReader(s3FilesSha);
        BufferedReader reader = new BufferedReader(str);
        String checksumVal = "";
        try {
            String line;
            while ((line = reader.readLine()) != null && checksumVal.isEmpty()) {
                logger.log(Level.FINE, "line {0}", line);
                String[] splitLine = line.split("  ");

                if (splitLine[1].contains(rootPackageName + ".zip")) { 
                    checksumVal = splitLine[0];
                    logger.log(Level.FINE, "checksumVal found {0}", checksumVal);
                }
            }
            if (checksumVal.isEmpty()) {
                logger.log(Level.SEVERE, "No checksum found for uploaded DCM S3 zip on dataset {0}", dataset.getIdentifier());
            }                
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error parsing DCM s3 checksum file on dataset {0} . Error: {1} ", new Object[]{dataset.getIdentifier(), ex});
        } finally {
            try {
                str.close();
                reader.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "errors closing s3 DCM object reader stream: {0}", ex);
            }
        }

        logger.log(Level.FINE, "Checksum value for the package in Dataset {0} is: {1}", 
           new Object[]{dataset.getIdentifier(), checksumVal});

        packageFile.setChecksumValue(checksumVal); 

        packageFile.setFilesize(totalSize);
        packageFile.setModificationTime(new Timestamp(new Date().getTime()));
        packageFile.setCreateDate(new Timestamp(new Date().getTime()));
        packageFile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        packageFile.setOwner(dataset);
        dataset.getFiles().add(packageFile);

        packageFile.setIngestDone();

        FileMetadata fmd = new FileMetadata();
        fmd.setLabel(rootPackageName + ".zip");

        fmd.setDataFile(packageFile);
        packageFile.getFileMetadatas().add(fmd);
        if (dataset.getLatestVersion().getFileMetadatas() == null) dataset.getLatestVersion().setFileMetadatas(new ArrayList<>());

        dataset.getLatestVersion().getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(dataset.getLatestVersion());

        FileUtil.generateS3PackageStorageIdentifier(packageFile);
        PidProvider pidProvider = commandEngine.getContext().dvObjects().getEffectivePidGenerator(dataset);
        if (packageFile.getIdentifier() == null || packageFile.getIdentifier().isEmpty()) {
            pidProvider.generatePid(packageFile);
        }

        if (!packageFile.isIdentifierRegistered()) {
            String doiRetString = "";
            try {
                doiRetString = pidProvider.createIdentifier(packageFile);
            } catch (Throwable e) {
                // Handle exception
            }

            if (!pidProvider.registerWhenPublished() && doiRetString.contains(packageFile.getIdentifier())) {
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