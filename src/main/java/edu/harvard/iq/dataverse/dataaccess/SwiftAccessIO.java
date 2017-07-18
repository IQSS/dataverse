package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
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
import org.javaswift.joss.client.factory.AccountFactory;
import static org.javaswift.joss.client.factory.AuthenticationMethod.BASIC;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

/**
 *
 * @author leonid andreev
 */
/* 
    Experimental Swift driver, implemented as part of the Dataverse - Mass Open Cloud
    collaboration. 
 */
public class SwiftAccessIO extends DataFileIO {

    private String swiftFolderPath;

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO");

    public SwiftAccessIO() throws IOException {
        this(null);
    }

    public SwiftAccessIO(DataFile dataFile) throws IOException {
        this(dataFile, null);

    }

    public SwiftAccessIO(DataFile dataFile, DataAccessRequest req) throws IOException {

        super(dataFile, req);

        this.setIsLocalFile(false);
    }

    private boolean isReadAccess = false;
    private boolean isWriteAccess = false;
    private Properties swiftProperties = null;
    private Account account = null;
    private StoredObject swiftFileObject = null;
    private Container swiftContainer = null;
    
    private static int LIST_PAGE_LIMIT = 100;  

    @Override
    public boolean canRead() {
        return isReadAccess;
    }

    @Override
    public boolean canWrite() {
        return isWriteAccess;
    }

    @Override
    public void open(DataAccessOption... options) throws IOException {

        DataFile dataFile = this.getDataFile();
        DataAccessRequest req = this.getRequest();

        if (req != null && req.getParameter("noVarHeader") != null) {
            this.setNoVarHeader(true);
        }

        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }

        if (this.getDataFile().getStorageIdentifier() == null || "".equals(this.getDataFile().getStorageIdentifier())) {
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

                List datavariables = dataFile.getDataTable().getDataVariables();
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
    }

    // DataFileIO method for copying a local Path (for ex., a temp file), into this DataAccess location:

    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;

        if (swiftFileObject == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }

        File inputFile = null;

        try {
            inputFile = fileSystemPath.toFile();

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
    
    @Override public Path getAuxObjectAsPath(String auxItemTag) throws IOException {
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

        } catch (Exception ioex) {
            String failureMsg = ioex.getMessage();
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while uploading a local file into a Swift StoredObject";
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

        File inputFile = null;

        try {
            inputFile = fileSystemPath.toFile();
            StoredObject swiftAuxObject = openSwiftAuxFile(true, auxItemTag);
            swiftAuxObject.uploadObject(inputFile);

        } catch (Exception ex) {
            String failureMsg = ex.getMessage();
            
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while uploading a local file into a Swift StoredObject";
            }

            throw new IOException(failureMsg);
        }

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

        } catch (Exception ex) {
            String failureMsg = ex.getMessage();
            
            if (failureMsg == null) {
                failureMsg = "Swift AccessIO: Unknown exception occured while saving a local InputStream as a Swift StoredObject";
            }

            throw new IOException(failureMsg);
        }    
    }
    
    @Override
    public List<String>listAuxObjects() throws IOException {
        if (this.swiftContainer == null || this.swiftFileObject == null) {
            throw new IOException("This SwiftAccessIO() hasn't been properly initialized yet.");
        }
        
        String namePrefix = this.swiftFileObject.getName()+".";
        
        Collection<StoredObject> items = null; 
        String lastItemName = null; 
        List<String> ret = new ArrayList<>();
        
        while ((items = this.swiftContainer.list(namePrefix, lastItemName, LIST_PAGE_LIMIT))!= null && items.size() > 0) {
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
            throw new FileNotFoundException("No such Aux object: "+auxItemTag);
        }
        
        swiftAuxObject.delete();
    }
    
    @Override
    public void deleteAllAuxObjects() throws IOException {
    if (this.swiftContainer == null || this.swiftFileObject == null) {
            throw new IOException("This SwiftAccessIO() hasn't been properly initialized yet. (did you execute SwiftAccessIO.open()?)");
        }
        
        Collection<StoredObject> victims = null; 
        String lastVictim = null; 
        
        
        while ((victims = this.swiftContainer.list(this.swiftFileObject.getName()+".", lastVictim, LIST_PAGE_LIMIT))!= null && victims.size() > 0) {
            for (StoredObject victim : victims) {
                lastVictim = victim.getName();
                logger.info("trying to delete "+lastVictim);

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
    
    
    private StoredObject initializeSwiftFileObject(boolean writeAccess) throws IOException {
        return initializeSwiftFileObject(writeAccess, null);
    }
    
    private StoredObject initializeSwiftFileObject(boolean writeAccess, String auxItemTag) throws IOException {
        String storageIdentifier = this.getDataFile().getStorageIdentifier();

        String swiftEndPoint = null;
        String swiftContainerName = null;
        String swiftFileName = null;

        
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

            //swiftFolderPath = this.getDataFile().getOwner().getDisplayName();
            
            swiftFolderPath = getSwiftContainerName();
            setSwiftContainerName(swiftFolderPath);

            swiftFileName = storageIdentifier;
            //setSwiftContainerName(swiftFolderPath);
            //swiftFileName = this.getDataFile().getDisplayName();
            //Storage Identifier is now updated after the object is uploaded on Swift.
            this.getDataFile().setStorageIdentifier("swift://"+swiftEndPoint+":"+swiftFolderPath+":"+swiftFileName);
        } else {
            throw new IOException("SwiftAccessIO: unknown access mode.");
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
                // dataContainer.create();
                 try {
                     //creates a public data container
                     this.swiftContainer.makePublic();
                 }
                 catch (Exception e){
                     //e.printStackTrace();
                     logger.warning("Caught exception "+e.getClass()+" while creating a swift container (it's likely not fatal!)");
                 }
            } else {
                // This is a fatal condition - it has to exist, if we were to 
                // read an existing object!
                throw new IOException("SwiftAccessIO: container " + swiftContainerName + " does not exist.");
            }
        }

        StoredObject fileObject = this.swiftContainer.getObject(swiftFileName);
        
        
        // If this is the main, primary datafile object (i.e., not an auxiliary 
        // object for a primary file), we also set the file download url here: 
        if (auxItemTag == null) {
            setRemoteUrl(getSwiftFileURI(fileObject));
            logger.fine(getRemoteUrl() + " success; write mode: "+writeAccess);
        } else {
            logger.fine("sucessfully opened AUX object "+auxItemTag+" , write mode: "+writeAccess);
        }
        
        if (!writeAccess && !fileObject.exists()) {
            throw new FileNotFoundException("SwiftAccessIO: File object " + swiftFileName + " does not exist (Dataverse datafile id: " + this.getDataFile().getId());
        }

        List<String> auxFiles = null; 
        
        return fileObject;
    }

    private InputStream openSwiftFileAsInputStream() throws IOException {
        InputStream in = null;

        swiftFileObject = initializeSwiftFileObject(false);

        in = swiftFileObject.downloadObjectAsInputStream();
        this.setSize(swiftFileObject.getContentLength());

        return in;
    }

    private InputStream openSwiftAuxFileAsInputStream(String auxItemTag) throws IOException {
        InputStream in = null;

        StoredObject swiftAuxFileObject = initializeSwiftFileObject(false, auxItemTag);

        in = swiftAuxFileObject.downloadObjectAsInputStream();

        return in;
    }
    
    private StoredObject openSwiftAuxFile(String auxItemTag) throws IOException {
        return openSwiftAuxFile(false, auxItemTag); 
    }
    
    private StoredObject openSwiftAuxFile(boolean writeAccess, String auxItemTag) throws IOException {
        StoredObject swiftAuxFileObject = initializeSwiftFileObject(writeAccess, auxItemTag);
        
        return swiftAuxFileObject; 
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
        String swiftEndPointUrl = p.getProperty("swift.swift_endpoint." + swiftEndPoint);
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
        // Keystone vs. Basic
        try {
            if (swiftEndPointAuthMethod.equals("keystone")) {
                account = new AccountFactory()
                        .setTenantName(swiftEndPointTenantName)
                        // .setTenantId(swiftEndPointTenantId)
                        .setUsername(swiftEndPointUsername)
                        .setPassword(swiftEndPointSecretKey)
                        .setAuthUrl(swiftEndPointAuthUrl)
                        .createAccount();
            } else { // assume BASIC
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

    private boolean isWriteAccessRequested(DataAccessOption... options) throws IOException {

        for (DataAccessOption option : options) {
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
    
    private String getSwiftFileURI(StoredObject fileObject) throws IOException {
        String fileUri;
        try {
            fileUri = fileObject.getPublicURL();
        } catch (Exception ex) {
            //ex.printStackTrace();
            throw new IOException("SwiftAccessIO: failed to get public URL of the stored object");
        }
        return fileUri;
    }

     public String getSwiftContainerName() {
        String swiftFolderPathSeparator = System.getProperty("dataverse.files.swift-folder-path-separator");
        if (swiftFolderPathSeparator == null) {
            swiftFolderPathSeparator = "_";
        }
        String authorityNoSlashes = this.getDataFile().getOwner().getAuthority().replace(this.getDataFile().getOwner().getDoiSeparator(), swiftFolderPathSeparator);
        String containerName = this.getDataFile().getOwner().getProtocol() + swiftFolderPathSeparator +
            authorityNoSlashes.replace(".", swiftFolderPathSeparator) +
            swiftFolderPathSeparator + this.getDataFile().getOwner().getIdentifier();
        
        return containerName;
     }
     


}
