package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.common.base.Preconditions;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.FileUtil;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @param <T> what it stores
 * @author Matthew A Dunlap
 * @author Sarah Ferry
 * @author Rohit Bhattacharjee
 * @author Brian Silverstein
 */
/* 
    Amazon AWS S3 driver
 */
public class S3AccessIO<T extends DvObject> extends StorageIO<T> {

    private static final Logger logger = Logger.getLogger(S3AccessIO.class.getName());

    public final static String S3_STORAGE_IDENTIFIER_PREFIX = "s3://";
    private final static long S3_MULTIPART_UPLOAD_THRESHOLD = 100 * 1024 * 1024;

    private static final String MD5_METADATA_KEY = "MD5";

    private AmazonS3 s3 = null;

    private String bucketName;
    private String key;


    public S3AccessIO(String storageLocation, AmazonS3 s3client) {
        super();
        this.setIsLocalFile(false);

        // TODO: validate the storage location supplied
        bucketName = storageLocation.substring(0, storageLocation.indexOf('/'));
        key = storageLocation.substring(storageLocation.indexOf('/') + 1);
        this.s3 = s3client;
    }

    public S3AccessIO(T dvObject, @NotNull AmazonS3 s3client, String defaultBucketName) {
        super(dvObject);
        Preconditions.checkState(dvObject.getStorageIdentifier().startsWith(S3_STORAGE_IDENTIFIER_PREFIX),
                "StorageIO: No local storage identifier defined for this dvobject.");
        
        this.setIsLocalFile(false);
        bucketName = defaultBucketName;
        this.s3 = s3client;
    }

    public static String createStorageId(DvObject dvObject, String defaultBucketName) {
        Preconditions.checkState(dvObject instanceof Dataset || dvObject instanceof DataFile,
                "StorageIO: Unsupported DvObject type: " + dvObject.getClass().getName());

        if (dvObject instanceof DataFile) {
            return S3_STORAGE_IDENTIFIER_PREFIX + defaultBucketName + ":" + FileUtil.generateStorageIdentifier();
        } else {
            Dataset dataset = (Dataset) dvObject;
            return S3_STORAGE_IDENTIFIER_PREFIX + dataset.getAuthority() + "/" + dataset.getIdentifier();
        }
    }

    
    @Override
    public void open(DataAccessOption... options) throws IOException {

        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }

        if (dvObject instanceof DataFile) {

            DataFile dataFile = (DataFile)dvObject;

            key = getMainFileKey();

            if (isReadAccess) {

                if (dataFile.getContentType() != null
                        && dataFile.getContentType().equals("text/tab-separated-values")
                        && dataFile.isTabularData()
                        && dataFile.getDataTable() != null
                        && (!this.noVarHeader())) {

                    List<DataVariable> datavariables = dataFile.getDataTable().getDataVariables();
                    String varHeaderLine = generateVariableHeader(datavariables);
                    this.setVarHeader(varHeaderLine);
                }

            }
        } else if (dvObject instanceof Dataset) {
            key = dvObject.getStorageIdentifier().substring(S3_STORAGE_IDENTIFIER_PREFIX.length());
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("Data Access: Storage driver does not support dvObject type Dataverse yet");
        } else {
            throw new IOException("Data Access: Usupported DvObject type");
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (super.getInputStream() == null) {
            try {
                setInputStream(s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent());
            } catch (SdkClientException sce) {
                throw new IOException("Cannot get S3 object " + key + " (" + sce.getMessage() + ")");
            }
        }

        if (super.getInputStream() == null) {
            throw new IOException("Cannot get InputStream for S3 Object" + key);
        }

        setChannel(Channels.newChannel(super.getInputStream()));

        return super.getInputStream();
    }

    @Override
    public Channel getChannel() throws IOException {
        if (super.getChannel() == null) {
            getInputStream();
        }
        return channel;
    }

    @Override
    public ReadableByteChannel getReadChannel() throws IOException {
        //Make sure StorageIO.channel variable exists
        getChannel();
        return super.getReadChannel();
    }

    // StorageIO method for copying a local Path (for ex., a temp file), into this DataAccess location:
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (!(dvObject instanceof DataFile)) {
            throw new IOException("DvObject type other than datafile is not yet supported");
        }


        DataFile dataFile = (DataFile)dvObject;
        boolean shouldCompareMd5 = ChecksumType.MD5 == dataFile.getChecksumType() && dataFile.getDataTable() == null;

        if (shouldCompareMd5) {
            putFileToS3(fileSystemPath.toFile(), key, dataFile.getChecksumValue());
        } else {
            putFileToS3(fileSystemPath.toFile(), key, null);
        }
    }

    @Override
    public void delete() throws IOException {
        if (!isDirectAccess()) {
            throw new IOException("Direct Access IO must be used to permanently delete stored file objects");
        }
        if (key == null) {
            throw new IOException("Delete called with null key");
        }
        // Verify that it exists, before we attempt to delete it?
        // (probably unnecessary - attempting to delete it will fail if it doesn't exist - ?)
        try {
            DeleteObjectRequest deleteObjRequest = new DeleteObjectRequest(bucketName, key);
            s3.deleteObject(deleteObjRequest);
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonClientException in S3AccessIO.delete(): " + ase.getMessage());
            throw new IOException("Failed to delete storage location " + getStorageLocation());
        }

        // Delete all the cached aux files as well:
        deleteAllAuxObjects();

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
            logger.warning("Caught an AmazonClientException in S3AccessIO.isAuxObjectCached:    " + ase.getMessage());
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
            logger.warning("Caught an AmazonClientException in S3AccessIO.getAuxObjectSize:    " + ase.getMessage());
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
            logger.warning("Caught an AmazonClientException in S3AccessIO.backupAsAux:    " + ase.getMessage());
            throw new IOException("S3AccessIO: Unable to backup original auxiliary object");
        }
    }


    @Override
    public void revertBackupAsAux(String auxItemTag) throws IOException {
        String destinationKey = getDestinationKey(auxItemTag);
        try {
            s3.copyObject(new CopyObjectRequest(bucketName, destinationKey, bucketName, key));
            deleteAuxObject(auxItemTag);
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonServiceException in S3AccessIO.backupAsAux:    " + ase.getMessage());
            throw new IOException("S3AccessIO: Unable to revert backup auxiliary object");
        }
    }

    @Override
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        String destinationKey = getDestinationKey(auxItemTag);
        File inputFile = fileSystemPath.toFile();
        String checksum = FileUtil.calculateChecksum(inputFile.toPath(), ChecksumType.MD5);
        putFileToS3(inputFile, destinationKey, checksum);
    }

    /**
     * Implements the StorageIO saveInputStreamAsAux() method.
     * This implementation is problematic, because S3 cannot save an object of
     * an unknown length. This effectively nullifies any benefits of streaming;
     * as we cannot start saving until we have read the entire stream.
     * One way of solving this would be to buffer the entire stream as byte[],
     * in memory, then save it... Which of course would be limited by the amount
     * of memory available, and thus would not work for streams larger than that.
     * So we have eventually decided to save save the stream to a temp file, then
     * save to S3. This is slower, but guaranteed to work on any size stream.
     * An alternative we may want to consider is to not implement this method
     * in the S3 driver, and make it throw the UnsupportedDataAccessOperationException,
     * similarly to how we handle attempts to open OutputStreams, in this and the
     * Swift driver.
     *
     * @param inputStream InputStream we want to save
     * @param auxItemTag  String representing this Auxiliary type ("extension")
     * @throws IOException if anything goes wrong.
     */
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        String destinationKey = getDestinationKey(auxItemTag);
        File tempFile = copyInputStreamToTempFile(inputStream);
        
        try {
            putFileToS3(tempFile, destinationKey, null);
        } finally {
            tempFile.delete();
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
        ObjectListing storedAuxFilesList = null;
        try {
            storedAuxFilesList = s3.listObjects(req);
        } catch (SdkClientException sce) {
            throw new IOException("S3 listAuxObjects: failed to get a listing for " + prefix);
        }
        if (storedAuxFilesList == null) {
            return ret;
        }
        List<S3ObjectSummary> storedAuxFilesSummary = storedAuxFilesList.getObjectSummaries();
        try {
            while (storedAuxFilesList.isTruncated()) {
                logger.fine("S3 listAuxObjects: going to next page of list");
                storedAuxFilesList = s3.listNextBatchOfObjects(storedAuxFilesList);
                if (storedAuxFilesList != null) {
                    storedAuxFilesSummary.addAll(storedAuxFilesList.getObjectSummaries());
                }
            }
        } catch (AmazonClientException ase) {
            //logger.warning("Caught an AmazonServiceException in S3AccessIO.listAuxObjects():    " + ase.getMessage());
            throw new IOException("S3AccessIO: Failed to get aux objects for listing.");
        }

        for (S3ObjectSummary item : storedAuxFilesSummary) {
            String destinationKey = item.getKey();
            String fileName = destinationKey.substring(destinationKey.lastIndexOf(".") + 1);
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
        if (!isDirectAccess() && !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        String prefix = getDestinationKey("");

        List<S3ObjectSummary> storedAuxFilesSummary = null;
        try {
            ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
            ObjectListing storedAuxFilesList = s3.listObjects(req);
            if (storedAuxFilesList == null) {
                // nothing to delete
                return;
            }
            storedAuxFilesSummary = storedAuxFilesList.getObjectSummaries();
            while (storedAuxFilesList.isTruncated()) {
                storedAuxFilesList = s3.listNextBatchOfObjects(storedAuxFilesList);
                if (storedAuxFilesList != null) {
                    storedAuxFilesSummary.addAll(storedAuxFilesList.getObjectSummaries());
                }
            }
        } catch (AmazonClientException ase) {
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
        } catch (SdkClientException e) {
            throw new IOException("S3AccessIO: Failed to delete one or more auxiliary objects.");
        }
    }

    //TODO: Do we need this? - Answer: yes! 
    @Override
    public String getStorageLocation() {
        Preconditions.checkState(dvObject instanceof DataFile, "getStorageLocation() is supported only for datafiles");
        String locationKey = getMainFileKey();

        return S3_STORAGE_IDENTIFIER_PREFIX + bucketName + "/" + locationKey;
    }

    @Override
    public Path getFileSystemPath() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, it has no local filesystem path associated with it.");
    }

    @Override
    public boolean exists() throws IOException {
        String destinationKey = null;
        if (dvObject instanceof DataFile) {
            destinationKey = getMainFileKey();
        } else {
            logger.warning("Trying to check if a path exists is only supported for a data file.");
        }
        try {
            return s3.doesObjectExist(bucketName, destinationKey);
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonClientException in S3AccessIO.exists():    " + ase.getMessage());
            return false;
        }
    }

    @Override
    public long getSize() throws IOException {
        ObjectMetadata objectMetadata = null;
        try {
            objectMetadata = s3.getObjectMetadata(bucketName, key);
        } catch (SdkClientException sce) {
            throw new IOException("Cannot get S3 object " + key + " (" + sce.getMessage() + ")");
        }
        return objectMetadata.getContentLength();
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
            if (s3object != null) {
                return s3object.getObjectContent();
            }
            return null;
        } catch (AmazonClientException ase) {
            logger.fine("Caught an AmazonClientException in S3AccessIO.getAuxFileAsInputStream() (object not cached?):    " + ase.getMessage());
            return null;
        }
    }

    @Override
    public boolean isMD5CheckSupported() {
        return true;
    }

    @Override
    public String getMD5() throws IOException {
        ObjectMetadata objectMetadata;
        try {
            objectMetadata = s3.getObjectMetadata(bucketName, key);
        } catch (SdkClientException sce) {
            throw new IOException("Cannot get S3 object " + key + " (" + sce.getMessage() + ")", sce);
        }
        
        return objectMetadata.getUserMetadata().get(MD5_METADATA_KEY);
    }

    @Override
    public String getAuxObjectMD5(String auxItemTag) throws IOException {
        ObjectMetadata objectMetadata;
        try {
            objectMetadata = s3.getObjectMetadata(bucketName, getDestinationKey(auxItemTag));
        } catch (SdkClientException sce) {
            throw new IOException("Cannot get S3 object " + key + " (" + sce.getMessage() + ")", sce);
        }
        
        return objectMetadata.getUserMetadata().get(MD5_METADATA_KEY);
    }

    String getDestinationKey(String auxItemTag) throws IOException {
        if (isDirectAccess() || dvObject instanceof DataFile) {
            return getMainFileKey() + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            if (key == null) {
                open();
            }
            return key + "/" + auxItemTag;
        } else {
            throw new IOException("S3AccessIO: This operation is only supported for Datasets and DataFiles.");
        }
    }

    /**
     * TODO: this function is not side effect free (sets instance variables key and bucketName).
     * Is this good or bad? Need to ask @landreev
     * <p>
     * Extract the file key from a file stored on S3.
     * Follows template: "owner authority name"/"owner identifier"/"storage identifier without bucketname and protocol"
     *
     * @return Main File Key
     * @throws IOException
     */
    String getMainFileKey() {
        if (key == null) {
            // TODO: (?) - should we worry here about the datafile having null for the owner here? 
            // or about the owner dataset having null for the authority and/or identifier?
            // we should probably check for that and throw an exception. (unless we are 
            // super positive that this condition would have been intercepted by now)
            String baseKey = dvObject.getOwner().getStorageIdentifier().substring(S3_STORAGE_IDENTIFIER_PREFIX.length());
            String storageIdentifier = dvObject.getStorageIdentifier();

            bucketName = storageIdentifier.substring(S3_STORAGE_IDENTIFIER_PREFIX.length(), storageIdentifier.lastIndexOf(":"));
            key = baseKey + "/" + storageIdentifier.substring(storageIdentifier.lastIndexOf(":") + 1);
        }

        return key;
    }

    public String generateTemporaryS3Url() throws IOException {
        //Questions:
        // Q. Should this work for private and public?
        // A. Yes! Since the URL has a limited, short life span. -- L.A. 
        // Q. how long should the download url work?
        // A. 1 hour by default seems like an OK number. Making it configurable seems like a good idea too. -- L.A.

        if (dvObject instanceof DataFile) {
            key = getMainFileKey();
            java.util.Date expiration = new java.util.Date();
            long msec = expiration.getTime();
            msec += 1000 * getUrlExpirationMinutes();
            expiration.setTime(msec);

            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, key);
            generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
            generatePresignedUrlRequest.setExpiration(expiration);
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            //responseHeaders.setContentDisposition("attachment; filename="+this.getDataFile().getDisplayName());
            // Encode the file name explicitly specifying the encoding as UTF-8:
            // (otherwise S3 may not like non-ASCII characters!)
            // Most browsers are happy with just "filename="+URLEncoder.encode(this.getDataFile().getDisplayName(), "UTF-8") 
            // in the header. But Firefox appears to require that "UTF8" is 
            // specified explicitly, as below:
            responseHeaders.setContentDisposition("attachment; filename*=UTF-8''" + URLEncoder.encode(this.getFileName(), "UTF-8"));
            // - without it, download will work, but Firefox will leave the special
            // characters in the file name encoded. For example, the file name 
            // will look like "1976%E2%80%932016.txt" instead of "1976–2016.txt", 
            // where the dash is the "long dash", represented by a 3-byte UTF8 
            // character "\xE2\x80\x93"

            responseHeaders.setContentType(this.getMimeType());
            generatePresignedUrlRequest.setResponseHeaders(responseHeaders);

            URL s;
            try {
                s = s3.generatePresignedUrl(generatePresignedUrlRequest);
            } catch (SdkClientException sce) {
                //throw new IOException("SdkClientException generating temporary S3 url for "+key+" ("+sce.getMessage()+")");
                s = null;
            }

            if (s != null) {
                return s.toString();
            }

            //throw new IOException("Failed to generate temporary S3 url for "+key);
            return null;
        } else if (dvObject instanceof Dataset) {
            throw new IOException("Data Access: GenerateTemporaryS3Url: Invalid DvObject type : Dataset");
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("Data Access: GenerateTemporaryS3Url: Invalid DvObject type : Dataverse");
        } else {
            throw new IOException("Data Access: GenerateTemporaryS3Url: Unknown DvObject type");
        }
    }

    int getUrlExpirationMinutes() {
        String optionValue = System.getProperty("dataverse.files.s3-url-expiration-minutes");
        return NumberUtils.toInt(optionValue, 60);
    }
    
    
    private void putFileToS3(File file, String s3Key, String providedMD5Checksum) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        if (StringUtils.isNotBlank(providedMD5Checksum)) {
            try {
                metadata.setContentMD5(Base64.getEncoder().encodeToString(Hex.decodeHex(providedMD5Checksum.toCharArray())));
            } catch (DecoderException e) {
                throw new IOException(e);
            }
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put(MD5_METADATA_KEY, providedMD5Checksum);
            metadata.setUserMetadata(userMetadata);
        }
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, s3Key, file);
        putRequest.setMetadata(metadata);
        putObjectToS3(putRequest);
    }

    private void putObjectToS3(PutObjectRequest putRequest) throws IOException {
        TransferManager s3Transfer = TransferManagerBuilder.standard()
                .withMultipartUploadThreshold(S3_MULTIPART_UPLOAD_THRESHOLD)
                .withS3Client(s3).build();
        
        try {
            Upload s3Upload = s3Transfer.upload(putRequest);
            s3Upload.waitForCompletion();
            if (!verifyUploadedFileIfPossible(putRequest))  {
                s3.deleteObject(putRequest.getBucketName(), putRequest.getKey());
                throw new IOException("File storage error - checsums before and after put are not identical");
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted s3 upload", e);
        } catch (AmazonClientException e) {
            String failureMsg = e.getMessage();
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3Object " + putRequest.getKey();
            }

            throw new IOException(failureMsg, e);
        }
        
        s3Transfer.shutdownNow(false);
    }
    

    private boolean verifyUploadedFileIfPossible(PutObjectRequest putRequest) {
        String checksumSent = putRequest.getMetadata().getContentMD5();
        if (checksumSent == null) {
            return true;
        }
        String checksumLocal = FileUtil.calculateChecksum(putRequest.getFile().toPath(), ChecksumType.MD5);
        try {
            checksumLocal = Base64.getEncoder().encodeToString(Hex.decodeHex(checksumLocal.toCharArray()));
        } catch (DecoderException e) {
            return false;
        }
        return checksumLocal.equals(checksumSent);
    }

    private File copyInputStreamToTempFile(InputStream inputStream) throws IOException {
        String directoryString = FileUtil.getFilesTempDirectory();

        Random rand = new Random();
        Path tempPath = Paths.get(directoryString, Integer.toString(rand.nextInt(Integer.MAX_VALUE)));
        File tempFile = createTempFile(tempPath, inputStream);
        
        return tempFile;
    }
    
    //Helper method for supporting saving streams with unknown length to S3
    //We save those streams to a file and then upload the file
    private File createTempFile(Path path, InputStream inputStream) throws IOException {

        File targetFile = new File(path.toUri()); //File needs a name
        OutputStream outStream = new FileOutputStream(targetFile);

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outStream);
        return targetFile;
    }
}
