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
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
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

//    private S3Object initializeS3Object(boolean writeAccess) throws IOException {
//        return initializeS3Object(writeAccess, null);
//    }
//    
//    private S3Object  initializeS3Object(boolean writeAccess, String auxItemTag) throws IOException {
////        String swiftEndPoint = null;
////        String swiftContainerName = null;
////        
//        S3Object fileObject;
//        List<String> auxFiles = null; 
//        String s3FolderPathSeparator = "/";
//        String storageIdentifier = dvObject.getStorageIdentifier();
//        
//         if (dvObject instanceof DataFile) {
//            Dataset owner = this.getDataFile().getOwner();
//
//            if (storageIdentifier.startsWith("s3://")) {
//               
//                if ( StringUtil.isEmpty(s3FileName)) {
//                    // all of these things need to be specified, for this to be a valid Swift location
//                    // identifier.
//                    throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
//                }
//
//                if (auxItemTag != null) {
//                    s3FileName = s3FileName.concat("."+auxItemTag);
//                }
//            } else if (this.isReadAccess) {
//                // An attempt to call Swift driver,  in a Read mode on a non-swift stored datafile
//                // object!
//                throw new IOException("IO driver mismatch: SwiftAccessIO called on a non-swift stored object.");
//            } else if (this.isWriteAccess) {   
//                
//                s3FolderPath = owner.getAuthority() + s3FolderPathSeparator + owner.getIdentifier() + s3FolderPathSeparator;
////                 String key =  datafile.getOwner().getAuthority() + "/" + datafile.getOwner().getIdentifier() + "/" + datafile.getDisplayName();
//            
//                s3FileName = s3FolderPath+this.getDataFile().getDisplayName();
//                //Storage Identifier is now updated after the object is uploaded on s3.
//                dvObject.setStorageIdentifier("s3://" + s3FolderPath + s3FolderPathSeparator + s3FileName);
//            } else {
//                throw new IOException("SwiftAccessIO: unknown access mode.");
//            }
//        } else if (dvObject instanceof Dataset) {
//            Dataset dataset = this.getDataset();
//
//            if (storageIdentifier.startsWith("s3://")) {
//                // This is a call on an already existing swift object. 
//
//                //We will not have a file name, just an aux tag
//                if (auxItemTag != null) {
//                    s3FileName = auxItemTag;
//                } else {
//                    throw new IOException("Dataset related auxillary files require an auxItemTag");
//                }       
//
//                if (StringUtil.isEmpty(s3FileName) ) {
//                    // all of these things need to be specified, for this to be a valid Swift location
//                    // identifier.
//                    throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
//                }
//
//            } else if (this.isReadAccess) {
//                // An attempt to call Swift driver,  in a Read mode on a non-swift stored datafile
//                // object!
//                throw new IOException("IO driver mismatch: SwiftAccessIO called on a non-swift stored object.");
//            } else if (this.isWriteAccess) {
//                s3FolderPath= dataset.getAuthority() + s3FolderPathSeparator + dataset.getIdentifier() + s3FolderPathSeparator;
//                s3FileName = auxItemTag;
//                dvObject.setStorageIdentifier("s3://"+ s3FolderPath);
//            } else {
//                throw new IOException("SwiftAccessIO: unknown access mode.");
//            }
//        } else {
//            //for future scope, if dataverse is decided to be stored in swift storage containersopen    
//            throw new FileNotFoundException("Error initializing swift object");  
//        }
//
//        fileObject = this.swiftContainer.getObject(swiftFileName);
//
//        // If this is the main, primary datafile object (i.e., not an auxiliary 
//        // object for a primary file), we also set the file download url here: 
//        if (auxItemTag == null && dvObject instanceof DataFile) {
//            setRemoteUrl(getSwiftFileURI(fileObject));
//            logger.fine(getRemoteUrl() + " success; write mode: "+writeAccess);
//        } else {
//            logger.fine("sucessfully opened AUX object "+auxItemTag+" , write mode: "+writeAccess);
//        }
//
//        if (!writeAccess && !fileObject.exists()) {
//            throw new FileNotFoundException("SwiftAccessIO: DvObject " + swiftFileName + " does not exist (Dataverse dvObject id: " + dvObject.getId());
//        }
//
//        auxFiles = null; 
//
//        return fileObject;
// 
//    }
//    
    //FIXME: Finish
    @Override
    public void open(DataAccessOption... options) throws IOException {
        DataAccessRequest req = this.getRequest();
        
        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }
        
        //FIXME: Fill. Most of the content in here I (Matthew) don't really understand. copypaste
        if (dvObject instanceof DataFile) {
            DataFile dataFile = this.getDataFile();
            
            if (req != null && req.getParameter("noVarHeader") != null) {
                this.setNoVarHeader(true);
            }
            
            if (dataFile.getStorageIdentifier() == null || "".equals(dataFile.getStorageIdentifier())) {
                throw new IOException("Data Access: No local storage identifier defined for this datafile.");
            }
            if (isReadAccess) {
                
            } else if (isWriteAccess) {
                //create a real init function
                awsCredentials = new ProfileCredentialsProvider().getCredentials();
                s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.US_EAST_1).build();
                
                //saveInputStream();
                
                
                //I'm not sure what I actually need here. 
                //s3 does not use file objects like swift
                //maybe use putobjectrequest as an intermediate
                //...
                //maybe I just need an amazons3 object
            }

            this.setMimeType(dataFile.getContentType());

            try {
                this.setFileName(dataFile.getFileMetadata().getLabel());
            } catch (Exception ex) {
                this.setFileName("unknown");
            }

            
        } else if (dvObject instanceof Dataset) {
            
        } else if (dvObject instanceof Dataverse) {

        } else {
            throw new IOException("Data Access: Invalid DvObject type");
        }
        
    }

    // StorageIO method for copying a local Path (for ex., a temp file), into this DataAccess location:

    //FIXME: Incomplete
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;

        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            File inputFile = fileSystemPath.toFile();
            if (dvObject instanceof DataFile) {
                DataFile datafile = (DataFile)dvObject;
            
                if(!s3.doesBucketExist(bucketName)) { 
                    s3.createBucket(bucketName);
                } 
//                if(s3.doesObjectExist(bucketName, key)){
//                    System.out.println("Rohit Bhattacharjee File Exists!!");
//                } else{
                    s3.putObject(new PutObjectRequest(bucketName, s3FileName, inputFile));
//                }
                    
                newFileSize = inputFile.length();
            } else {
                throw new IOException("DvObject type other than datafile is not yet supported");
            }
            
        } catch (SdkClientException ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while uploading a local file into a Swift StoredObject";
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
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            long length = bytes.length;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(length);
            s3.putObject(bucketName, key, inputStream, metadata);
        } catch (IOException ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "S3AccessIO: Unknown exception occured while uploading a local file into S3 Storage.";
            }

            throw new IOException(failureMsg);
        }
        //TODO:
        // setSize(swiftFileObject.getContentLength());

    }
    
    //FIXME: s3 or s3client..? + need key defined for this method
    @Override
    public void delete() throws IOException {
<<<<<<< HEAD
        
=======
        String key = null;
        if (dvObject instanceof DataFile) {
            key = s3FileName;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath;
        }
>>>>>>> 047cdecbf144aaf6fb2b2fab098171a24b8b1c97
        if (key != null) {
            try {
            DeleteObjectRequest deleteObjRequest = new DeleteObjectRequest(bucketName, key);
            s3.deleteObject(deleteObjRequest);
            //s3client.deleteObject(bucketName, key);
            } catch (AmazonClientException ase) {
                System.out.println("Caught an AmazonServiceException:    " + ase.getMessage());
            }
        } else {
<<<<<<< HEAD
            //initialize key
            
=======
            throw new IOException("Failed to delete the object because the key was null");
>>>>>>> 047cdecbf144aaf6fb2b2fab098171a24b8b1c97
        }
        
    }

    //FIXME: Empty
    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {
        return null;
    }

    //FIXME: Empty
    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        return false;
    }
    
    //FIXME: Empty
    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        return 0;
    }
    
    @Override 
    public Path getAuxObjectAsPath(String auxItemTag) throws IOException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, its Aux objects have no local filesystem Paths associated with it.");
    }

    //FIXME: Empty
    @Override
    public void backupAsAux(String auxItemTag) throws IOException {
    }

    //FIXME: Empty
    @Override
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        String key = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath;
        }
        try {
            File inputFile = fileSystemPath.toFile();
            s3.putObject(new PutObjectRequest(bucketName, key + "." + auxItemTag, inputFile));
        } catch (AmazonClientException ase) {
            System.out.println("Caught an AmazonServiceException:    " + ase.getMessage());
        }
    }
    
    //FIXME: Empty
    // this method copies a local InputStream into this DataAccess Auxiliary location:
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        String key = null;
        if (s3 == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        if (dvObject instanceof DataFile) {
            key = s3FileName;
        } else if (dvObject instanceof Dataset) {
            key = s3FolderPath;
        }
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            long length = bytes.length;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(length);
            s3.putObject(bucketName, key + "." + auxItemTag, inputStream, metadata);
        } catch(IOException ioex) {
            String failureMsg = ioex.getMessage();
            
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while saving a local InputStream as a Swift StoredObject";
            }
            throw new IOException(failureMsg);
        }  
    }
    
    //FIXME: Empty
    @Override
    public List<String>listAuxObjects() throws IOException {
        return null;
    }
    
    //FIXME: Empty
    @Override
    public void deleteAuxObject(String auxItemTag) throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public void deleteAllAuxObjects() throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public String getStorageLocation() {
        return null;
    }

    @Override
    public Path getFileSystemPath() throws IOException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: this is a remote DataAccess IO object, it has no local filesystem path associated with it.");
    }
    
    //FIXME: Empty
    @Override
    public boolean exists() throws IOException {
        boolean exists = s3.doesObjectExist(bucketName, key);
        return exists;
    }

    @Override
    public WritableByteChannel getWriteChannel() throws IOException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: there are no write Channels associated with S3 objects.");
    }
    
    @Override  
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedDataAccessOperationException("S3AccessIO: there are no output Streams associated with S3 objects.");
    }
    
    @Override
    public InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException {
        return null;
    }

    
    // Auxilary helper methods, S3-specific:
    // // FIXME: Refer to swift implementation while implementing S3
    
    AmazonS3 authenticateWithS3(String swiftEndPoint) throws IOException {
        AWSCredentials credentials = getAWSCredentials();

        AmazonS3 s3 = null;

        try {
            s3 = AmazonS3ClientBuilder.standard().withCredentials(
                    new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_EAST_1).build();

        } catch (Exception ex) {
            throw new IOException("S3AccessIO: failed to authenticate S3" + ex.getMessage());
        }

        return s3;
    }
     
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
    private boolean isWriteAccessRequested (DataAccessOption... options) throws IOException {
        
        for (DataAccessOption option: options) {
            // In the future we may need to be able to open read-write 
            // Channels; no support, or use case for that as of now. 
            
            if (option == DataAccessOption.READ_ACCESS) {
                return false;
            }

            if (option == DataAccessOption.WRITE_ACCESS) {
                return true;
            }
        }
        
        // By default, we open the file in read mode:
        return false; 
    }
}
