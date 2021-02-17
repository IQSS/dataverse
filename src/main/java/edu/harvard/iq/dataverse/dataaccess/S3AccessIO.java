package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.util.FileUtil;
import opennlp.tools.util.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.constraints.NotNull;

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

    private static HashMap<String, AmazonS3> driverClientMap = new HashMap<String,AmazonS3>();
    private static HashMap<String, TransferManager> driverTMMap = new HashMap<String,TransferManager>();

    public S3AccessIO(T dvObject, DataAccessRequest req, String driverId) {
        super(dvObject, req, driverId);
        this.setIsLocalFile(false);
        
        try {
            bucketName=getBucketName(driverId);
            minPartSize = getMinPartSize(driverId);
            s3=getClient(driverId);
            tm=getTransferManager(driverId);
            endpoint = System.getProperty("dataverse.files." + driverId + ".custom-endpoint-url", "");
            proxy = System.getProperty("dataverse.files." + driverId + ".proxy-url", "");
            if(!StringUtil.isEmpty(proxy)&&StringUtil.isEmpty(endpoint)) {
                logger.severe(driverId + " config error: Must specify a custom-endpoint-url if proxy-url is specified");
            }
            //Not sure this is needed but moving it from the open method for now since it definitely doesn't need to run every time an object is opened.
            try {
                if (bucketName == null || !s3.doesBucketExistV2(bucketName)) {
                    throw new IOException("ERROR: S3AccessIO - You must create and configure a bucket before creating datasets.");
                }
            } catch (SdkClientException sce) {
                throw new IOException("ERROR: S3AccessIO - Failed to look up bucket "+bucketName+" (is AWS properly configured?): " + sce.getMessage());
            }
        } catch (Exception e) {
            throw new AmazonClientException(
                        "Cannot instantiate a S3 client; check your AWS credentials and region",
                        e);
        }
    }
    
    public S3AccessIO(String storageLocation, String driverId) {
        this(null, null, driverId);
        // TODO: validate the storage location supplied
        bucketName = storageLocation.substring(0,storageLocation.indexOf('/'));
        minPartSize = getMinPartSize(driverId);
        key = storageLocation.substring(storageLocation.indexOf('/')+1);
    }
    
    //Used for tests only
    public S3AccessIO(T dvObject, DataAccessRequest req, @NotNull AmazonS3 s3client, String driverId) {
        super(dvObject, req, driverId);
        bucketName = getBucketName(driverId);
        this.setIsLocalFile(false);
        this.s3 = s3client;
    }
    
    private AmazonS3 s3 = null;
    private TransferManager tm = null;
    private String bucketName = null;
    private String key = null;
    private long minPartSize;
    private String endpoint = null;
    private String proxy= null;

    @Override
    public void open(DataAccessOption... options) throws IOException {
        if (s3 == null) {
            throw new IOException("ERROR: s3 not initialised. ");
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

            if (req != null && req.getParameter("noVarHeader") != null) {
                this.setNoVarHeader(true);
            }

            if (storageIdentifier == null || "".equals(storageIdentifier)) {
                throw new FileNotFoundException("Data Access: No local storage identifier defined for this datafile.");
            }
            
            
            //Fix new DataFiles: DataFiles that have not yet been saved may use this method when they don't have their storageidentifier in the final <driverId>://<bucketname>:<id> form
            // So we fix it up here. ToDo: refactor so that storageidentifier is generated by the appropriate StorageIO class and is final from the start.
            String newStorageIdentifier = null;
            if (storageIdentifier.startsWith(this.driverId + "://")) {
                if(!storageIdentifier.substring((this.driverId + "://").length()).contains(":")) {
                    //Driver id but no bucket
                    if(bucketName!=null) {
                        newStorageIdentifier=this.driverId + "://" + bucketName + ":" + storageIdentifier.substring((this.driverId + "://").length()); 
                    } else {
                        throw new IOException("S3AccessIO: DataFile (storage identifier " + storageIdentifier + ") is not associated with a bucket.");
                    }
                } // else we're OK (assumes bucket name in storageidentifier matches the driver's bucketname)
            } else {
                if(!storageIdentifier.substring((this.driverId + "://").length()).contains(":")) {
                    //No driver id or bucket 
                    newStorageIdentifier= this.driverId + "://" + bucketName + ":" + storageIdentifier;
                } else {
                    //Just the bucketname
                    newStorageIdentifier= this.driverId + "://" + storageIdentifier;
                }
            }
            if(newStorageIdentifier != null) {
                //Fixup needed:
                storageIdentifier = newStorageIdentifier;
                dvObject.setStorageIdentifier(newStorageIdentifier);
            }

            
            if (isReadAccess) {
                key = getMainFileKey();
                ObjectMetadata objectMetadata = null; 
                try {
                    objectMetadata = s3.getObjectMetadata(bucketName, key);
                } catch (SdkClientException sce) {
                    throw new IOException("Cannot get S3 object " + key + " ("+sce.getMessage()+")");
                }
                this.setSize(objectMetadata.getContentLength());

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
                key = dataFile.getOwner().getAuthorityForFileStorage() + "/" + this.getDataFile().getOwner().getIdentifierForFileStorage();
                key += "/" + storageIdentifier.substring(storageIdentifier.lastIndexOf(":") + 1);
            }

            this.setMimeType(dataFile.getContentType());

            try {
                this.setFileName(dataFile.getFileMetadata().getLabel());
            } catch (Exception ex) {
                this.setFileName("unknown");
            }
        } else if (dvObject instanceof Dataset) {
            Dataset dataset = this.getDataset();
            key = dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage();
            dataset.setStorageIdentifier(this.driverId + "://" + key);
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("Data Access: Storage driver does not support dvObject type Dataverse yet");
        } else {
            // Direct access, e.g. for external upload - no associated DVobject yet, but we want to be able to get the size
            // With small files, it looks like we may call before S3 says it exists, so try some retries before failing
            if(key!=null) {
                 ObjectMetadata objectMetadata = null; 
                 int retries = 20;
                 while(retries > 0) {
                     try {
                         objectMetadata = s3.getObjectMetadata(bucketName, key);
                         if(retries != 20) {
                           logger.warning("Success for key: " + key + " after " + ((20-retries)*3) + " seconds");
                         }
                         retries = 0;
                     } catch (SdkClientException sce) {
                         if(retries > 1) {
                             retries--;
                             try {
                                 Thread.sleep(3000);
                             } catch (InterruptedException e) {
                                 e.printStackTrace();
                             }
                             logger.warning("Retrying after: " + sce.getMessage());
                         } else {
                             throw new IOException("Cannot get S3 object " + key + " ("+sce.getMessage()+")");
                         }
                     }
                 }
                 this.setSize(objectMetadata.getContentLength());
            }else {
            throw new IOException("Data Access: Invalid DvObject type");
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(super.getInputStream()==null) {
            try {
                setInputStream(s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent());
            } catch (SdkClientException sce) {
                throw new IOException("Cannot get S3 object " + key + " ("+sce.getMessage()+")");
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
        if(super.getChannel()==null) {
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
        long newFileSize = -1;

        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            File inputFile = fileSystemPath.toFile();
            if (dvObject instanceof DataFile) {
                tm.upload(new PutObjectRequest(bucketName, key, inputFile)).waitForCompletion();
                newFileSize = inputFile.length();
            } else {
                throw new IOException("DvObject type other than datafile is not yet supported");
            }

        } catch (SdkClientException | InterruptedException ioex ) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3Object "+key;
            }

            throw new IOException(failureMsg);
        }


        // if it has uploaded successfully, we can reset the size
        // of the object:
        setSize(newFileSize);
    }

    /**
     * Implements the StorageIO saveInputStream() method. 
     * This implementation is somewhat problematic, because S3 cannot save an object of 
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
     * @param filesize Long representing the filesize
     * @throws IOException if anything goes wrong.
    */
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
        String directoryString = FileUtil.getFilesTempDirectory();

        Random rand = new Random();
        Path tempPath = Paths.get(directoryString, Integer.toString(rand.nextInt(Integer.MAX_VALUE)));
        File tempFile = createTempFile(tempPath, inputStream);
        
        try {
            s3.putObject(bucketName, key, tempFile);
        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3 Storage.";
            }
            tempFile.delete();
            throw new IOException(failureMsg);
        }
        tempFile.delete();
        ObjectMetadata objectMetadata = s3.getObjectMetadata(bucketName, key);
        if (objectMetadata != null) {
            setSize(objectMetadata.getContentLength());
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
            s3.copyObject(new CopyObjectRequest(bucketName, destinationKey,  bucketName, key));
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
        try {
            File inputFile = fileSystemPath.toFile();
            s3.putObject(new PutObjectRequest(bucketName, destinationKey, inputFile));            
        } catch (AmazonClientException ase) {
            logger.warning("Caught an AmazonClientException in S3AccessIO.savePathAsAux():    " + ase.getMessage());
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
                    failureMsg = "S3AccessIO: SdkClientException occured while saving a local InputStream as S3Object";
                }
                throw new IOException(failureMsg);
            }
        }
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
     * @param auxItemTag String representing this Auxiliary type ("extension")
     * @throws IOException if anything goes wrong.
    */
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        if (!this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        String directoryString = FileUtil.getFilesTempDirectory();

        Random rand = new Random();
        String pathNum = Integer.toString(rand.nextInt(Integer.MAX_VALUE));
        Path tempPath = Paths.get(directoryString, pathNum);
        File tempFile = createTempFile(tempPath, inputStream);
        
        String destinationKey = getDestinationKey(auxItemTag);
        
        try {
            s3.putObject(bucketName, destinationKey, tempFile);
        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();

            if (failureMsg == null) {
                failureMsg = "S3AccessIO: SdkClientException occured while saving a local InputStream as S3Object";
            }
            tempFile.delete();
            throw new IOException(failureMsg);
        }
        tempFile.delete();
    }
    
    //Helper method for supporting saving streams with unknown length to S3
    //We save those streams to a file and then upload the file
    private File createTempFile(Path path, InputStream inputStream) throws IOException {

        File targetFile = new File(path.toUri()); // File needs a name
        try (OutputStream outStream = new FileOutputStream(targetFile);) {

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }

        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return targetFile;
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
            throw new IOException ("S3 listAuxObjects: failed to get a listing for "+prefix);
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
    public String getStorageLocation() throws IOException {
        String locationKey = getMainFileKey(); 
        
        if (locationKey == null) {
            throw new IOException("Failed to obtain the S3 key for the file");
        }
        
        return this.driverId + "://" + bucketName + "/" + locationKey; 
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
        } else if((dvObject==null) && (key !=null)) {
            //direct access
            destinationKey = key;
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
            if (s3object != null) {
                return s3object.getObjectContent();
            } 
            return null; 
        } catch (AmazonClientException ase) {
            logger.fine("Caught an AmazonClientException in S3AccessIO.getAuxFileAsInputStream() (object not cached?):    " + ase.getMessage());
            return null;
        }
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
     *       Is this good or bad? Need to ask @landreev
     *
     * Extract the file key from a file stored on S3.
     * Follows template: "owner authority name"/"owner identifier"/"storage identifier without bucketname and protocol"
     * @return Main File Key
     * @throws IOException
     */
    String getMainFileKey() throws IOException {
        if (key == null) {
            DataFile df = this.getDataFile();
            // TODO: (?) - should we worry here about the datafile having null for the owner here? 
            key = getMainFileKey(df.getOwner(), df.getStorageIdentifier(), driverId);
        }
        return key;
    }
    
    static String getMainFileKey(Dataset owner, String storageIdentifier, String driverId) throws IOException {
             
        // or about the owner dataset having null for the authority and/or identifier?
        // we should probably check for that and throw an exception. (unless we are 
        // super positive that this condition would have been intercepted by now)
        String baseKey = owner.getAuthorityForFileStorage() + "/" + owner.getIdentifierForFileStorage();
        return getMainFileKey(baseKey, storageIdentifier, driverId);
    }
    
    private static String getMainFileKey(String baseKey, String storageIdentifier, String driverId) throws IOException {
        String key = null;
        if (storageIdentifier == null || "".equals(storageIdentifier)) {
            throw new FileNotFoundException("Data Access: No local storage identifier defined for this datafile.");
        }

        if (storageIdentifier.indexOf(driverId + "://")>=0) {
            //String driverId = storageIdentifier.substring(0, storageIdentifier.indexOf("://")+3);
            //As currently implemented (v4.20), the bucket is part of the identifier and we could extract it and compare it with getBucketName() as a check - 
            //Only one bucket per driver is supported (though things might work if the profile creds work with multiple buckets, then again it's not clear when logic is reading from the driver property or from the DataFile).
            //String bucketName = storageIdentifier.substring(driverId.length() + 3, storageIdentifier.lastIndexOf(":"));
            key = baseKey + "/" + storageIdentifier.substring(storageIdentifier.lastIndexOf(":") + 1);    
        } else {
            throw new IOException("S3AccessIO: DataFile (storage identifier " + storageIdentifier + ") does not appear to be an S3 object associated with driver: " + driverId);
        }
        return key;
    }

    public boolean downloadRedirectEnabled() {
        String optionValue = System.getProperty("dataverse.files." + this.driverId + ".download-redirect");
        if ("true".equalsIgnoreCase(optionValue)) {
            return true;
        }
        return false;
    }
    
    public String generateTemporaryS3Url() throws IOException {
        //Questions:
        // Q. Should this work for private and public?
        // A. Yes! Since the URL has a limited, short life span. -- L.A. 
        // Q. how long should the download url work?
        // A. 1 hour by default seems like an OK number. Making it configurable seems like a good idea too. -- L.A.
        if (s3 == null) {
            throw new IOException("ERROR: s3 not initialised. ");
        }
        if (dvObject instanceof DataFile) {
            key = getMainFileKey();
            java.util.Date expiration = new java.util.Date();
            long msec = expiration.getTime();
            msec += 60 * 1000 * getUrlExpirationMinutes();
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
            responseHeaders.setContentDisposition("attachment; filename*=UTF-8''" + URLEncoder.encode(this.getDataFile().getDisplayName(), "UTF-8")
                    .replaceAll("\\+", "%20"));
            // - without it, download will work, but Firefox will leave the special
            // characters in the file name encoded. For example, the file name 
            // will look like "1976%E2%80%932016.txt" instead of "1976–2016.txt", 
            // where the dash is the "long dash", represented by a 3-byte UTF8 
            // character "\xE2\x80\x93"
            
            responseHeaders.setContentType(this.getDataFile().getContentType());
            generatePresignedUrlRequest.setResponseHeaders(responseHeaders);

            URL s; 
            try {
                s = s3.generatePresignedUrl(generatePresignedUrlRequest);
            } catch (SdkClientException sce) {
                //throw new IOException("SdkClientException generating temporary S3 url for "+key+" ("+sce.getMessage()+")");
                s = null; 
            }

            if (s != null) {
                if(!StringUtil.isEmpty(proxy)) {
                    /*
                     * AWS actually uses two URLs for its endpoint - for example
                     *   https://s3.amazonaws.com is what's used to configure the custom-endpoint-url
                     *     in Dataverse, but presigned URLs are of the form
                     *   https://<bucketname>.s3.amazonaws.com
                     * 
                     * Since we only record the first form, we'll use a regexp to match endpoints
                     * that have an additional 'bucket prefix' in the servername.
                     * 
                     * Institutional S3 servers, e.g. based on MinIO, don't need to do this (i.e.
                     * it's just AWS network setup, not part of the S3 protocol itself), so
                     * supporting this may only be used in testing.
                     * 
                     * Further, since the signatures only validate for the correct URLs, the risk in
                     * a bad match appears to be limited to breaking things, but if the potential
                     * for substitutions gets more complex, it might be better to just add another
                     * config setting.
                     */
                    // endpoint-urls for AWS don't have to have the protocol, so while we expect
                    // them for some servers, we check whether the protocol is in the url and then
                    // normalizing to use the part without the protocol
                    String endpointServer = endpoint;
                    int protocolEnd = endpoint.indexOf("://");
                    if (protocolEnd >=0 ) {
                        endpointServer = endpoint.substring(protocolEnd + 3);
                    }
                    logger.fine("Endpoint: " + endpointServer);
                    // We're then replacing 
                    //    http or https followed by :// and an optional <bucketname>. before the normalized endpoint url
                    // with the proxy info (which is protocol + machine name and optional port)
                    logger.fine("Original Url: " + s.toString());
                    String finalUrl = s.toString().replaceFirst("http[s]*:\\/\\/([^\\/]+\\.)"+endpointServer, proxy);
                    logger.fine("ProxiedURL: " + finalUrl);
                    return finalUrl; 
                } else {
                    return s.toString();
                }
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
    
    @Deprecated
    public String generateTemporaryS3UploadUrl() throws IOException {
        
        key = getMainFileKey();
        Date expiration = new Date();
        long msec = expiration.getTime();
        msec += 60 * 1000 * getUrlExpirationMinutes();
        expiration.setTime(msec);

        return generateTemporaryS3UploadUrl(key, expiration);
    }
    
    private String generateTemporaryS3UploadUrl(String key, Date expiration) throws IOException {
        GeneratePresignedUrlRequest generatePresignedUrlRequest = 
                new GeneratePresignedUrlRequest(bucketName, key).withMethod(HttpMethod.PUT).withExpiration(expiration);
        //Require user to add this header to indicate a temporary file
        generatePresignedUrlRequest.putCustomRequestHeader(Headers.S3_TAGGING, "dv-state=temp");
        
        URL presignedUrl; 
        try {
            presignedUrl = s3.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (SdkClientException sce) {
            logger.warning("SdkClientException generating temporary S3 url for "+key+" ("+sce.getMessage()+")");
            presignedUrl = null; 
        }
        String urlString = null;
        if (presignedUrl != null) {
            if(!StringUtil.isEmpty(proxy)) {
                //See discussion in getTemporaryS3Url
                // endpoint-urls for AWS don't have to have the protocol, so while we expect
                // them for some servers, we check whether the protocol is in the url and then
                // normalizing to use the part without the protocol
                String endpointServer = endpoint;
                int protocolEnd = endpoint.indexOf("://");
                if (protocolEnd >=0 ) {
                    endpointServer = endpoint.substring(protocolEnd + 3);
                }
                logger.fine("Endpoint: " + endpointServer);
                // We're then replacing 
                //    http or https followed by :// and an optional <bucketname>. before the normalized endpoint url
                // with the proxy info (which is protocol + machine name and optional port)
                urlString = presignedUrl.toString().replaceFirst("http[s]*:\\/\\/([^\\/]+\\.)"+endpointServer, proxy);
                logger.fine("ProxiedURL: " + urlString);
            } else {
                urlString = presignedUrl.toString();
            }
        }

        return urlString;
    }
    
    public JsonObjectBuilder generateTemporaryS3UploadUrls(String globalId, String storageIdentifier, long fileSize) throws IOException {

        JsonObjectBuilder response = Json.createObjectBuilder();
        key = getMainFileKey();
        java.util.Date expiration = new java.util.Date();
        long msec = expiration.getTime();
        msec += 60 * 1000 * getUrlExpirationMinutes();
        expiration.setTime(msec);
        
        if (fileSize <= minPartSize) {
            response.add("url", generateTemporaryS3UploadUrl(key, expiration));
        } else {
            JsonObjectBuilder urls = Json.createObjectBuilder();
            InitiateMultipartUploadRequest initiationRequest = new InitiateMultipartUploadRequest(bucketName, key);
            initiationRequest.putCustomRequestHeader(Headers.S3_TAGGING, "dv-state=temp");
            InitiateMultipartUploadResult initiationResponse = s3.initiateMultipartUpload(initiationRequest);
            String uploadId = initiationResponse.getUploadId();
            for (int i = 1; i <= (fileSize / minPartSize) + (fileSize % minPartSize > 0 ? 1 : 0); i++) {
                GeneratePresignedUrlRequest uploadPartUrlRequest = new GeneratePresignedUrlRequest(bucketName, key)
                        .withMethod(HttpMethod.PUT).withExpiration(expiration);
                uploadPartUrlRequest.addRequestParameter("uploadId", uploadId);
                uploadPartUrlRequest.addRequestParameter("partNumber", Integer.toString(i));
                URL presignedUrl;
                try {
                    presignedUrl = s3.generatePresignedUrl(uploadPartUrlRequest);
                } catch (SdkClientException sce) {
                    logger.warning("SdkClientException generating temporary S3 url for " + key + " (" + sce.getMessage()
                            + ")");
                    presignedUrl = null;
                }
                String urlString = null;
                if (presignedUrl != null) {
                    if(!StringUtil.isEmpty(proxy)) {
                        urlString = presignedUrl.toString().replace(endpoint, proxy);
                    } else {
                        urlString = presignedUrl.toString();
                    }
                }
                urls.add(Integer.toString(i), urlString);
            }
            response.add("urls", urls);
            response.add("abort", "/api/datasets/mpupload?globalid=" + globalId + "&uploadid=" + uploadId
                    + "&storageidentifier=" + storageIdentifier);
            response.add("complete", "/api/datasets/mpupload?globalid=" + globalId + "&uploadid=" + uploadId
                    + "&storageidentifier=" + storageIdentifier);

        }
        response.add("partSize", minPartSize);

        return response;
    }
    
    int getUrlExpirationMinutes() {
        String optionValue = System.getProperty("dataverse.files." + this.driverId + ".url-expiration-minutes"); 
        if (optionValue != null) {
            Integer num; 
            try {
                num = Integer.parseInt(optionValue);
            } catch (NumberFormatException ex) {
                num = null; 
            }
            if (num != null) {
                return num;
            }
        }
        return 60; 
    }
    
    private static String getBucketName(String driverId) {
        return System.getProperty("dataverse.files." + driverId + ".bucket-name");
    }
    
    private static long getMinPartSize(String driverId) {
        // as a default, pick 1 GB minimum part size for AWS S3 
        // (minimum allowed is 5*1024**2 but it probably isn't worth the complexity starting at ~5MB. Also -  confirmed that they use base 2 definitions)
        long min = 5 * 1024 * 1024l; 

        String partLength = System.getProperty("dataverse.files." + driverId + ".min-part-size");
        try {
            if (partLength != null) {
                long val = Long.parseLong(partLength);
                if(val>=min) {
                    min=val;
                } else {
                    logger.warning(min + " is the minimum part size allowed for jvm option dataverse.files." + driverId + ".min-part-size" );
                }
            } else {
                min = 1024 * 1024 * 1024l;
            }
        } catch (NumberFormatException nfe) {
            logger.warning("Unable to parse dataverse.files." + driverId + ".min-part-size as long: " + partLength);
        }
        return min;
    }


    private static TransferManager getTransferManager(String driverId) {
        if(driverTMMap.containsKey(driverId)) {
            return driverTMMap.get(driverId);
        } else {
            // building a TransferManager instance to support multipart uploading for files over 4gb.
            TransferManager manager = TransferManagerBuilder.standard()
                    .withS3Client(getClient(driverId))
                    .build();
            driverTMMap.put(driverId,  manager);
            return manager;
        }
    }


    private static AmazonS3 getClient(String driverId) {
        if(driverClientMap.containsKey(driverId)) {
            return driverClientMap.get(driverId);
        } else {
            // get a standard client, using the standard way of configuration the credentials, etc.
            AmazonS3ClientBuilder s3CB = AmazonS3ClientBuilder.standard();

            ClientConfiguration cc = new ClientConfiguration();
            Integer poolSize = Integer.getInteger("dataverse.files." + driverId + ".connection-pool-size", 256);
            cc.setMaxConnections(poolSize);
            s3CB.setClientConfiguration(cc);
            
            /**
             * Pass in a URL pointing to your S3 compatible storage.
             * For possible values see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/client/builder/AwsClientBuilder.EndpointConfiguration.html
             */
            String s3CEUrl = System.getProperty("dataverse.files." + driverId + ".custom-endpoint-url", "");
            /**
             * Pass in a region to use for SigV4 signing of requests.
             * Defaults to "dataverse" as it is not relevant for custom S3 implementations.
             */
            String s3CERegion = System.getProperty("dataverse.files." + driverId + ".custom-endpoint-region", "dataverse");

            // if the admin has set a system property (see below) we use this endpoint URL instead of the standard ones.
            if (!s3CEUrl.isEmpty()) {
                s3CB.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3CEUrl, s3CERegion));
            }
            /**
             * Pass in a boolean value if path style access should be used within the S3 client.
             * Anything but case-insensitive "true" will lead to value of false, which is default value, too.
             */
            Boolean s3pathStyleAccess = Boolean.parseBoolean(System.getProperty("dataverse.files." + driverId + ".path-style-access", "false"));
            // some custom S3 implementations require "PathStyleAccess" as they us a path, not a subdomain. default = false
            s3CB.withPathStyleAccessEnabled(s3pathStyleAccess);

            /**
             * Pass in a boolean value if payload signing should be used within the S3 client.
             * Anything but case-insensitive "true" will lead to value of false, which is default value, too.
             */
            Boolean s3payloadSigning = Boolean.parseBoolean(System.getProperty("dataverse.files." + driverId + ".payload-signing","false"));
            /**
             * Pass in a boolean value if chunked encoding should not be used within the S3 client.
             * Anything but case-insensitive "false" will lead to value of true, which is default value, too.
             */
            Boolean s3chunkedEncoding = Boolean.parseBoolean(System.getProperty("dataverse.files." + driverId + ".chunked-encoding","true"));
            // Openstack SWIFT S3 implementations require "PayloadSigning" set to true. default = false
            s3CB.setPayloadSigningEnabled(s3payloadSigning);
            // Openstack SWIFT S3 implementations require "ChunkedEncoding" set to false. default = true
            // Boolean is inverted, otherwise setting dataverse.files.<id>.chunked-encoding=false would result in leaving Chunked Encoding enabled
            s3CB.setChunkedEncodingDisabled(!s3chunkedEncoding);

            /**
             * Pass in a string value if this storage driver should use a non-default AWS S3 profile.
             * The default is "default" which should work when only one profile exists.
             */
            String s3profile = System.getProperty("dataverse.files." + driverId + ".profile","default");

            s3CB.setCredentials(new ProfileCredentialsProvider(s3profile));
            // let's build the client :-)
            AmazonS3 client =  s3CB.build();
            driverClientMap.put(driverId,  client);
            return client;
        }
    }

    public void removeTempTag() throws IOException {
        if (!(dvObject instanceof DataFile)) {
            logger.warning("Attempt to remove tag from non-file DVObject id: " + dvObject.getId());
            throw new IOException("Attempt to remove temp tag from non-file S3 Object");
        }
        try {
            
            key = getMainFileKey();
            DeleteObjectTaggingRequest deleteObjectTaggingRequest = new DeleteObjectTaggingRequest(bucketName, key);
            //NOte - currently we only use one tag so delete is the fastest and cheapest way to get rid of that one tag 
            //Otherwise you have to get tags, remove the one you don't want and post new tags and get charged for the operations
            s3.deleteObjectTagging(deleteObjectTaggingRequest);
         } catch (SdkClientException sce) {
             if(sce.getMessage().contains("Status Code: 501")) {
                 // In this case, it's likely that tags are not implemented at all (e.g. by Minio) so no tag was set either and it's just something to be aware of
                 logger.warning("Temp tag not deleted: Object tags not supported by storage: " + driverId);
             } else {
               // In this case, the assumption is that adding tags has worked, so not removing it is a problem that should be looked into.
               logger.severe("Unable to remove temp tag from : " + bucketName + " : " + key);
             }
         } catch (IOException e) {
            logger.warning("Could not create key for S3 object." );
            e.printStackTrace();
        }
        
    }

    public static void abortMultipartUpload(String globalId, String storageIdentifier, String uploadId)
            throws IOException {
        String baseKey = null;
        int index = globalId.indexOf(":");
        if (index >= 0) {
            baseKey = globalId.substring(index + 1);
        } else {
            throw new IOException("Invalid Global ID (expected form with '<type>:' prefix)");
        }
        String[] info = DataAccess.getDriverIdAndStorageLocation(storageIdentifier);
        String driverId = info[0];
        AmazonS3 s3Client = getClient(driverId);
        String bucketName = getBucketName(driverId);
        String key = getMainFileKey(baseKey, storageIdentifier, driverId);
        AbortMultipartUploadRequest req = new AbortMultipartUploadRequest(bucketName, key, uploadId);
        s3Client.abortMultipartUpload(req);
    }

    public static void completeMultipartUpload(String globalId, String storageIdentifier, String uploadId,
            List<PartETag> etags) throws IOException {
        String baseKey = null;
        int index = globalId.indexOf(":");
        if (index >= 0) {
            baseKey = globalId.substring(index + 1);
        } else {
            throw new IOException("Invalid Global ID (expected form with '<type>:' prefix)");
        }

        String[] info = DataAccess.getDriverIdAndStorageLocation(storageIdentifier);
        String driverId = info[0];
        AmazonS3 s3Client = getClient(driverId);
        String bucketName = getBucketName(driverId);
        String key = getMainFileKey(baseKey, storageIdentifier, driverId);
        CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(bucketName, key, uploadId, etags);
        s3Client.completeMultipartUpload(req);
    }

}
