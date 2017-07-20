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
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.FileNotFoundException;
import java.nio.channels.Channel;
import java.nio.file.DirectoryStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;


public class FileAccessIO extends DataFileIO {

    public FileAccessIO () throws IOException {
        this(null);
    }

    public FileAccessIO(DvObject dvObject) throws IOException {
        this (dvObject, null);
        
    }

    public FileAccessIO(DvObject dvObject, DataAccessRequest req) throws IOException {

        super(dvObject, req);

        this.setIsLocalFile(true);
    }
    
    private boolean isReadAccess = false;
    private boolean isWriteAccess = false; 
    private Path physicalPath = null; 
    private DvObjectType dvObjectType;
    
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
        DataFile dataFile = null;
        Dataset dataset = null;
        Dataverse dataverse = null;
        DataAccessRequest req = this.getRequest();
        
        dvObjectType = this.getDvObjectType();
        //if(this.getDvObject().isInstanceofDataFile())
        
        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }
        
        switch(dvObjectType){
            case datafile:
                dataFile = (DataFile)this.getDvObject();
                
                if (req != null && req.getParameter("noVarHeader") != null) {
                    this.setNoVarHeader(true);
                }

                if (dataFile.getStorageIdentifier() == null || "".equals(dataFile.getStorageIdentifier())) {
                    throw new IOException("Data Access: No local storage identifier defined for this datafile.");
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
                break;
                
            case dataset:
                //This case is for uploading a dataset related auxiliary file 
                //e.g. image thumbnails/metadata exports
                //TODO: do we really need to do anything here? should we return the dataset directory?
                dataset = (Dataset)this.getDvObject();
                if (isReadAccess) {
                    
                    FileInputStream fin = openLocalFileAsInputStream();
                    Path path= dataset.getFileSystemDirectory();                    
                    if (path == null) {
                        throw new IOException("Failed to locate Dataset"+dataset.getIdentifier());
                    }

                    this.setInputStream(fin);
                    setChannel(fin.getChannel());
                    this.setSize(getLocalFileSize());
                } else if (isWriteAccess) {
                    //this checks whether a directory for a dataset 
                    if (dataset.getFileSystemDirectory() != null && !Files.exists(dataset.getFileSystemDirectory())) {
                    /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                     */
                    Files.createDirectories(dataset.getFileSystemDirectory());
                    dataset.setStorageIdentifier("file://"+dataset.getAuthority()+dataset.getDoiSeparator()+dataset.getIdentifier());
                    
                    //FileOutputStream fout = openLocalFileAsOutputStream();

                    //if (fout == null) {
                      //  throw new IOException ("Failed to open local file "+getStorageLocation()+" for writing.");
                    //}

                    //this.setOutputStream(fout);
                    //setChannel(fout.getChannel());
                    }
                }
                break;
            case dataverse:
                dataverse = (Dataverse)this.getDvObject();
                break;
            default:
                throw new IOException("Data Access: Invalid DvObject type");
        }
        // This "status" is a leftover from 3.6; we don't have a use for it 
        // in 4.0 yet; and we may not need it at all. 
        // -- L.A. 4.0.2
        this.setStatus(200);
    }
    
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        long newFileSize = -1;
        // Since this is a local fileystem file, we can use the
        // quick NIO Files.copy method: 

        Path outputPath = getFileSystemPath();

        if (outputPath == null) {
            throw new FileNotFoundException("FileAccessIO: Could not locate aux file for writing.");
        }
        Files.copy(fileSystemPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        newFileSize = outputPath.toFile().length();

        // if it has worked successfully, we also need to reset the size
        // of the object. 
        setSize(newFileSize);
    }
    
    @Override
    public void saveInputStream(InputStream inputStream) throws IOException {
        // Since this is a local fileystem file, we can use the
        // quick NIO Files.copy method: 

        File outputFile = getFileSystemPath().toFile();

        if (outputFile == null) {
            throw new FileNotFoundException("FileAccessIO: Could not locate file for writing.");
        }
        
        OutputStream outputStream = new FileOutputStream(outputFile);
        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        
        outputStream.close();
        inputStream.close();

        // if it has worked successfully, we also need to reset the size
        // of the object. 
        setSize(outputFile.length());
    }
    
    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {

        Path auxPath = getAuxObjectAsPath(auxItemTag);

        if (isWriteAccessRequested(options)) {
            FileOutputStream auxOut = new FileOutputStream(auxPath.toFile());

            if (auxOut == null) {
                throw new IOException("Failed to open Auxiliary File " + this.getDvObject().getStorageIdentifier() + "." + auxItemTag + " for writing.");
            }

            return auxOut.getChannel();
        }
        
        // Read access requested.
        // Check if this Aux object is cached; and if so, open for reading:

        if (!auxPath.toFile().exists()) {
            throw new FileNotFoundException("Auxiliary File " + this.getDvObject().getStorageIdentifier() + "." + auxItemTag + " does not exist.");
        }

        FileInputStream auxIn = new FileInputStream(auxPath.toFile());

        if (auxIn == null) {
            throw new IOException("Failed to open Auxiliary File " + this.getDvObject().getStorageIdentifier() + "." + auxItemTag + " for reading");
        }

        return auxIn.getChannel();

    }
    
    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        // Check if the file exists:
        
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        
        if (auxPath.toFile().exists()) {
            return true;
        }
        
        return false; 
    }
    
    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        
        if (!auxPath.toFile().exists()) {
            throw new FileNotFoundException ("Aux file does not exist.");
        }
        
        return auxPath.toFile().length();
    }
    
    @Override
    public Path getAuxObjectAsPath(String auxItemTag) throws IOException {

        if (auxItemTag == null || "".equals(auxItemTag)) {
            throw new IOException("Null or invalid Auxiliary Object Tag.");
        }

        String datasetDirectory = getDatasetDirectory();

        if (this.getDvObject().getStorageIdentifier() == null || "".equals(this.getDvObject().getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }
        Path auxPath = null;
        switch(this.getDvObjectType()) {
            case datafile:
                auxPath = Paths.get(datasetDirectory, this.getDvObject().getStorageIdentifier() + "." + auxItemTag);
                break;
            case dataset:
                auxPath = Paths.get(datasetDirectory, auxItemTag);
                break;
            case dataverse:
                break;
            default:
                throw new IOException("Aux path could not be generated for " + auxItemTag);
                
        } 
        
        if (auxPath == null) {
            throw new IOException("Invalid Path location for the auxiliary file " + this.getDvObject().getStorageIdentifier() + "." + auxItemTag);
        }
        
        return auxPath;
    }
    
    @Override 
    public void backupAsAux(String auxItemTag) throws IOException {
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        
        Files.move(getFileSystemPath(), auxPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    @Override
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        // quick Files.copy method: 
        Path auxPath = null;
        try {
            auxPath = getAuxObjectAsPath(auxItemTag);
        } catch (IOException ex) {
            return;
        }

        if (auxPath != null) {
            Files.copy(fileSystemPath, auxPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        
        // Since this is a local fileystem file, we can use the
        // quick NIO Files.copy method: 

        File outputFile = getAuxObjectAsPath(auxItemTag).toFile();

        if (outputFile == null) {
            throw new FileNotFoundException("FileAccessIO: Could not locate aux file for writing.");
        }
        
        OutputStream outputStream = new FileOutputStream(outputFile);
        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        
        outputStream.close();
        inputStream.close();
    }
    
    @Override
    public List<String>listAuxObjects() throws IOException {
        if (this.getDataFile() == null) {
            throw new IOException("This FileAccessIO object hasn't been properly initialized.");
        }
        
        List<Path> cachedFiles = listCachedFiles();
        
        if (cachedFiles == null) {
            return null;
        }
        
        List<String> cachedFileNames = new ArrayList<>();
        String baseName = this.getDataFile().getStorageIdentifier() + ".";
        for (Path auxPath : cachedFiles) {
            cachedFileNames.add(auxPath.getFileName().toString().substring(baseName.length()));
        }
        
        return cachedFileNames;
    }
    
    @Override
    public void deleteAuxObject(String auxItemTag) throws IOException {
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        Files.delete(auxPath);
    }
    
    @Override
    public void deleteAllAuxObjects() throws IOException {
        List<Path> cachedFiles = listCachedFiles();
        
        if (cachedFiles == null) {
            return;
        }
        
        for (Path auxPath : cachedFiles) {
            Files.delete(auxPath);
        }
        
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
        
        if (this.getDvObject().getStorageIdentifier() == null || "".equals(this.getDvObject().getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        physicalPath = Paths.get(datasetDirectory, this.getDvObject().getStorageIdentifier());
        return physicalPath;

    }
    
    @Override
    public boolean exists() throws IOException {
        if (getFileSystemPath() == null) {
            throw new FileNotFoundException("FileAccessIO: invalid Access IO object.");
        }
        
        return getFileSystemPath().toFile().exists();
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
        if (this.getDvObject()== null) {
            throw new IOException("No DvObject defined in the Data Access Object");
        }

        if (this.getDvObject().getOwner() == null) {
            throw new IOException("Data Access: no parent defined this Object");
        }
        Path datasetDirectoryPath=null;
        
//        try{
        if(this.getDvObjectType().equals(DvObjectType.dataset)){
            datasetDirectoryPath = ((Dataset)this.getDvObject()).getFileSystemDirectory();
        }
        else if(this.getDvObjectType().equals(DvObjectType.datafile)){
            datasetDirectoryPath = this.getDataFile().getOwner().getFileSystemDirectory();
//        }
        }
//        catch(Exception e){
//                System.out.println("Rohit Bhattacharjee EXCEPTION");
//               e.printStackTrace();
//                }
//        
        
        
        if (datasetDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the parent dataset.");
        }
        String datasetDirectory = datasetDirectoryPath.toString();

        if (this.getDvObject().getStorageIdentifier() == null || "".equals(this.getDvObject().getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        return datasetDirectory;
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
    
    private List<Path> listCachedFiles() throws IOException {
        List<Path> auxItems = new ArrayList<>();

        // cached files for a given datafiles are stored on the filesystem
        // as <filesystemname>.*; for example, <filename>.thumb64 or 
        // <filename>.RData.
        
        if (this.getDataFile() == null || this.getDataFile().getStorageIdentifier() == null || this.getDataFile().getStorageIdentifier().equals("")) {
            throw new IOException("Null or invalid DataFile in FileAccessIO object.");
        }
        
        String baseName = this.getDataFile().getStorageIdentifier();

        Path datasetDirectoryPath = this.getDataFile().getOwner().getFileSystemDirectory();

        if (datasetDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the parent dataset.");
        }
        
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path file) throws IOException {
                return (file.getFileName() != null
                        && file.getFileName().toString().startsWith(baseName + "."));
            }
        };

        DirectoryStream<Path> dirStream = Files.newDirectoryStream(datasetDirectoryPath, filter);
        
        if (dirStream != null) {
            for (Path filePath : dirStream) {
                auxItems.add(filePath);
            }
        }   

        return auxItems;
    }

//    // TODO: add logic to check for existing metadata exports as well
//    @Override
//    public boolean fileExists(Path path) throws IOException {
//        if (Files.exists(path)) {
//            return true;
//            } else return false;
//    }

    @Override
    public InputStream getAuxFile(String auxItemTag) throws IOException {
        InputStream in = null;
//        Path path
        if(this.isAuxObjectCached(auxItemTag))
        {
            Path path=getAuxObjectAsPath(auxItemTag);
            in=Files.newInputStream(path);
        }
       
       
        
        return in;
    }

}