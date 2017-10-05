package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Matthew A Dunlap
 * @author Sarah Ferry
 * @author Rohit Bhattacharjee
 * @author Brian Silverstein
 * @param <T> what it stores
 */
/* 
    Amazon AWS S3 driver
 */
public class S3AccessIO<T extends DvObject> extends StorageIO<T> {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.S3AccessIO");

    public S3AccessIO() {
        this(null);
    }

    public S3AccessIO(T dvObject) {
        this(dvObject, null);
    }

    public S3AccessIO(T dvObject, DataAccessRequest req) {
        super(dvObject, req);
        this.setIsLocalFile(false);
        try {
            awsCredentials = new ProfileCredentialsProvider().getCredentials();
            s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.US_EAST_1).build();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
    }

    private AWSCredentials awsCredentials = null;
    private AmazonS3 s3 = null;
    private String bucketName = System.getProperty("dataverse.files.s3-bucket-name");
    private String key;

    @Override
    public void open(DataAccessOption... options) throws IOException {
        if (s3 == null) {
            throw new IOException("ERROR: s3 not initialised. ");
        }

        if (bucketName == null || !s3.doesBucketExist(bucketName)) {
            throw new IOException("ERROR: S3AccessIO - You must create and configure a bucket before creating datasets.");
        }

        DataAccessRequest req = this.getRequest();

        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }

        if (dvObject instanceof DataFile) {
            String storageIdentifier = dvObject.getStorageIdentifier();

            DataFile dataFile = this.getDataFile();
            key = dataFile.getOwner().getAuthority() + "/" + this.getDataFile().getOwner().getIdentifier();

            if (req != null && req.getParameter("noVarHeader") != null) {
                this.setNoVarHeader(true);
            }

            if (storageIdentifier == null || "".equals(storageIdentifier)) {
                throw new FileNotFoundException("Data Access: No local storage identifier defined for this datafile.");
            }

            if (isReadAccess) {
                if (storageIdentifier.startsWith("s3://")) {
                    bucketName = storageIdentifier.substring(storageIdentifier.indexOf(":") + 3, storageIdentifier.lastIndexOf(":"));
                    key += "/" + storageIdentifier.substring(storageIdentifier.lastIndexOf(":") + 1);
                } else {
                    throw new IOException("IO driver mismatch: S3AccessIO called on a non-s3 stored object.");
                }
                S3Object s3object = s3.getObject(new GetObjectRequest(bucketName, key));
                InputStream in = s3object.getObjectContent();

                if (in == null) {
                    throw new IOException("Cannot get Object" + key);
                }

                this.setInputStream(in);

                setChannel(Channels.newChannel(in));
                this.setSize(s3object.getObjectMetadata().getContentLength());

                if (dataFile.getContentType() != null
                        && dataFile.getContentType().equals("text/tab-separated-values")
                        && dataFile.isTabularData()
                        && dataFile.getDataTable() != null
                        && (!this.noVarHeader())) {

                    List<DataVariable> datavariables = dataFile.getDataTable().getDataVariables();
                    String varHeaderLine = generateVariableHeader(datavariables);
                    this.setVarHeader(varHeaderLine);
                }

            } else if (isWriteAccess) {
                if (storageIdentifier.startsWith("s3://")) {
                    key += "/" + storageIdentifier.substring(storageIdentifier.lastIndexOf(":") + 1);
                } else {
                    key += "/" + storageIdentifier;
                    dvObject.setStorageIdentifier("s3://" + bucketName + ":" + storageIdentifier);
                }

            }

            this.setMimeType(dataFile.getContentType());

            try {
                this.setFileName(dataFile.getFileMetadata().getLabel());
            } catch (Exception ex) {
                this.setFileName("unknown");
            }
        } else if (dvObject instanceof Dataset) {
            Dataset dataset = this.getDataset();
            key = dataset.getAuthority() + "/" + dataset.getIdentifier();
            dataset.setStorageIdentifier("s3://" + key);
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("Data Access: Invalid DvObject type : Dataverse");
        } else {
            throw new IOException("Data Access: Invalid DvObject type");
        }
    }

    // StorageIO method for copying a local Path (for ex., a temp file), into this DataAccess location:
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;

        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            File inputFile = fileSystemPath.toFile();
            if (dvObject instanceof DataFile) {
                s3.putObject(new PutObjectRequest(bucketName, key, inputFile));
                newFileSize = inputFile.length();
            } else {
                throw new IOException("DvObject type other than datafile is not yet supported");
            }

        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3Object";
            }

            throw new IOException(failureMsg);
        }

        // if it has uploaded successfully, we can reset the size
        // of the object:
        setSize(newFileSize);
    }

    @Override
    public void saveInputStream(InputStream inputStream, Long filesize) throws IOException {
        if (filesize == null || filesize < 0) {
            saveInputStream(inputStream);
        } else {
            if (!this.canWrite()) {
                open(DataAccessOption.WRITE_ACCESS);
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(filesize);
            try {
                s3.putObject(bucketName, key, inputStream, metadata);
            } catch (SdkClientException ioex) {
                String failureMsg = ioex.getMessage();
                if (failureMsg == null) {
                    failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3 Storage.";
                }

                throw new IOException(failureMsg);
            }
            setSize(filesize);  
        }
    }
    
    @Override
    public void saveInputStream(InputStream inputStream) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        //TODO? Copying over the object to a byte array is farily inefficient.
        // We need the length of the data to upload inputStreams (see our putObject calls).
        // There may be ways to work around this, see https://github.com/aws/aws-sdk-java/issues/474 to start.
        // This is out of scope of creating the S3 driver and referenced in issue #4064!
        byte[] bytes = IOUtils.toByteArray(inputStream);
        long length = bytes.length;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(length);
        try {
            s3.putObject(bucketName, key, inputStream, metadata);
        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3 Storage.";
            }

            throw new IOException(failureMsg);
        }
        setSize(s3.getObjectMetadata(bucketName, key).getContentLength());
    }

    @Override
    public void delete() throws IOException {
        open();
        if (key == null) {
            throw new IOException("Failed to delete the object because the key was null");
        }
        try {
            DeleteObjectRequest deleteObjRequest = new DeleteObjectRequest(bucketName, key);
            s3.deleteObject(deleteObjRequest);
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.delete():    " + ase.getMessage());
            throw new IOException("Failed to delete object" + dvObject.getId());
        }
    }

    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {
        if (isWriteAccessRequested(options)) {
            throw new UnsupportedDataAccessOperationException("S3AccessIO: write mode openAuxChannel() not yet implemented in this storage driver.");
        }

        InputStream fin = getAuxFileAsInputStream(auxItemTag);

        if (fin == null) {
            throw new IOException("Failed to open auxilary file " + auxItemTag + " for S3 file");
        }

        return Channels.newChannel(fin);
    }

    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        open();
        logger.fine("Inside isAuxObjectCached");
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            return s3.doesObjectExist(bucketName, destinationKey);
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.isAuxObjectCached:    " + ase.getMessage());
            throw new IOException("S3AccessIO: Failed to cache auxilary object : " + auxItemTag);
        }
    }

    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        open();
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            return s3.getObjectMetadata(bucketName, destinationKey).getContentLength();
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.getAuxObjectSize:    " + ase.getMessage());
        }
        return -1;
    }

    @Override
    public Path getAuxObjectAsPath(String auxItemTag) throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, its Aux objects have no local filesystem Paths associated with it.");
    }

    @Override
    public void backupAsAux(String auxItemTag) throws IOException {
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            s3.copyObject(new CopyObjectRequest(bucketName, key, bucketName, destinationKey));
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.backupAsAux:    " + ase.getMessage());
            throw new IOException("S3AccessIO: Unable to backup original auxiliary object");
        }
    }

    @Override
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            File inputFile = fileSystemPath.toFile();
            s3.putObject(new PutObjectRequest(bucketName, destinationKey, inputFile));
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.savePathAsAux():    " + ase.getMessage());
            throw new IOException("S3AccessIO: Failed to save path as an auxiliary object.");
        }
    }

    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag, Long filesize) throws IOException {
        if (filesize == null || filesize < 0) {
            saveInputStreamAsAux(inputStream, auxItemTag);
        } else {
            if (!this.canWrite()) {
                open(DataAccessOption.WRITE_ACCESS);
            }
            String destinationKey = getDestinationKey(auxItemTag);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(filesize);
            try {
                s3.putObject(bucketName, destinationKey, inputStream, metadata);
            } catch (SdkClientException ioex) {
                String failureMsg = ioex.getMessage();

                if (failureMsg == null) {
                    failureMsg = "S3AccessIO: Unknown exception occured while saving a local InputStream as S3Object";
                }
                throw new IOException(failureMsg);
            }
        }
    }
    
    //todo: add new method with size?
    //or just check the data file content size?
    // this method copies a local InputStream into this DataAccess Auxiliary location:
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        String destinationKey = getDestinationKey(auxItemTag);
        byte[] bytes = IOUtils.toByteArray(inputStream);
        long length = bytes.length;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(length);
        try {
            s3.putObject(bucketName, destinationKey, inputStream, metadata);
        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();

            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while saving a local InputStream as S3Object";
            }
            throw new IOException(failureMsg);
        }
    }

    @Override
    public List<String> listAuxObjects() throws IOException {
        if (!this.canWrite()) {
            open();
        }
        String prefix = getDestinationKey("");

        List<String> ret = new ArrayList<>();
        ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
        ObjectListing storedAuxFilesList = s3.listObjects(req);
        List<S3ObjectSummary> storedAuxFilesSummary = storedAuxFilesList.getObjectSummaries();
        try {
            while (storedAuxFilesList.isTruncated()) {
                logger.fine("S3 listAuxObjects: going to second page of list");
                storedAuxFilesList = s3.listNextBatchOfObjects(storedAuxFilesList);
                storedAuxFilesSummary.addAll(storedAuxFilesList.getObjectSummaries());
            }
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.listAuxObjects():    " + ase.getMessage());
            throw new IOException("S3AccessIO: Failed to get aux objects for listing.");
        }

        for (S3ObjectSummary item : storedAuxFilesSummary) {
            String destinationKey = item.getKey();
            String fileName = destinationKey.substring(destinationKey.lastIndexOf("/"));
            logger.fine("S3 cached aux object fileName: " + fileName);
            ret.add(fileName);
        }
        return ret;
    }

    @Override
    public void deleteAuxObject(String auxItemTag) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            DeleteObjectRequest dor = new DeleteObjectRequest(bucketName, destinationKey);
            s3.deleteObject(dor);
        } catch (AmazonClientException ase) {
            logger.warning("S3AccessIO: Unable to delete object    " + ase.getMessage());
        }
    }

    @Override
    public void deleteAllAuxObjects() throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        String prefix = getDestinationKey("");

        List<S3ObjectSummary> storedAuxFilesSummary = null;
        try {
            ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
            ObjectListing storedAuxFilesList = s3.listObjects(req);
            storedAuxFilesSummary = storedAuxFilesList.getObjectSummaries();
            while (storedAuxFilesList.isTruncated()) {
                storedAuxFilesList = s3.listNextBatchOfObjects(storedAuxFilesList);
                storedAuxFilesSummary.addAll(storedAuxFilesList.getObjectSummaries());
            }
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException:    " + ase.getMessage());
            throw new IOException("S3AccessIO: Failed to get aux objects for listing to delete.");
        }

        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(bucketName);
        List<KeyVersion> keys = new ArrayList<>();

        for (S3ObjectSummary item : storedAuxFilesSummary) {
            String destinationKey = item.getKey();
            keys.add(new KeyVersion(destinationKey));
        }
        //Check if the list of auxiliary files for a data file is empty
        if (keys.isEmpty()) {
            logger.fine("S3AccessIO: No auxiliary objects to delete.");
            return;
        }
        multiObjectDeleteRequest.setKeys(keys);

        logger.fine("Trying to delete auxiliary files...");
            try {
                s3.deleteObjects(multiObjectDeleteRequest);
            } catch (MultiObjectDeleteException e) {
                logger.warning("S3AccessIO: Unable to delete auxilary objects" + e.getMessage());
                throw new IOException("S3AccessIO: Failed to delete one or more auxiliary objects.");
            }
    }

    //TODO: Do we need this?
    @Override
    public String getStorageLocation() {
        return null;
    }

    @Override
    public Path getFileSystemPath() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, it has no local filesystem path associated with it.");
    }

    @Override
    public boolean exists() {
        String destinationKey = null;
        if (dvObject instanceof DataFile) {
            destinationKey = key;
        } else {
            logger.warning("Trying to check if a path exists is only supported for a data file.");
        }
        try {
            return s3.doesObjectExist(bucketName, destinationKey);
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.exists():    " + ase.getMessage());
            return false;
        }
    }

    @Override
    public WritableByteChannel getWriteChannel() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: there are no write Channels associated with S3 objects.");
    }

    @Override
    public OutputStream getOutputStream() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: there are no output Streams associated with S3 objects.");
    }

    @Override
    public InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException {
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            S3Object s3object = s3.getObject(new GetObjectRequest(bucketName, destinationKey));
            return s3object.getObjectContent();
        } catch (AmazonClientException ase) {
            logger.fine("Caught an AmazonServiceException in S3AccessIO.getAuxFileAsInputStream() (object not cached?):    " + ase.getMessage());
            return null;
        }
    }

    private String getDestinationKey(String auxItemTag) throws IOException {
        if (dvObject instanceof DataFile) {
            if (key == null) {
                String baseKey = this.getDataFile().getOwner().getAuthority() + "/" + this.getDataFile().getOwner().getIdentifier();
                String storageIdentifier = dvObject.getStorageIdentifier();

                if (storageIdentifier == null || "".equals(storageIdentifier)) {
                    throw new FileNotFoundException("Data Access: No local storage identifier defined for this datafile.");
                }

                if (storageIdentifier.startsWith("s3://")) {
                    bucketName = storageIdentifier.substring(storageIdentifier.indexOf(":") + 3, storageIdentifier.lastIndexOf(":"));
                    key = baseKey + "/" + storageIdentifier.substring(storageIdentifier.lastIndexOf(":") + 1);
                } else {
                    throw new IOException("S3AccessIO: DataFile (storage identifier "+storageIdentifier+") does not appear to be an S3 object.");
                }
            }
            
            return key + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            return key + "/" + auxItemTag;
        } else {
            throw new IOException("S3AccessIO: This operation is only supported for Datasets and DataFiles.");
        }
    }
}
