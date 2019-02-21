package edu.harvard.iq.dataverse.dataaccess;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.javaswift.joss.client.factory.AccountFactory;
import static org.javaswift.joss.client.factory.AuthenticationMethod.BASIC;
import static org.javaswift.joss.client.factory.AuthenticationMethod.KEYSTONE_V3;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

/**
 *
 * @author leonid andreev
 * @param <T> what it stores
 */
/* 
    Swift driver, implemented as part of the Dataverse - Mass Open Cloud
    collaboration. 
 */
public class SwiftAccessIO<T extends DvObject> extends StorageIO<T> {

    private String swiftFolderPath;

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO");

    public SwiftAccessIO() {
        this(null);
    }

    public SwiftAccessIO(T dvObject) {
        this(dvObject, null);
    }

    public SwiftAccessIO(T dvObject, DataAccessRequest req) {
        super(dvObject, req);

        this.setIsLocalFile(false);
    }

    private Properties swiftProperties = null;
    private Account account = null;
    private StoredObject swiftFileObject = null;
    private Container swiftContainer = null;
    //TODO: when swift containers can be private, change this -SF
    boolean publicSwiftContainer = true;

        
    //for hash
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    
    //TODO: should this be dynamically generated based on size of file?
    //Also, this is in seconds
    private static int TEMP_URL_EXPIRES = System.getProperty("dataverse.files.temp_url_expire") != null ? Integer.parseInt(System.getProperty("dataverse.files.temp_url_expire")) : 60;

    private static int LIST_PAGE_LIMIT = 100;  

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

        if (dvObject instanceof DataFile) {
            DataFile dataFile = this.getDataFile();

            if (req != null && req.getParameter("noVarHeader") != null) {
                this.setNoVarHeader(true);
            }

            if (dataFile.getStorageIdentifier() == null || "".equals(dataFile.getStorageIdentifier())) {
                throw new IOException("Data Access: No local storage identifier defined for this datafile.");
            }

            if (isReadAccess) {
                InputStream fin = openSwiftFileAsInputStream();

                if (fin == null) {
                    throw new IOException("Failed to open Swift file " + getStorageLocation());
                }

                this.setInputStream(fin);
                setChannel(Channels.newChannel(fin));

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
                swiftFileObject = initializeSwiftFileObject(true);
            }

            this.setMimeType(dataFile.getContentType());


            try {
                this.setFileName(dataFile.getFileMetadata().getLabel());
            } catch (Exception ex) {
                this.setFileName("unknown");
            }
        } else if (dvObject instanceof Dataset) {
            //we are uploading a dataset related auxilary file
            //such as a dataset thumbnail or a metadata export
            if (isReadAccess) {
                //TODO: fix this
                InputStream fin = openSwiftFileAsInputStream();

                if (fin == null) {
                    throw new IOException("Failed to open Swift file " + getStorageLocation());
                }

                this.setInputStream(fin);
            } else if (isWriteAccess) {
                swiftFileObject = initializeSwiftFileObject(true);
            }
        } else if (dvObject instanceof Dataverse) {
        } else {
            throw new IOException("Data Access: Invalid DvObject type");
        }
    }


    // StorageIO method for copying a local Path (for ex., a temp file), into this DataAccess location:
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;

        if (swiftFileObject == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            File inputFile = fileSystemPath.toFile();

            swiftFileObject.uploadObject(inputFile);

            newFileSize = inputFile.length();

        } catch (Exception ioex) {
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
    public void saveInputStream(InputStream inputStream, Long filesize) throws IOException {
        saveInputStream(inputStream);
    }
    
    @Override
    public void saveInputStream(InputStream inputStream) throws IOException {
        long newFileSize = -1;

        if (swiftFileObject == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            swiftFileObject.uploadObject(inputStream);

        } catch (Exception ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while uploading a local file into a Swift StoredObject";
            }

            throw new IOException(failureMsg);
        }

        // if it has uploaded successfully, we can reset the size
        // of this SwiftAccessIO object:
        setSize(swiftFileObject.getContentLength());
    }

    @Override
    public void delete() throws IOException {
        if (swiftFileObject == null) {
            try {
                swiftFileObject = initializeSwiftFileObject(false);
            } catch (IOException ioex) {
                swiftFileObject = null;
            }
        }

        if (swiftFileObject != null) {
            swiftFileObject.delete();
        }
    }

    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {

        if (isWriteAccessRequested(options)) {
            throw new UnsupportedDataAccessOperationException("SwiftAccessIO: write mode openAuxChannel() not yet implemented in this storage driver.");
        }

        InputStream fin = openSwiftAuxFileAsInputStream(auxItemTag);

        if (fin == null) {
            throw new IOException("Failed to open auxilary file " + auxItemTag + " for Swift file " + getStorageLocation());
        }

        return Channels.newChannel(fin);

    }

    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        StoredObject swiftAuxObject;
        try {
            swiftAuxObject = openSwiftAuxFile(auxItemTag);
        } catch (IOException ioex) {
            swiftAuxObject = null;
        }

        return swiftAuxObject != null;
    }

    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        StoredObject swiftAuxObject = openSwiftAuxFile(auxItemTag);

        if (swiftAuxObject == null) {
            return -1L;
        }

        return swiftAuxObject.getContentLength();
    }

    @Override
    public Path getAuxObjectAsPath(String auxItemTag) throws IOException {
        throw new UnsupportedDataAccessOperationException("SwiftAccessIO: this is a remote DataAccess IO object, its Aux objects have no local filesystem Paths associated with it.");
    }

    @Override
    public void backupAsAux(String auxItemTag) throws IOException {

        if (swiftFileObject == null || swiftContainer == null) {
            open();
        }

        try {
            StoredObject swiftAuxObject = openSwiftAuxFile(true, auxItemTag);
            swiftFileObject.copyObject(swiftContainer, swiftAuxObject);
            // I'm assuming we don't need to delete the main object here - ?
            //swiftFileObject.delete();

        } catch (IOException ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while uploading a local file into a Swift StoredObject";
            }

            throw new IOException(failureMsg);
        }
    }
    
    @Override
    public void revertBackupAsAux(String auxItemTag) throws IOException {
        // We are going to try and overwrite the current main file 
        // with the contents of the stored original, currently saved as an 
        // Aux file. So we need WRITE access on the main file: 
        
        if (swiftFileObject == null || swiftContainer == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        try {
            // We are writing FROM the saved AUX object, back to the main object;
            // So we need READ access on the AUX object:
            
            StoredObject swiftAuxObject = openSwiftAuxFile(auxItemTag);
            swiftAuxObject.copyObject(swiftContainer, swiftFileObject);

        } catch (Exception ex) {
            String failureMsg = ex.getMessage();
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while renaming orig file";
            }

            throw new IOException(failureMsg);
        }

    }

    @Override
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        if (swiftFileObject == null) {
            open();
        }

        try {
            File inputFile = fileSystemPath.toFile();
            StoredObject swiftAuxObject = openSwiftAuxFile(true, auxItemTag);
            swiftAuxObject.uploadObject(inputFile);

        } catch (IOException ex) {
            String failureMsg = ex.getMessage();

            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while uploading a local file into a Swift StoredObject";
            }

            throw new IOException(failureMsg);
        }

    }
    
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag, Long filesize) throws IOException {
        saveInputStreamAsAux(inputStream, auxItemTag);
    }
    
    // this method copies a local InputStream into this DataAccess Auxiliary location:
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        if (swiftFileObject == null) {
            open();
        }

        try {
            StoredObject swiftAuxObject = openSwiftAuxFile(true, auxItemTag);
            swiftAuxObject.uploadObject(inputStream);

        } catch (IOException ex) {
            String failureMsg = ex.getMessage();

            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while saving a local InputStream as a Swift StoredObject";
            }

            throw new IOException(failureMsg);
        }
    }

    @Override
    public List<String> listAuxObjects() throws IOException {
        if (this.swiftContainer == null || this.swiftFileObject == null) {
            throw new IOException("This SwiftAccessIO() hasn't been properly initialized yet.");
        }
        
        String namePrefix = this.swiftFileObject.getName()+".";
        
        Collection<StoredObject> items; 
        String lastItemName = null; 
        List<String> ret = new ArrayList<>();

        while ((items = this.swiftContainer.list(namePrefix, lastItemName, LIST_PAGE_LIMIT)) != null && items.size() > 0) {
            for (StoredObject item : items) {
                lastItemName = item.getName().substring(namePrefix.length());
                ret.add(lastItemName);
            }
        }

        return ret;
    }

    @Override
    public void deleteAuxObject(String auxItemTag) throws IOException {
        StoredObject swiftAuxObject = openSwiftAuxFile(auxItemTag);

        if (swiftAuxObject == null) {
            throw new FileNotFoundException("No such Aux object: " + auxItemTag);
        }

        swiftAuxObject.delete();
    }

    @Override
    public void deleteAllAuxObjects() throws IOException {
        if (this.swiftContainer == null || this.swiftFileObject == null) {
            throw new IOException("This SwiftAccessIO() hasn't been properly initialized yet. (did you execute SwiftAccessIO.open()?)");
        }

        Collection<StoredObject> victims; 
        String lastVictim = null; 
          
        while ((victims = this.swiftContainer.list(this.swiftFileObject.getName()+".", lastVictim, LIST_PAGE_LIMIT))!= null && victims.size() > 0) {
            for (StoredObject victim : victims) {
                lastVictim = victim.getName();
                logger.info("trying to delete " + lastVictim);

                victim.delete();
            }
        }
    }

    @Override
    public String getStorageLocation() {
        // What should this be, for a Swift file? 
        // A Swift URL? 
        // Or a Swift URL with an authenticated Auth token? 
        return null;
    }

    @Override
    public Path getFileSystemPath() throws IOException {
        throw new UnsupportedDataAccessOperationException("SwiftAccessIO: this is a remote DataAccess IO object, it has no local filesystem path associated with it.");
    }

    @Override
    public boolean exists() throws IOException {
        try {
            swiftFileObject = initializeSwiftFileObject(false);
        } catch (FileNotFoundException fnfe) {
            return false;
        }

        return true;
    }

    @Override
    public WritableByteChannel getWriteChannel() throws IOException {
        throw new UnsupportedDataAccessOperationException("SwiftAccessIO: there are no write Channels associated with Swift objects.");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedDataAccessOperationException("SwiftAccessIO: there are no output Streams associated with Swift objects.");
    }

    // Auxilary helper methods, Swift-specific:
    //TODO: rename initializeSwiftObject 
    private StoredObject initializeSwiftFileObject(boolean writeAccess) throws IOException {
        return initializeSwiftFileObject(writeAccess, null);
    }

    private StoredObject initializeSwiftFileObject(boolean writeAccess, String auxItemTag) throws IOException {
        String swiftEndPoint = null;
        String swiftContainerName = null;
        String swiftFileName = null;

        StoredObject fileObject;
        List<String> auxFiles = null; 
        String storageIdentifier = dvObject.getStorageIdentifier();

        if (dvObject instanceof DataFile) {
            Dataset owner = this.getDataFile().getOwner();

            if (storageIdentifier.startsWith("swift://")) {
                // This is a call on an already existing swift object. 

                String[] swiftStorageTokens = storageIdentifier.substring(8).split(":", 3);    

                if (swiftStorageTokens.length != 3) {
                    // bad storage identifier
                    throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
                }

                swiftEndPoint = swiftStorageTokens[0];
                swiftContainerName = swiftStorageTokens[1];
                swiftFileName = swiftStorageTokens[2];

                if (StringUtil.isEmpty(swiftEndPoint) || StringUtil.isEmpty(swiftContainerName) || StringUtil.isEmpty(swiftFileName)) {
                    // all of these things need to be specified, for this to be a valid Swift location
                    // identifier.
                    throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
                }

                if (auxItemTag != null) {
                    swiftFileName = swiftFileName.concat("."+auxItemTag);
                }
            } else if (this.isReadAccess) {
                // An attempt to call Swift driver,  in a Read mode on a non-swift stored datafile
                // object!
                throw new IOException("IO driver mismatch: SwiftAccessIO called on a non-swift stored object.");
            } else if (this.isWriteAccess) {
                Properties p = getSwiftProperties();
                swiftEndPoint = p.getProperty("swift.default.endpoint");

                // Swift uses this to create pseudo-hierarchical folders
                String swiftPseudoFolderPathSeparator = "/";

                //swiftFolderPath = dataFile.getOwner().getDisplayName();
                String swiftFolderPathSeparator = "-";
                String authorityNoSlashes = owner.getAuthority().replace("/", swiftFolderPathSeparator);
                swiftFolderPath = owner.getProtocolForFileStorage() + swiftFolderPathSeparator
                                  + authorityNoSlashes.replace(".", swiftFolderPathSeparator);

                swiftFileName = owner.getIdentifierForFileStorage() + swiftPseudoFolderPathSeparator
                                + storageIdentifier;
                //setSwiftContainerName(swiftFolderPath);
                //swiftFileName = dataFile.getDisplayName();
                //Storage Identifier is now updated after the object is uploaded on Swift.
                dvObject.setStorageIdentifier("swift://" + swiftEndPoint + ":" + swiftFolderPath + ":" + swiftFileName);
            } else {
                throw new IOException("SwiftAccessIO: unknown access mode.");
            }
        } else if (dvObject instanceof Dataset) {
            Dataset dataset = this.getDataset();

            if (storageIdentifier.startsWith("swift://")) {
                // This is a call on an already existing swift object. 

                //TODO: determine how storage identifer will give us info
                String[] swiftStorageTokens = storageIdentifier.substring(8).split(":", 3);    
                //number of tokens should be two because there is not main file
                if (swiftStorageTokens.length != 2) {
                    // bad storage identifier
                    throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
                }

                swiftEndPoint = swiftStorageTokens[0];
                swiftContainerName = swiftStorageTokens[1];
                //We will not have a file name, just an aux tag
                if (auxItemTag != null) {
                    swiftFileName = auxItemTag;
                } else {
                    throw new IOException("Dataset related auxillary files require an auxItemTag");
                }       

                if (StringUtil.isEmpty(swiftEndPoint) || StringUtil.isEmpty(swiftContainerName) || StringUtil.isEmpty(swiftFileName) ) {
                    // all of these things need to be specified, for this to be a valid Swift location
                    // identifier.1
                    throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
                }

            } else if (this.isReadAccess) {
                // An attempt to call Swift driver,  in a Read mode on a non-swift stored datafile
                // object!
                throw new IOException("IO driver mismatch: SwiftAccessIO called on a non-swift stored object.");
            } else if (this.isWriteAccess) {
                Properties p = getSwiftProperties();
                swiftEndPoint = p.getProperty("swift.default.endpoint");
                String swiftFolderPathSeparator = "-";

                // Swift uses this to create pseudo-hierarchical folders
                String swiftPseudoFolderPathSeparator = "/";

                String authorityNoSlashes = dataset.getAuthorityForFileStorage().replace("/", swiftFolderPathSeparator);
                swiftFolderPath = dataset.getProtocolForFileStorage() + swiftFolderPathSeparator +
                    authorityNoSlashes.replace(".", swiftFolderPathSeparator) +
                    swiftPseudoFolderPathSeparator + dataset.getIdentifierForFileStorage();

                swiftFileName = auxItemTag;
                dvObject.setStorageIdentifier("swift://" + swiftEndPoint + ":" + swiftFolderPath);
            } else {
                throw new IOException("SwiftAccessIO: unknown access mode.");
            }
        } else {
            //for future scope, if dataverse is decided to be stored in swift storage containersopen    
            throw new FileNotFoundException("Error initializing swift object");  
        }
        // Authenticate with Swift: 

        // should we only authenticate when account == null? 

        if (this.account == null) {
            account = authenticateWithSwift(swiftEndPoint);
        }

        /*
        The containers created is swiftEndPoint concatenated with the swiftContainerName
        property. Creating container with certain names throws 'Unable to create
        container' error on Openstack. 
        Any datafile with http://rdgw storage identifier i.e present on Object 
        store service endpoint already only needs to look-up for container using
        just swiftContainerName which is the concatenated name.
        In future, a container for the endpoint can be created and for every
        other swiftContainerName Object Store pseudo-folder can be created, which is
        not provide by the joss Java swift library as of yet.
         */
        if (storageIdentifier.startsWith("swift://")) {
            // An existing swift object; the container must already exist as well.
            this.swiftContainer = account.getContainer(swiftContainerName);
        } else {
            // This is a new object being created.
            this.swiftContainer = account.getContainer(swiftFolderPath); //changed from swiftendpoint
        }
        if (!this.swiftContainer.exists()) {
            if (writeAccess) {
                //creates a private data container
                swiftContainer.create();
                if (publicSwiftContainer) {
                    try {
                        //creates a public data container
                        this.swiftContainer.makePublic();
                    }
                    catch (Exception e){
                        //e.printStackTrace();
                        logger.warning("Caught exception "+e.getClass()+" while creating a swift container (it's likely not fatal!)");
                    }
                }
            } else {
                // This is a fatal condition - it has to exist, if we were to 
                // read an existing object!
                throw new IOException("SwiftAccessIO: container " + swiftContainerName + " does not exist.");
            }
        }

        fileObject = this.swiftContainer.getObject(swiftFileName);

        // If this is the main, primary datafile object (i.e., not an auxiliary 
        // object for a primary file), we also set the file download url here: 
        if (auxItemTag == null && dvObject instanceof DataFile) {
            setRemoteUrl(getSwiftFileURI(fileObject));
            if (!this.isWriteAccess && !this.getDataFile().isIngestInProgress()) {
                //otherwise this gets called a bunch on upload
                setTemporarySwiftUrl(generateTemporarySwiftUrl(swiftEndPoint, swiftContainerName, swiftFileName, TEMP_URL_EXPIRES));
                setTempUrlSignature(generateTempUrlSignature(swiftEndPoint, swiftContainerName, swiftFileName, TEMP_URL_EXPIRES));
                setTempUrlExpiry(generateTempUrlExpiry(TEMP_URL_EXPIRES, System.currentTimeMillis()));
            }
            setSwiftFileName(swiftFileName);

            logger.fine(getRemoteUrl() + " success; write mode: " + writeAccess);
        } else {
            logger.fine("sucessfully opened AUX object " + auxItemTag + " , write mode: " + writeAccess);
        }

        if (!writeAccess && !fileObject.exists()) {
            throw new FileNotFoundException("SwiftAccessIO: DvObject " + swiftFileName + " does not exist (Dataverse dvObject id: " + dvObject.getId());
        }

        auxFiles = null; 
        return fileObject;
    }

    private InputStream openSwiftFileAsInputStream() throws IOException {
        swiftFileObject = initializeSwiftFileObject(false);
        this.setSize(swiftFileObject.getContentLength());

        return swiftFileObject.downloadObjectAsInputStream();
    }

    private InputStream openSwiftAuxFileAsInputStream(String auxItemTag) throws IOException {
        return initializeSwiftFileObject(false, auxItemTag).downloadObjectAsInputStream();
    }

    private StoredObject openSwiftAuxFile(String auxItemTag) throws IOException {
        return openSwiftAuxFile(false, auxItemTag);
    }

    private StoredObject openSwiftAuxFile(boolean writeAccess, String auxItemTag) throws IOException {
        return initializeSwiftFileObject(writeAccess, auxItemTag);
    }

    private Properties getSwiftProperties() throws IOException {
        if (swiftProperties == null) {
            String domainRoot = System.getProperties().getProperty("com.sun.aas.instanceRoot");
            String swiftPropertiesFile = domainRoot + File.separator + "config" + File.separator + "swift.properties";
            swiftProperties = new Properties();
            swiftProperties.load(new FileInputStream(new File(swiftPropertiesFile)));
        }

        return swiftProperties;
    }

    Account authenticateWithSwift(String swiftEndPoint) throws IOException {

        Properties p = getSwiftProperties();

        // (this will throw an IOException, if the swift properties file
        // is missing or corrupted)
        String swiftEndPointAuthUrl = p.getProperty("swift.auth_url." + swiftEndPoint);
        String swiftEndPointUsername = p.getProperty("swift.username." + swiftEndPoint);
        String swiftEndPointSecretKey = p.getProperty("swift.password." + swiftEndPoint);
        String swiftEndPointTenantName = p.getProperty("swift.tenant." + swiftEndPoint);
        String swiftEndPointAuthMethod = p.getProperty("swift.auth_type." + swiftEndPoint);
        String swiftEndPointTenantId = p.getProperty("swift.tenant_id." + swiftEndPoint);

        if (swiftEndPointAuthUrl == null || swiftEndPointUsername == null || swiftEndPointSecretKey == null
                || "".equals(swiftEndPointAuthUrl) || "".equals(swiftEndPointUsername) || "".equals(swiftEndPointSecretKey)) {
            // again, all of these things need to be defined, for this Swift endpoint to be 
            // accessible.
            throw new IOException("SwiftAccessIO: no configuration available for endpoint " + swiftEndPoint);
        }

        // Authenticate: 
        Account account = null;

        /*
        This try { } now authenticates using either the KEYSTONE mechanism which uses
        the tenant name in addition to the Username Password and AuthUrl OR the BASIC method
        Also, the AuthUrl is now the identity service endpoint of MOC Openstack
        environment instead of the Object store service endpoint.
         */
        // Keystone vs. Basic vs. Keystone V3
        try {
            if (swiftEndPointAuthMethod.equals("keystone")) {
                logger.fine("Authentication type: keystone v2.0");
                account = new AccountFactory()
                        .setTenantName(swiftEndPointTenantName)
                        .setUsername(swiftEndPointUsername)
                        .setPassword(swiftEndPointSecretKey)
                        .setAuthUrl(swiftEndPointAuthUrl)
                        .createAccount();
            } else if (swiftEndPointAuthMethod.equals("keystone_v3")) {
                logger.fine("Authentication type: keystone_v3");
                account = new AccountFactory()
                        .setTenantName(swiftEndPointTenantName)
                        .setUsername(swiftEndPointUsername)
                        .setAuthenticationMethod(KEYSTONE_V3)
                        .setPassword(swiftEndPointSecretKey)
                        .setAuthUrl(swiftEndPointAuthUrl)
                        .createAccount();
            }
            else { // assume BASIC
                logger.fine("Authentication type: basic");
                account = new AccountFactory()
                        .setUsername(swiftEndPointUsername)
                        .setPassword(swiftEndPointSecretKey)
                        .setAuthUrl(swiftEndPointAuthUrl)
                        .setAuthenticationMethod(BASIC)
                        .createAccount();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException("SwiftAccessIO: failed to authenticate " + swiftEndPointAuthMethod + " for the end point " + swiftEndPoint);
        }

        return account;
    }
    
    private String getSwiftFileURI(StoredObject fileObject) throws IOException {
        try {
            return fileObject.getPublicURL();
        } catch (Exception ex) {
            throw new IOException("SwiftAccessIO: failed to get public URL of the stored object");
        }
    }
    
    //these all get called a lot (20+ times) to load a page
    //lets cache them if the expiry is not expired
    private String hmac = null;
    public String generateTempUrlSignature(String swiftEndPoint, String containerName, String objectName, int duration) throws IOException {
        if (hmac == null || isExpiryExpired(generateTempUrlExpiry(duration, System.currentTimeMillis()), duration, System.currentTimeMillis())) {
            Properties p = getSwiftProperties();
            String secretKey = p.getProperty("swift.hash_key." + swiftEndPoint);
            if (secretKey == null) {
                throw new IOException("Please input a hash key in swift.properties");
            }
            String path = "/v1/" + containerName + "/" + objectName;
            Long expires = generateTempUrlExpiry(duration, System.currentTimeMillis());
            String hmacBody = "GET\n" + expires + "\n" + path;
            try {
                hmac = calculateRFC2104HMAC(hmacBody, secretKey);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return hmac;

    }
    
    private long expiry = -1;
    public long generateTempUrlExpiry(int duration, long currentTime) {
        if (expiry == -1 || isExpiryExpired(expiry, duration, System.currentTimeMillis())) {
            expiry = (currentTime / 1000) + duration;
        }
        return expiry;
    }

    private String temporaryUrl = null;
    private String generateTemporarySwiftUrl(String swiftEndPoint, String containerName, String objectName, int duration) throws IOException {
        Properties p = getSwiftProperties();
        String baseUrl = p.getProperty("swift.swift_endpoint." + swiftEndPoint);
        String path = "/v1/" + containerName + "/" + objectName;
        
        if (temporaryUrl == null || isExpiryExpired(generateTempUrlExpiry(duration, System.currentTimeMillis()), duration, System.currentTimeMillis())) {
            temporaryUrl = baseUrl + path + "?temp_url_sig=" + generateTempUrlSignature(swiftEndPoint, containerName, objectName, duration) + "&temp_url_expires=" + generateTempUrlExpiry(duration, System.currentTimeMillis());
        }
        if (temporaryUrl == null) {
            throw new IOException("Failed to generate the temporary Url");
        }

        return temporaryUrl;
    }
    
    public boolean isExpiryExpired(long expiry, int duration, long currentTime) {
        return ((expiry - duration) * 1000) > currentTime;
    }

    @Override
    public InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException {        
        if (this.isAuxObjectCached(auxItemTag)) {
            return openSwiftAuxFileAsInputStream(auxItemTag);
        } else {
            throw new IOException("SwiftAccessIO: Failed to get aux file as input stream");
        }
    }

    @Override
    public String getSwiftContainerName() {
        String swiftFolderPathSeparator = System.getProperty("dataverse.files.swift-folder-path-separator");
        if (swiftFolderPathSeparator == null) {
            swiftFolderPathSeparator = "_";
        }
        if (dvObject instanceof DataFile) {
            String authorityNoSlashes = this.getDataFile().getOwner().getAuthorityForFileStorage().replace("/", swiftFolderPathSeparator);
            return this.getDataFile().getOwner().getProtocolForFileStorage() + swiftFolderPathSeparator
                   +            authorityNoSlashes.replace(".", swiftFolderPathSeparator) +
                swiftFolderPathSeparator + this.getDataFile().getOwner().getIdentifierForFileStorage();
        }
        return null;
     }
     
    //https://gist.github.com/ishikawa/88599
    public static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        
        return formatter.toString();
    }

    public static String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }
     
}
