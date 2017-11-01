/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.dataaccess;

// java core imports:
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream; 
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Iterator; 

// NIO imports: 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.nio.channels.Channel;
import java.nio.file.StandardCopyOption;

// JavaSwift imports: 


import org.javaswift.joss.client.factory.AccountFactory;
import static org.javaswift.joss.client.factory.AuthenticationMethod.BASIC;
import static org.javaswift.joss.client.factory.AuthenticationMethod.KEYSTONE;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;




// Dataverse imports:
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.nio.channels.Channels;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javaswift.joss.client.factory.AccountConfig;


/**
 *
 * @author leonid andreev
 */
/* 
    Experimental Swift driver, implemented as part of the Dataverse - Mass Open Cloud
    collaboration. 
    Read-only access, for now. 
*/
public class SwiftAccessIO extends DataFileIO {
    
    private String swiftFolderPath;
    private String swiftFileName = null;
        
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO");

    public SwiftAccessIO () throws IOException {
        this(null);
    }

    public SwiftAccessIO(DataFile dataFile) throws IOException {
        this (dataFile, null);
        
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
    
    @Override
    public boolean canRead () {
        return isReadAccess;
    }
    
    @Override
    public boolean canWrite () {
        return isWriteAccess; 
    }
    

    @Override
    public void open (DataAccessOption... options) throws IOException {

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
                throw new IOException ("Failed to open Swift file "+getStorageLocation());
            }

            this.setInputStream(fin);
            setChannel(Channels.newChannel(fin));
            

        } else if (isWriteAccess) {
            swiftFileObject = initializeSwiftFileObject(true);
        }

        this.setMimeType(dataFile.getContentType());
        try {
            this.setFileName(dataFile.getFileMetadata().getLabel());
        } catch (Exception ex) {
            this.setFileName("unknown");
        }

        // This "status" is a leftover from 3.6; we don't have a use for it 
        // in 4.0 yet; and we may not need it at all. 
        // -- L.A. 4.0.2
        this.setStatus(200);
    }
    
    // this is a Swift-specific override of the convenience method provided in the 
    // DataFileIO for copying a local Path (for ex., a temp file, into this DataAccess location):
    @Override
    public void copyPath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;
        Properties p = getSwiftProperties();
        String swiftEndPoint = p.getProperty("swift.default.endpoint");
        String swiftDirectory = p.getProperty("swift.swift_endpoint."+swiftEndPoint);

        
        if (swiftFileObject == null || !this.canWrite()) {
            open(DataAccessOption.WRITE_ACCESS);
        }
        
        File inputFile = null;

        try {
            inputFile = fileSystemPath.toFile();
            
            //@author Anuj Thakur 
            swiftFileObject.uploadObject(inputFile);
            //After the files object is uploaded the identifier is changed.
            logger.info(this.swiftFileName+" "+this.swiftFolderPath);
            this.getDataFile().setStorageIdentifier(swiftDirectory+"/"+this.swiftFolderPath+"/"+this.swiftFileName);
                 
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
    public void delete() throws IOException {
        //throw new IOException("SwiftAccessIO: delete() not yet implemented in this storage driver.");
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

        throw new IOException("SwiftAccessIO: openAuxChannel() not yet implemented in this storage driver.");
    }
    
    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        throw new IOException("SwiftAccessIO: isAuxObjectCached() not yet implemented in this storage driver.");
    }
    
    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        throw new IOException("SwiftAccessIO: getAuxObjectSize() not yet implemented in this storage driver.");
    }
    
    @Override 
    public void backupAsAux(String auxItemTag) throws IOException {
        throw new IOException("SwiftAccessIO: backupAsAux() not yet implemented in this storage driver.");
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
        throw new IOException("SwiftAccessIO: this is a remote AccessIO object, it has no local filesystem path associated with it.");
    }
    
    // Auxilary helper methods, Swift-specific:
    private StoredObject initializeSwiftFileObject(boolean writeAccess) throws IOException {
        String storageIdentifier = this.getDataFile().getStorageIdentifier();
        
        String swiftEndPoint = null; 
        String swiftContainer = null; 
        
       //
       // if (storageIdentifier.startsWith("swift://")) {
        if (storageIdentifier.startsWith(DataAccess.DEFAULT_SWIFT_ENDPOINT_START_CHARACTERS)) {
            // This is a call on an already existing swift object. 

            //String[] swiftStorageTokens = storageIdentifier.substring(8).split(":", 3);
            //The Storage identifier now is the Object store service endpoint
            //followed by the endpoint named container
            String[] swiftStorageTokens = storageIdentifier.substring(46).split("/", 2);
            
            if (swiftStorageTokens.length != 2) {
                // bad storage identifier
                throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
            }
                        
            Properties p = getSwiftProperties();
            swiftEndPoint = p.getProperty("swift.default.endpoint");
            
            swiftContainer = swiftStorageTokens[0];
            swiftFileName = swiftStorageTokens[1];
            
            
            if (swiftEndPoint == null || swiftContainer == null || swiftFileName == null
                    || "".equals(swiftEndPoint) || "".equals(swiftContainer) || "".equals(swiftFileName)) {
                // all of these things need to be specified, for this to be a valid Swift location
                // identifier.
                throw new IOException("SwiftAccessIO: invalid swift storage token: " + storageIdentifier);
            }
        } else if (this.isReadAccess) {
            // An attempt to call Swift driver,  in a Read mode on a non-swift stored datafile
            // object!
            throw new IOException("IO driver mismatch: SwiftAccessIO called on a non-swift stored object.");
        } else if (this.isWriteAccess) {
            Properties p = getSwiftProperties();
            swiftEndPoint = p.getProperty("swift.default.endpoint");
            swiftFolderPath = this.getDataFile().getOwner().getDisplayName();
            //swiftFolderPath = this.getDataFile().getOwner().getIdentifier(); /* TODO: ? */
//            swiftFileName = storageIdentifier;
            swiftFileName = this.getDataFile().getDisplayName();
            //Storage Identifier is now updated after the object is uploaded on Swift.
            // this.getDataFile().setStorageIdentifier("swift://"+swiftEndPoint+":"+swiftContainer+":"+swiftFileName);
        } else {
            throw new IOException("SwiftAccessIO: unknown access mode.");
        }
        // Authenticate with Swift: 
        
        account = authenticateWithSwift(swiftEndPoint);
        
        /*
        The containers created is swiftEndPoint concatenated with the swiftContainer
        property. Creating container with certain names throws 'Unable to create
        container' error on Openstack. 
        Any datafile with http://rdgw storage identifier i.e present on Object 
        store service endpoint already only needs to look-up for container using
        just swiftContainer which is the concatenated name.
        In future, a container for the endpoint can be created and for every
        other swiftContainer Object Store pseudo-folder can be created, which is
        not provide by the joss Java swift library as of yet.
        */
        Container dataContainer;
          
        if (storageIdentifier.startsWith(DataAccess.DEFAULT_SWIFT_ENDPOINT_START_CHARACTERS)) {
            dataContainer = account.getContainer(swiftContainer); 
        } else {
            dataContainer = account.getContainer(swiftFolderPath); //changed from swiftendpoint
        }
        
        if (!dataContainer.exists()) {
            if (writeAccess) {
                dataContainer.create();
                //dataContainer.makePublic();
            } else {
                // This is a fatal condition - it has to exist, if we were to 
                // read an existing object!
                throw new IOException("SwiftAccessIO: container "+swiftContainer+" does not exist.");
            }
        }
        
        StoredObject fileObject = dataContainer.getObject(swiftFileName);
        //file download url for public files
        DataAccess.swiftFileUri = DataAccess.getSwiftFileURI(fileObject);
        setRemoteUrl(DataAccess.getSwiftFileURI(fileObject));

        logger.info(DataAccess.swiftFileUri + " success");
        //shows contents of container for public containers
        DataAccess.swiftContainerUri = DataAccess.getSwiftContainerURI(fileObject);
        logger.info(DataAccess.swiftContainerUri + " success");
        
        if (!writeAccess && !fileObject.exists()) {
            throw new IOException("SwiftAccessIO: File object "+swiftFileName+" does not exist (Dataverse datafile id: "+this.getDataFile().getId());
        }
        
        return fileObject; 
    }
    
    private InputStream openSwiftFileAsInputStream () throws IOException {
        InputStream in = null;
        
        swiftFileObject = initializeSwiftFileObject(false);        
        
        in = swiftFileObject.downloadObjectAsInputStream();
        this.setSize(swiftFileObject.getContentLength());
        
        
        return in;
    }
    
    private Properties getSwiftProperties () throws IOException {
        if (swiftProperties == null) {
            String domainRoot = System.getProperties().getProperty("com.sun.aas.instanceRoot");
            String swiftPropertiesFile = domainRoot+File.separator+"config"+File.separator+"swift.properties";
            swiftProperties = new Properties();
            swiftProperties.load(new FileInputStream(new File(swiftPropertiesFile)));
        }
        
        return swiftProperties;
    }
    
    Account authenticateWithSwift(String swiftEndPoint) throws IOException {
        
        Properties p = getSwiftProperties();
        
        // (this will throw an IOException, if the swift properties file
        // is missing or corrupted)
        
        String swiftEndPointAuthUrl = p.getProperty("swift.auth_url."+swiftEndPoint);
        String swiftEndPointUsername = p.getProperty("swift.username."+swiftEndPoint);
        String swiftEndPointSecretKey = p.getProperty("swift.password."+swiftEndPoint);
        String swiftEndPointTenantName = p.getProperty("swift.tenant."+swiftEndPoint);
        String swiftEndPointAuthMethod = p.getProperty("swift.auth_type."+swiftEndPoint);
        
        if (swiftEndPointAuthUrl == null || swiftEndPointUsername == null || swiftEndPointSecretKey == null ||
                "".equals(swiftEndPointAuthUrl) || "".equals(swiftEndPointUsername) || "".equals(swiftEndPointSecretKey)) {
            // again, all of these things need to be defined, for this Swift endpoint to be 
            // accessible.
            throw new IOException("SwiftAccessIO: no configuration available for endpoint "+swiftEndPoint); 
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
            throw new IOException("SwiftAccessIO: failed to authenticate " + swiftEndPointAuthMethod + " for the end point "+swiftEndPoint);
        }
        
        return account;
    }
    
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