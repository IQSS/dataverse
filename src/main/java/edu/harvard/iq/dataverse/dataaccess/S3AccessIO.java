package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.partitions.PartitionsLoader;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.thirdparty.apache.http.client.methods.HttpRequestBase;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;


/**
 *
 * @author Matthew A Dunlap
 * @param <T> what it stores
 */
/* 
    Experimental Amazon AWS S3 driver
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
        awsCredentials = new ProfileCredentialsProvider().getCredentials();
        s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.US_EAST_1).build();

        if (dvObject instanceof DataFile) {
            s3FolderPath = this.getDataFile().getOwner().getAuthority() + "/" + this.getDataFile().getOwner().getIdentifier();
            s3FileName = s3FolderPath + "/" + this.getDataFile().getDisplayName();
        } else if (dvObject instanceof Dataset) {
            Dataset dataset = (Dataset)dvObject;
            s3FolderPath = dataset.getAuthority() + "/" + dataset.getIdentifier();
        }
        
    }

    private AWSCredentials awsCredentials = null;
    private AmazonS3 s3 = null;
    private String bucketName = "testiqss-1239759fgsef34w4"; //name is global, no uppercase
    private String s3FolderPath;
    private String s3FileName;
  
    @Override
    public void open(DataAccessOption... options) throws IOException {
        DataAccessRequest req = this.getRequest();
        S3Object s3object=null;
        
        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }
        
        //FIXME: Finish? 
        if (dvObject instanceof DataFile) {
            DataFile dataFile = this.getDataFile();
            
            if (req != null && req.getParameter("noVarHeader") != null) {
                this.setNoVarHeader(true);
            }
            
            if (dataFile.getStorageIdentifier() == null || "".equals(dataFile.getStorageIdentifier())) {
                throw new FileNotFoundException("Data Access: No local storage identifier defined for this datafile.");
            }
            if (isReadAccess) {
                s3object = s3.getObject(new GetObjectRequest(bucketName, s3FileName));
                InputStream in = s3object.getObjectContent();

                if (in == null) {
                    throw new IOException("Cannot get Object" + s3FileName);
                }

                this.setInputStream(in);
                
                setChannel(Channels.newChannel(in));
                this.setSize(in.available());
                
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
                
                
                //I'm not sure what I actually need here. 
                //s3 does not use file objects like swift
                //maybe use putobjectrequest as an intermediate
                //...
                //maybe I just need an amazons3 object
            }

            this.setMimeType(dataFile.getContentType());
            this.setFileName(dataFile.getFileMetadata().getLabel());
        } else if (dvObject instanceof Dataset) {
            
            Dataset dataset = this.getDataset();
            dataset.setStorageIdentifier("s3://" + s3FolderPath);
            
            
        } else if (dvObject instanceof Dataverse) {

        } else {
            throw new IOException("Data Access: Invalid DvObject type");
        }
        
    }

    // StorageIO method for copying a local Path (for ex., a temp file), into this DataAccess location:
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;
        String storageIdentifier = dvObject.getStorageIdentifier();

        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            File inputFile = fileSystemPath.toFile();
            if (dvObject instanceof DataFile) {
                if(!s3.doesBucketExist(bucketName)) { 
                    s3.createBucket(bucketName);
                } 

                s3.putObject(new PutObjectRequest(bucketName, s3FileName, inputFile));
                    
                dvObject.setStorageIdentifier("s3://" + storageIdentifier);
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
    public void saveInputStream(InputStream inputStream) throws IOException {
        String key = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath;
        }
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

        String key = null;
        if (dvObject instanceof DataFile) {
            key = s3FileName;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath;
        }
        if (key != null) {
            try {
                DeleteObjectRequest deleteObjRequest = new DeleteObjectRequest(bucketName, key);
                s3.deleteObject(deleteObjRequest);
            } catch (AmazonClientException ase) {
                System.out.println("Caught an AmazonServiceException in S3AccessIO.delete():    " + ase.getMessage());
            }
        } else {
            throw new IOException("Failed to delete the object because the key was null");
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
        String key = null;
        if (s3 == null) {
            open();
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath + "/" + auxItemTag;
        }
        try {
            return s3.doesObjectExist(bucketName, key);
        } catch (AmazonClientException ase) {
                System.out.println("Caught an AmazonServiceException in S3AccessIO.isAuxObjectCached:    " + ase.getMessage());
        }
        return false;
    }
    
    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        String key = null;
        if (s3 == null) {
            open();
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath + "/" + auxItemTag;
        }
        try {
            return s3.getObjectMetadata(bucketName, key).getContentLength();
        } catch (AmazonClientException ase) {
                System.out.println("Caught an AmazonServiceException in S3AccessIO.getAuxObjectSize:    " + ase.getMessage());
        }
        return -1L;
    }
    
    @Override 
    public Path getAuxObjectAsPath(String auxItemTag) throws IOException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, its Aux objects have no local filesystem Paths associated with it.");
    }

    //FIXME: Empty
    @Override
    public void backupAsAux(String auxItemTag) throws IOException {
    }

    @Override
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        String key = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        if (dvObject instanceof DataFile) {
            key = s3FileName + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath + "/" + auxItemTag;
        }
        try {
            File inputFile = fileSystemPath.toFile();
            s3.putObject(new PutObjectRequest(bucketName, key, inputFile));
        } catch (AmazonClientException ase) {
            System.out.println("Caught an AmazonServiceException in S3AccessIO.savePathAsAux():    " + ase.getMessage());
        }
    }
    
    // this method copies a local InputStream into this DataAccess Auxiliary location:
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        String key = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath + "/" + auxItemTag;
        }
        byte[] bytes = IOUtils.toByteArray(inputStream);
        long length = bytes.length;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(length);
        try {
            s3.putObject(bucketName, key, inputStream, metadata);
        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();
            
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while saving a local InputStream as S3Object";
            }
            throw new IOException(failureMsg);
        }  
    }
    
    //TODO: How do we test this?
    @Override
    public List<String>listAuxObjects() throws IOException {
        String prefix = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (dvObject instanceof DataFile) {
            prefix = s3FileName + ".";
        } else if (dvObject instanceof Dataset) {
            prefix = s3FolderPath + "/";
        }
                
        List<String> ret = new ArrayList<>();
        ObjectListing storedAuxFilesList = null;
        List<S3ObjectSummary> storedAuxFilesSummary = null;
        logger.info("S3 prefix: " + prefix);

        ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
        storedAuxFilesList = s3.listObjects(req);
        storedAuxFilesSummary = storedAuxFilesList.getObjectSummaries();
        try {
            while (storedAuxFilesList.isTruncated()) {
                logger.info("S3 listAuxObjects: going to second page of list");
                storedAuxFilesList = s3.listNextBatchOfObjects(storedAuxFilesList);
                storedAuxFilesSummary.addAll(storedAuxFilesList.getObjectSummaries());
            }
        } catch (AmazonClientException ase) {
            System.out.println("Caught an AmazonServiceException in S3AccessIO.listAuxObjects():    " + ase.getMessage());
            logger.warning("Caught an AmazonServiceException:    " + ase.getMessage());
        }
        
        for (S3ObjectSummary item : storedAuxFilesSummary) {
            String key = item.getKey();
            String fileName = key.substring(key.lastIndexOf("/"));
            logger.info("S3 cached aux object fileName: " + fileName);
            ret.add(fileName);
        }
        return ret;
    }
    
    @Override
    public void deleteAuxObject(String auxItemTag) {
        String key = null;
        if (dvObject instanceof DataFile) {
            key = s3FileName + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath + "/" + auxItemTag;
        }
        try {
            DeleteObjectRequest dor = new DeleteObjectRequest(bucketName, key);
            s3.deleteObject(dor);
        } catch (AmazonClientException ase) {
            logger.warning("S3AccessIO: Unable to delete object    " + ase.getMessage());
        }
        
    }
    
    //TODO: is this efficient? i.e. number of calls to S3
    @Override
    public void deleteAllAuxObjects() throws IOException {
        String prefix = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (dvObject instanceof DataFile) {
            prefix = s3FileName + ".";
        } else if (dvObject instanceof Dataset) {
            prefix = s3FolderPath + "/";
        }
                
        List<String> ret = new ArrayList<>();
        ObjectListing storedAuxFilesList = null;
        List<S3ObjectSummary> storedAuxFilesSummary = null;
        logger.info("S3 prefix: " + prefix);
        try {
            ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
            storedAuxFilesList = s3.listObjects(req);
            storedAuxFilesSummary = storedAuxFilesList.getObjectSummaries();
            while (storedAuxFilesList.isTruncated()) {
                logger.info("S3 listAuxObjects: going to second page of list");
                storedAuxFilesList = s3.listNextBatchOfObjects(storedAuxFilesList);
                storedAuxFilesSummary.addAll(storedAuxFilesList.getObjectSummaries());
            }
        } catch (AmazonClientException ase) {
            System.out.println("Caught an AmazonServiceException:    " + ase.getMessage());
        }
        
        for (S3ObjectSummary item : storedAuxFilesSummary) {
            String key = item.getKey();
            logger.info("Trying to delete auxiliary file " + key);
            try {
                DeleteObjectRequest dor = new DeleteObjectRequest(bucketName, key);
                s3.deleteObject(dor);
            } catch (AmazonClientException ase) {
                    System.out.println("S3AccessIO: Unable to delete auxilary object    " + ase.getMessage());
            }
        }
    }
    
    //FIXME: Empty
    @Override
    public String getStorageLocation() {
        s3.getBucketLocation(s3FileName);
        return null;
    }

    @Override
    public Path getFileSystemPath() throws UnsupportedDataAccessOperationException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, it has no local filesystem path associated with it.");
    }
    
    @Override
    public boolean exists() {
        String key = null;
        if (dvObject instanceof DataFile) {
            key = s3FileName;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath;
        }
        try {
            return s3.doesObjectExist(bucketName, key);
        } catch (AmazonClientException ase) {
                System.out.println("Caught an AmazonServiceException in S3AccessIO.exists():    " + ase.getMessage());
        }
        return false;
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
        String key = null;
        if (s3 == null) {
            open();
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName + "." + auxItemTag;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath + "/" + auxItemTag;
        }
        try {
            S3Object s3object = s3.getObject(new GetObjectRequest(bucketName, key));
            return s3object.getObjectContent();
        } catch (AmazonClientException ase) {
                System.out.println("Caught an AmazonServiceException in S3AccessIO.getAuxFileAsInputStream():    " + ase.getMessage());
        }
        throw new IOException("S3AccessIO: Failed to get aux file as input stream");
    }

    
    // Auxilary helper methods, S3-specific:
     
    //FIXME: It looks like the best way to do credentials is to change a jvm variable on launch
    //          and use the standard getCredentials. Test this.
    private AWSCredentials getAWSCredentials() {
        if(awsCredentials == null)
        {
            try {
                //We can leave this as is and set an env variable
                awsCredentials = new ProfileCredentialsProvider().getCredentials();
            } catch (Exception e) {
                throw new AmazonClientException(
                        "Cannot load the credentials from the credential profiles file. " +
                        "Please make sure that your credentials file is at the correct " +
                        "location (~/.aws/credentials), and is in valid format.",
                        e);
            }
        }
        
        return awsCredentials;
    }
    
    //FIXME: This method is used across the IOs, why repeat?

}
