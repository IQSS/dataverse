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
// NIO imports: 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Iterator; 


// Dataverse imports:
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.FileNotFoundException;
import java.nio.channels.Channel;
import java.nio.file.StandardCopyOption;


public class FileAccessIO extends DataFileIO {

    public FileAccessIO () throws IOException {
        this(null);
    }

    public FileAccessIO(DataFile dataFile) throws IOException {
        this (dataFile, null);
        
    }

    public FileAccessIO(DataFile dataFile, DataAccessRequest req) throws IOException {

        super(dataFile, req);

        this.setIsLocalFile(true);
    }
    
    private boolean isReadAccess = false;
    private boolean isWriteAccess = false; 
    private Path physicalPath = null; 
    
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
        
        
        if (isReadAccess) {
            FileInputStream fin = openLocalFileAsInputStream();

            if (fin == null) {
                throw new IOException ("Failed to open local file "+getStorageLocation());
            }

            this.setInputStream(fin);
            setChannel(fin.getChannel());
            this.setSize(getLocalFileSize());
            
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
            FileOutputStream fout = openLocalFileAsOutputStream();

            if (fout == null) {
                throw new IOException ("Failed to open local file "+getStorageLocation()+" for writing.");
            }

            this.setOutputStream(fout);
            setChannel(fout.getChannel());
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
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {

        Path auxPath = getAuxFilePath(auxItemTag);

        if (isWriteAccessRequested(options)) {
            FileOutputStream auxOut = new FileOutputStream(auxPath.toFile());

            if (auxOut == null) {
                throw new IOException("Failed to open Auxiliary File " + this.getDataFile().getStorageIdentifier() + "." + auxItemTag + " for writing.");
            }

            return auxOut.getChannel();
        }
        
        // Read access requested.
        // Check if this Aux object is cached; and if so, open for reading:

        if (!auxPath.toFile().exists()) {
            throw new FileNotFoundException("Auxiliary File " + this.getDataFile().getStorageIdentifier() + "." + auxItemTag + " does not exist.");
        }

        FileInputStream auxIn = new FileInputStream(auxPath.toFile());

        if (auxIn == null) {
            throw new IOException("Failed to open Auxiliary File " + this.getDataFile().getStorageIdentifier() + "." + auxItemTag + " for reading");
        }

        return auxIn.getChannel();

    }
    
    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        // Check if the file exists:
        
        Path auxPath = getAuxFilePath(auxItemTag);
        
        if (auxPath.toFile().exists()) {
            return true;
        }
        
        return false; 
    }
    
    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        Path auxPath = getAuxFilePath(auxItemTag);
        
        if (!auxPath.toFile().exists()) {
            throw new FileNotFoundException ("Aux file does not exist.");
        }
        
        return auxPath.toFile().length();
    }
    
    @Override 
    public void backupAsAux(String auxItemTag) throws IOException {
        Path auxPath = getAuxFilePath(auxItemTag);
        
        Files.move(getFileSystemPath(), auxPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    @Override
    public String getStorageLocation() {
        // For a local file, the "storage location" is a complete, absolute
        // filesystem path, with the "file://" prefix:
        
        try {
            Path testPath = getFileSystemPath();
            if (testPath != null) {
                return "file://" + testPath.toString();
            }
        } catch (IOException ioex) {
            // just return null, below:
        }

        return null; 
    }
    
    @Override
    public Path getFileSystemPath() throws IOException {
        if (physicalPath != null) {
            return physicalPath;
        }
        
        String datasetDirectory = getDatasetDirectory(); 
        
        if (this.getDataFile().getStorageIdentifier() == null || "".equals(this.getDataFile().getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        physicalPath = Paths.get(datasetDirectory, this.getDataFile().getStorageIdentifier());
        return physicalPath;

    }
    
    @Override
    public void delete() throws IOException {
        Path victim = getFileSystemPath();
        
        if (victim != null) {
            Files.delete(victim);
        } else {
            throw new IOException("Could not locate physical file location for the Filesystem object.");
        }
    }
    
    // Auxilary helper methods, filesystem access-specific:
    
    private long getLocalFileSize () {
        long fileSize = -1;
        File testFile = null;

        try {
            testFile = getFileSystemPath().toFile();
            if (testFile != null) {
                fileSize = testFile.length();
            }
        } catch (Exception ex) {
            return -1;
        }

        return fileSize;
    }

    public FileInputStream openLocalFileAsInputStream () {
        FileInputStream in;

        try {
            in = new FileInputStream(getFileSystemPath().toFile());
        } catch (Exception ex) {
            // We don't particularly care what the reason why we have
            // failed to access the file was.
            // From the point of view of the download subsystem, it's a
            // binary operation -- it's either successfull or not.
            // If we can't access it for whatever reason, we are saying
            // it's 404 NOT FOUND in our HTTP response.
            // TODO: no, we should probably provide some kind of diagnostics. 
            // -- L.A. 4.0.2
            return null;
        }

        return in;
    }
    
    public FileOutputStream openLocalFileAsOutputStream () {
        FileOutputStream out;

        try {
            out = new FileOutputStream(getFileSystemPath().toFile());
        } catch (Exception ex) {
            // We don't particularly care what the reason why we have
            // failed to access the file was.
            // From the point of view of the download subsystem, it's a
            // binary operation -- it's either successfull or not.
            // If we can't access it for whatever reason, we are saying
            // it's 404 NOT FOUND in our HTTP response.
            // TODO: no, we should probably provide some kind of diagnostics. 
            // -- L.A. 4.0.2
            return null;
        }

        return out;
    }
    
    private String getDatasetDirectory() throws IOException {
        if (this.getDataFile() == null) {
            throw new IOException("No datafile defined in the Data Access Object");
        }

        if (this.getDataFile().getOwner() == null) {
            throw new IOException("Data Access: no parent dataset defined for this datafile");
        }

        Path datasetDirectoryPath = this.getDataFile().getOwner().getFileSystemDirectory();

        if (datasetDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the parent dataset.");
        }
        String datasetDirectory = datasetDirectoryPath.toString();

        if (this.getDataFile().getStorageIdentifier() == null || "".equals(this.getDataFile().getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        return datasetDirectory;
    }
    
    private Path getAuxFilePath(String auxItemTag) throws IOException {

        if (auxItemTag == null || "".equals(auxItemTag)) {
            throw new IOException("Null or invalid Auxiliary Object Tag.");
        }

        String datasetDirectory = getDatasetDirectory();

        if (this.getDataFile().getStorageIdentifier() == null || "".equals(this.getDataFile().getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        Path auxPath = Paths.get(datasetDirectory, this.getDataFile().getStorageIdentifier() + "." + auxItemTag);
        
        if (auxPath == null) {
            throw new IOException("Invalid Path location for the auxiliary file " + this.getDataFile().getStorageIdentifier() + "." + auxItemTag);
        }
        
        return auxPath;
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
    
    private String generateVariableHeader(List dvs) {
        String varHeader = null;

        if (dvs != null) {
            Iterator iter = dvs.iterator();
            DataVariable dv;

            if (iter.hasNext()) {
                dv = (DataVariable) iter.next();
                varHeader = dv.getName();
            }

            while (iter.hasNext()) {
                dv = (DataVariable) iter.next();
                varHeader = varHeader + "\t" + dv.getName();
            }

            varHeader = varHeader + "\n";
        }

        return varHeader;
    }

}