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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Dataverse imports:
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.FileNotFoundException;
import java.nio.channels.Channel;
import java.nio.file.DirectoryStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;


public class FileAccessIO<T extends DvObject> extends StorageIO<T> {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.FileAccessIO");


    public FileAccessIO() {
        // Constructor only for testing
        super(null, null, null);
    }

    public FileAccessIO(T dvObject, DataAccessRequest req, String driverId ) {

        super(dvObject, req, driverId);

        this.setIsLocalFile(true);
    }
    
    // "Direct" File Access IO, opened on a physical file not associated with
    // a specific DvObject
    public FileAccessIO(String storageLocation, String driverId) {
        super(storageLocation, driverId);
        this.setIsLocalFile(true);
        logger.fine("Storage path: " + storageLocation);
        physicalPath = Paths.get(storageLocation);
    }
    
    private Path physicalPath = null; 
    
    @Override
    public void open (DataAccessOption... options) throws IOException {
        DataFile dataFile;
        Dataset dataset;
        Dataverse dataverse = null;
        DataAccessRequest req = this.getRequest();
        
        if (isWriteAccessRequested(options)) {
            isWriteAccess = true;
            isReadAccess = false;
        } else {
            isWriteAccess = false;
            isReadAccess = true;
        }
        
        if (dvObject instanceof DataFile) {
            dataFile = this.getDataFile();
            String storageIdentifier = dataFile.getStorageIdentifier();
            if (req != null && req.getParameter("noVarHeader") != null) {
                this.setNoVarHeader(true);
            }

            if (storageIdentifier == null || "".equals(storageIdentifier)) {
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

                    List<DataVariable> datavariables = dataFile.getDataTable().getDataVariables();
                    String varHeaderLine = generateVariableHeader(datavariables);
                    this.setVarHeader(varHeaderLine);
                }
            } else if (isWriteAccess) {
                // Creates a new directory as needed for a dataset.
                Path datasetPath=Paths.get(getDatasetDirectory());
                if (datasetPath != null && !Files.exists(datasetPath)) {
                    Files.createDirectories(datasetPath);
                }
                FileOutputStream fout = openLocalFileAsOutputStream();

                if (fout == null) {
                    throw new IOException ("Failed to open local file "+getStorageLocation()+" for writing.");
                }

                this.setOutputStream(fout);
                setChannel(fout.getChannel());
                if (!storageIdentifier.startsWith(this.driverId + DataAccess.SEPARATOR)) {
                    dvObject.setStorageIdentifier(this.driverId + DataAccess.SEPARATOR + storageIdentifier);
                }
            }

            this.setMimeType(dataFile.getContentType());

            try {
                this.setFileName(dataFile.getFileMetadata().getLabel());
            } catch (Exception ex) {
                this.setFileName("unknown");
            }
        } else if (dvObject instanceof Dataset) {
            //This case is for uploading a dataset related auxiliary file 
            //e.g. image thumbnails/metadata exports
            dataset = this.getDataset();
            if (isReadAccess) {
                //TODO: Not necessary for dataset as there is no files associated with this
              //  FileInputStream fin = openLocalFileAsInputStream();
//                Path path= dataset.getFileSystemDirectory();                    
//                if (path == null) {
//                    throw new IOException("Failed to locate Dataset"+dataset.getIdentifier());
//                }
//
//                this.setInputStream(fin);  
              } else if (isWriteAccess) {
                //this checks whether a directory for a dataset exists 
                  Path datasetPath=Paths.get(getDatasetDirectory());
                  if (datasetPath != null && !Files.exists(datasetPath)) {
                      Files.createDirectories(datasetPath);
                  }
                dataset.setStorageIdentifier(this.driverId + DataAccess.SEPARATOR + dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage());
            }

        } else if (dvObject instanceof Dataverse) {
            dataverse = this.getDataverse();
        } else {
            logger.fine("Overlay case: FileAccessIO open for : " + physicalPath.toString());
            Path datasetPath= physicalPath.getParent();
            if (datasetPath != null && !Files.exists(datasetPath)) {
                Files.createDirectories(datasetPath);
            }
            //throw new IOException("Data Access: Invalid DvObject type");
        }
        // This "status" is a leftover from 3.6; we don't have a use for it 
        // in 4.0 yet; and we may not need it at all. 
        // -- L.A. 4.0.2
        /*this.setStatus(200);*/
    }
    
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
        
        // Since this is a local fileystem file, we can use the
        // quick NIO Files.copy method: 
        
        Path outputPath = getFileSystemPath();

        if (outputPath == null) {
            throw new FileNotFoundException("FileAccessIO: Could not locate aux file for writing.");
        }
        Files.copy(fileSystemPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        long newFileSize = outputPath.toFile().length();

        // if it has worked successfully, we also need to reset the size
        // of the object. 
        setSize(newFileSize);
    }
    
    @Override
    public void saveInputStream(InputStream inputStream, Long filesize) throws IOException {
        saveInputStream(inputStream);
    }
    
    @Override
    public void saveInputStream(InputStream inputStream) throws IOException {
        // Since this is a local fileystem file, we can use the
        // quick NIO Files.copy method: 

        File outputFile = getFileSystemPath().toFile();

        if (outputFile == null) {
            throw new FileNotFoundException("FileAccessIO: Could not locate file for writing.");
        }
        
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
        inputStream.close();

        // if it has worked successfully, we also need to reset the size
        // of the object. 
        setSize(outputFile.length());
    }
    
    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {
      
        Path auxPath = getAuxObjectAsPath(auxItemTag);

        if (isWriteAccessRequested(options)) {
            if (((dvObject instanceof Dataset) || isDirectAccess()) && !this.canWrite()) {
                // If this is a dataset-level auxilary file (a cached metadata export,
                // dataset logo, etc.) there's a chance that no "real" files 
                // have been saved for this dataset yet, and thus the filesystem 
                // directory does not exist yet. Let's force a proper .open() on 
                // this StorageIO, that will ensure it is created:
                open(DataAccessOption.WRITE_ACCESS);
            }

            FileOutputStream auxOut = new FileOutputStream(auxPath.toFile());

            if (auxOut == null) {
                throw new IOException("Failed to open Auxiliary File " + dvObject.getStorageIdentifier() + "." + auxItemTag + " for writing.");
            }

            return auxOut.getChannel();
        }
        
        // Read access requested.
        // Check if this Aux object is cached; and if so, open for reading:

        if (!auxPath.toFile().exists()) {
            throw new FileNotFoundException("Auxiliary File " + dvObject.getStorageIdentifier() + "." + auxItemTag + " does not exist.");
        }

        FileInputStream auxIn = new FileInputStream(auxPath.toFile());

        if (auxIn == null) {
            throw new IOException("Failed to open Auxiliary File " + dvObject.getStorageIdentifier() + "." + auxItemTag + " for reading");
        }

        return auxIn.getChannel();

    }
    
    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        // Check if the file exists:
        
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        
        return auxPath.toFile().exists();
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
        if(isDirectAccess()) {
            //includes overlay case
            return Paths.get(physicalPath.toString() + "." + auxItemTag);
        }
        String datasetDirectory = getDatasetDirectory();
        
        if (dvObject.getStorageIdentifier() == null || "".equals(dvObject.getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }
        Path auxPath = null;
        if (dvObject instanceof DataFile) {
            auxPath = Paths.get(datasetDirectory, stripDriverId(dvObject.getStorageIdentifier()) + "." + auxItemTag);
        } else if (dvObject instanceof Dataset) {
            auxPath = Paths.get(datasetDirectory, auxItemTag);
        } else if (dvObject instanceof Dataverse) {
        } else {
            throw new IOException("Aux path could not be generated for " + auxItemTag);
        } 
        
        if (auxPath == null) {
            throw new IOException("Invalid Path location for the auxiliary file " + dvObject.getStorageIdentifier() + "." + auxItemTag);
        }
        
        return auxPath;
    }
    

    @Override 
    public void backupAsAux(String auxItemTag) throws IOException {
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        
        Files.move(getFileSystemPath(), auxPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    @Override 
    public void revertBackupAsAux(String auxItemTag) throws IOException {
        Path auxPath = getAuxObjectAsPath(auxItemTag);
        Files.move(auxPath, getFileSystemPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    @Override
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
        if (dvObject instanceof Dataset && !this.canWrite()) {
            // see the comment in openAuxChannel()
            open(DataAccessOption.WRITE_ACCESS);
        }
        // quick Files.copy method: 
        try {
            Path auxPath = getAuxObjectAsPath(auxItemTag);
            Files.copy(fileSystemPath, auxPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
        }
    }
    
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag, Long filesize) throws IOException {
        saveInputStreamAsAux(inputStream, auxItemTag);
    }
    
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
        if (dvObject instanceof Dataset && !this.canWrite()) {
            // see the comment in openAuxChannel()
            open(DataAccessOption.WRITE_ACCESS);
        }
        // Since this is a local fileystem file, we can use the
        // quick NIO Files.copy method: 

        File outputFile = getAuxObjectAsPath(auxItemTag).toFile();

        if (outputFile == null) {
            throw new FileNotFoundException("FileAccessIO: Could not locate aux file for writing.");
        }
        
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
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
        String baseName = stripDriverId(this.getDataFile().getStorageIdentifier()) + ".";
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
        // filesystem path, with the "<driverId>://" prefix:
        
        try {
            Path testPath = getFileSystemPath();
            if (testPath != null) {
                return this.driverId + DataAccess.SEPARATOR + testPath.toString();
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
        
        if (dvObject.getStorageIdentifier() == null || "".equals(dvObject.getStorageIdentifier())) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        physicalPath = Paths.get(datasetDirectory, stripDriverId(dvObject.getStorageIdentifier()));
        return physicalPath;

    }
    
    @Override
    public boolean exists() throws IOException {
        if (getFileSystemPath() == null) {
            throw new FileNotFoundException("FileAccessIO: invalid Access IO object.");
        }
        
        return getFileSystemPath().toFile().exists();
    }
    
    /*@Override
    public void delete() throws IOException {
        Path victim = getFileSystemPath();
        
        if (victim != null) {
            Files.delete(victim);
        } else {
            throw new IOException("Could not locate physical file location for the Filesystem object.");
        }
    }*/
    
    @Override
    public void delete() throws IOException {
        if (!isDirectAccess()) {
            throw new IOException("Direct Access IO must be used to permanently delete stored file objects");
        }

        if (physicalPath == null) {
            throw new IOException("Attempted delete on an unspecified physical path");
        }
        
        deleteAllAuxObjects();
        
        Files.delete(physicalPath);

    }
    
    // Auxilary helper methods, filesystem access-specific:
    
    private long getLocalFileSize () {
        long fileSize = -1;

        try {
            File testFile = getFileSystemPath().toFile();
            if (testFile != null) {
                fileSize = testFile.length();
            }
            return fileSize;
        } catch (IOException ex) {
            return -1;
        }

    }

    public FileInputStream openLocalFileAsInputStream () {
        FileInputStream in;

        try {
            in = new FileInputStream(getFileSystemPath().toFile());
        } catch (IOException ex) {
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
        } catch (IOException ex) {
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
        if (isDirectAccess()) {
            throw new IOException("No DvObject defined in the Data Access Object");
        }

        Path datasetDirectoryPath=null;
        
        if (dvObject instanceof Dataset) {
            datasetDirectoryPath = Paths.get(this.getDataset().getAuthorityForFileStorage(), this.getDataset().getIdentifierForFileStorage());
        } else if (dvObject instanceof DataFile) {
            datasetDirectoryPath = Paths.get(this.getDataFile().getOwner().getAuthorityForFileStorage(), this.getDataFile().getOwner().getIdentifierForFileStorage());
        } else if (dvObject instanceof Dataverse) {
            throw new IOException("FileAccessIO: Dataverses are not a supported dvObject");
        }
            
        if (datasetDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the parent dataset.");
        }
        String datasetDirectory = Paths.get(getFilesRootDirectory(), datasetDirectoryPath.toString()).toString();

        if (dvObject.getStorageIdentifier() == null || dvObject.getStorageIdentifier().isEmpty()) {
            throw new IOException("Data Access: No local storage identifier defined for this datafile.");
        }

        return datasetDirectory;
    }
    
    
    protected String getFilesRootDirectory() {
        String filesRootDirectory = System.getProperty("dataverse.files." + this.driverId + ".directory", "/tmp/files");
        return filesRootDirectory;
    }
    
    private List<Path> listCachedFiles() throws IOException {
        List<Path> auxItems = new ArrayList<>();

        // cached files for a given datafiles are stored on the filesystem
        // as <filesystemname>.*; for example, <filename>.thumb64 or 
        // <filename>.RData.
        
        String baseName; 
        Path datasetDirectoryPath; 
        
        if (isDirectAccess()) {
            baseName = physicalPath.getFileName().toString();
            datasetDirectoryPath = physicalPath.getParent();
            
        } else {
            if (this.getDataFile() == null || this.getDataFile().getStorageIdentifier() == null || this.getDataFile().getStorageIdentifier().isEmpty()) {
                throw new IOException("Null or invalid DataFile in FileAccessIO object.");
            }
        
            baseName = stripDriverId(this.getDataFile().getStorageIdentifier());

            datasetDirectoryPath = Paths.get(getDatasetDirectory());
        }

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

        dirStream.close();
        
        return auxItems;
    }

    @Override
    public InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException {
        InputStream in = null;

        if(this.isAuxObjectCached(auxItemTag))
        {
            Path path=getAuxObjectAsPath(auxItemTag);
            in=Files.newInputStream(path);
        }
        return in;
    }
    private String stripDriverId(String storageIdentifier) {
        int separatorIndex = storageIdentifier.indexOf(DataAccess.SEPARATOR);
        if(separatorIndex>0) {
            return storageIdentifier.substring(separatorIndex + DataAccess.SEPARATOR.length());
        }
        return storageIdentifier;
    }
    
    //Confirm inputs are of the form of a relative file path that doesn't contain . or ..
    protected static boolean isValidIdentifier(String driverId, String storageId) {
        String pathString = storageId.substring(storageId.lastIndexOf("//") + 2);
        String basePath = "/tmp/";
        try {
            String rawPathString = basePath + pathString;
            Path normalized = Paths.get(rawPathString).normalize();
            if(!rawPathString.equals(normalized.toString())) {
                logger.warning("Non-normalized path in submitted identifier " + storageId);
                return false;
            }
            logger.fine(normalized.getFileName().toString());
            if (!usesStandardNamePattern(normalized.getFileName().toString())) {
                logger.warning("Unacceptable file name in submitted identifier: " + storageId);
                return false;
            }

        } catch (InvalidPathException ipe) {
            logger.warning("Invalid Path in submitted identifier " + storageId);
            return false;
        }
        return true;
    }

    private List<String> listAllFiles() throws IOException {
        Dataset dataset = this.getDataset();
        if (dataset == null) {
            throw new IOException("This FileAccessIO object hasn't been properly initialized.");
        }

        Path datasetDirectoryPath = Paths.get(dataset.getAuthorityForFileStorage(), dataset.getIdentifierForFileStorage());
        if (datasetDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the dataset.");
        }

        DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(this.getFilesRootDirectory(), datasetDirectoryPath.toString()));
        
        List<String> res = new ArrayList<>();
        if (dirStream != null) {
            for (Path filePath : dirStream) {
                res.add(filePath.getFileName().toString());
            }
            dirStream.close();
        }
        
        return res;
    }
    
    private void deleteFile(String fileName) throws IOException {
        Dataset dataset = this.getDataset();
        if (dataset == null) {
            throw new IOException("This FileAccessIO object hasn't been properly initialized.");
        }

        Path datasetDirectoryPath = Paths.get(dataset.getAuthorityForFileStorage(), dataset.getIdentifierForFileStorage());
        if (datasetDirectoryPath == null) {
            throw new IOException("Could not determine the filesystem directory of the dataset.");
        }

        Path p = Paths.get(this.getFilesRootDirectory(), datasetDirectoryPath.toString(), fileName);
        Files.delete(p);
    }

    @Override
    public List<String> cleanUp(Predicate<String> filter, boolean dryRun) throws IOException {
        List<String> toDelete = this.listAllFiles().stream().filter(filter).collect(Collectors.toList());
        if (dryRun) {
            return toDelete;
        }
        for (String f : toDelete) {
            this.deleteFile(f);
        }
        return toDelete;
    }

}
