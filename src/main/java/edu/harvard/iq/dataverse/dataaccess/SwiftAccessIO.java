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
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;




// Dataverse imports:
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.nio.channels.Channels;
import java.util.Properties;


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
    
    @Override
    public boolean canRead () {
        return isReadAccess;
    }
    
    @Override
    public boolean canWrite () {
        return false; 
    }


    @Override
    public void open (DataAccessOption... options) throws IOException {

        DataFile dataFile = this.getDataFile();
        DataAccessRequest req = this.getRequest(); 


        if (req != null && req.getParameter("noVarHeader") != null) {
            this.setNoVarHeader(true);
        }
        
        if (isWriteAccessRequested(options)) {
            throw new IOException("Write access not yet implemented.");
        } 
        
        isReadAccess = true; 
        
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
            // Not yet implemented.
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
    
    @Override
    public void delete() throws IOException {
        throw new IOException("SwiftAccessIO: delete() not yet implemented in this storage driver.");
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
    

    public InputStream openSwiftFileAsInputStream () {
        InputStream in = null;

        String storageIdentifier = this.getDataFile().getStorageIdentifier();
        
        if (!storageIdentifier.startsWith("swift://")) {
            // An attempt to call Swift driver on a non-swift stored datafile
            // object!
            return null; 
        }
        
        
        
        String[] swiftStorageTokens = storageIdentifier.substring(8).split(":", 3);
        
        if (swiftStorageTokens.length != 3) {
            // bad storage identifier
            return null; 
        }
        
        String swiftEndPoint = swiftStorageTokens[0]; 
        String swiftContainer = swiftStorageTokens[1]; 
        String swiftFileName = swiftStorageTokens[2]; 
        
        if (swiftEndPoint == null || swiftContainer == null || swiftFileName == null ||
                "".equals(swiftEndPoint) || "".equals(swiftContainer) || "".equals(swiftFileName)) {
            // all of these things need to be specified, for this to be a valid Swift location
            // identifier.
            return null;
        }
        
        
        String domainRoot = System.getProperties().getProperty("com.sun.aas.instanceRoot");
        String swiftPropertiesFile = domainRoot+File.separator+"config"+File.separator+"swift.properties";
        
        Properties p=new Properties();
        try { 
            p.load(new FileInputStream(new File(swiftPropertiesFile)));
        } catch (IOException ioex) {
            // Missing/corrupted swift properties file, probably. 
            // We'll have more diagnostics for this in the future. 
            return null; 
        }
        
        String swiftEndPointAuthUrl = p.getProperty("swift.auth_url."+swiftEndPoint);
        String swiftEndPointUsername = p.getProperty("swift.username."+swiftEndPoint);
        String swiftEndPointSecretKey = p.getProperty("swift.password."+swiftEndPoint);
        
        if (swiftEndPointAuthUrl == null || swiftEndPointUsername == null || swiftEndPointSecretKey == null ||
                "".equals(swiftEndPointAuthUrl) || "".equals(swiftEndPointUsername) || "".equals(swiftEndPointSecretKey)) {
            // again, all of these things need to be defined, for this Swift endpoint to be 
            // accessible.
            return null;
        }
        
        
        // Authenticate: 
        
        Account account = null; 
        
        try {
            account = new AccountFactory()
                    .setUsername(swiftEndPointUsername)
                    .setPassword(swiftEndPointSecretKey)
                    .setAuthUrl(swiftEndPointAuthUrl)
                    .setAuthenticationMethod(BASIC)
                    .createAccount();
        } catch (Exception ex) {
            return null; 
        }
        
        Container dataContainer = account.getContainer(swiftContainer);
        if (!dataContainer.exists()) {
            return null; 
        }
        
        StoredObject fileObject = dataContainer.getObject(swiftFileName);
        
        if (!fileObject.exists()) {
            return null; 
        }
        
        in = fileObject.downloadObjectAsInputStream();
        this.setSize(fileObject.getContentLength());
        
        
        return in;
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