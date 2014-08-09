/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.ejb.EJB;

/**
 *  Used by the IngestServiceBean to redistribute a zipped Shapefile*
 * 
 *  - Accomplish the actions described in the ShapefileHandler function "rezipShapefileSets"
 *  - Return a list of DataFile objects
 * 
 * @author rmp553
 */
public class IngestServiceShapefileHelper {
    
    @EJB
    DataFileServiceBean fileService; 

    /*
        code to add to IngestServiceBean
            File rezipFolder = this.getShapefileUnzipTempDirectory();
            IngestServiceShapefileHelper shapefileIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);
            List<DataFile> dataFiles = shapefileIngestHelper.getDataFiles();
            rezipFolder.delete();
     */
    
    private static final Logger logger = Logger.getLogger(IngestServiceShapefileHelper.class.getCanonicalName());

    private ShapefileHandler shpHandler;
    private DatasetVersion datasetVersion;
    List<DataFile> dataFileList = null;
    private File zippedShapefile;
    private File rezipFolder;

  
    
    public List<DataFile> getDataFileList(){
        if (this.dataFileList==null){
            return null;
        }
        if (this.dataFileList.size()==0){
            return null;
        }
        return this.dataFileList;
    }
    
    private boolean isValidFile(File fileObject){
        
         if (fileObject==null){
            logger.warning("fileObject was null");
            return false;
        }
        if (!fileObject.isFile()){
            logger.warning("fileObject was not a file.  Failed \"isFile()\": " + fileObject.getAbsolutePath());
            return false;
        }
        return true;
    }
    
    
   private boolean isValidFolder(File fileObject){
        
         if (fileObject==null){
            logger.warning("fileObject was null");
            return false;
        }
        if (!fileObject.isDirectory()){
            logger.warning("fileObject was not a directory.  Failed \"isFile()\": " + fileObject.getAbsolutePath());
            return false;
        }
        return true;
    }
    /*
        Constructor that accepts a file object
    */
   IngestServiceShapefileHelper(File zippedShapefile, File rezipFolder){
        
        if ((!isValidFile(zippedShapefile))||(!isValidFolder(rezipFolder))){
            return;
        }
        this.zippedShapefile = zippedShapefile;
        this.rezipFolder = rezipFolder;
        //this.datasetVersion = version;

        //this.processFile(zippedShapefile, rezipFolder);

    }
    
   private FileInputStream getFileInputStream(File fileObject){
       if (fileObject==null){
           return null;
       }
       try {
            return new FileInputStream(fileObject);
        } catch (FileNotFoundException ex) {
            logger.severe("Failed to create FileInputStream from File: " + fileObject.getAbsolutePath());
            return null;
        }
   }
   
   private void closeFileInputStream(FileInputStream fis){
       if (fis==null){
           return;
       }
        try {
            fis.close();            
        } catch (IOException ex) {
            logger.info("Failed to close FileInputStream");
        }
   }
   
    public boolean processFile() {
       
       if ((!isValidFile(this.zippedShapefile))||(!isValidFolder(this.rezipFolder))){
            return false;
        }
        
       // (1) Use the ShapefileHandler to the .zip for a shapefile
       //
        FileInputStream shpfileInputStream = this.getFileInputStream(zippedShapefile);
        if (shpfileInputStream==null){
            return false;
        }
        
        this.shpHandler = new ShapefileHandler(shpfileInputStream);
        if (!shpHandler.containsShapefile()){
            logger.severe("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");
            return false;
        }

        this.closeFileInputStream(shpfileInputStream);
        
        //  (2) Rezip the shapefile pieces
        logger.info("rezipFolder: " + rezipFolder.getAbsolutePath());
        shpfileInputStream = this.getFileInputStream(zippedShapefile);
        if (shpfileInputStream==null){
            return false;
        }

        boolean rezipSuccess;
        try {
            rezipSuccess = shpHandler.rezipShapefileSets(shpfileInputStream, rezipFolder);
        } catch (IOException ex) {
            logger.severe("Shapefile was not correctly unpacked/repacked");
            logger.severe("shpHandler message: " + shpHandler.errorMessage);
            return false;
        }
        
        this.closeFileInputStream(shpfileInputStream);
        
        return rezipSuccess;
     
        //   return createDataFiles(rezipFolder);
        
    }
    
    
    public List<File> getFinalRezippedFiles(){
        if (this.shpHandler==null){
            return null;
        }
        return this.shpHandler.getFinalRezippedFiles();
    }
    /*
        Note: This creates DataFile objects, the files themselves already exist!
    */
    private boolean createDataFiles(File rezipFolder){
        if (!isValidFolder(rezipFolder)){
            return false;
        }
        
        // Initialize dataFileList
        this.dataFileList = new ArrayList<DataFile>(); 

        for (File singleFile : rezipFolder.listFiles()){
            if (!singleFile.isFile()){
                continue; // not a file
            }
            DataFile datafile = createSingleDataFile(singleFile);
            if (!(datafile==null)){
                   this.dataFileList.add(datafile);
            }           
        }
        
        return false;
    }
    
    /*
        Note: This creates a DataFile object and a related FileMetadata object
        -> The files themselves already exist!
    */
    private DataFile createSingleDataFile(File inputFile){
        if (!this.isValidFile(inputFile)){
            logger.warning("createSingleDataFile: fileObject was not a file.");
            return null;
        }
        
        // (1) Get file name
        String fileName = inputFile.getName();
        if (fileName==null){
           logger.warning("Unusual: inputFile.getName() returned null");
            return null;
        }
        
        // (2) Determine file type
        String contentType;
        try {
            contentType = FileUtil.determineFileType(inputFile, fileName);
        } catch (IOException ex) {
            logger.fine("FileUtil.determineFileType failed for file with name: " + fileName);
            contentType = null;
        }
        
        if ((contentType==null)||(contentType=="")){
            contentType = "application/octet-stream";
        }
        
        // (3) - Start making a DataFile
        DataFile datafile = new DataFile(contentType);
        

        // Set the data file owner
        Dataset dataFileOwner = this.datasetVersion.getDataset();
        if (dataFileOwner == null){
          logger.severe("method call failed: this.datasetVersion.getDataset()");
          return null;
        }        
        datafile.setOwner(dataFileOwner);
        
        // (3a) - Start making the FileMetadata
        FileMetadata fileMetadata= new FileMetadata();
        
        // set the FileMetadata label
        fileMetadata.setLabel(fileName);
        fileMetadata.setDataFile(datafile);
        
        // Add file fileMetadata to the new datafile
        datafile.getFileMetadatas().add(fileMetadata);

        // Add file fileMetadata to the datasetVersion
        if (this.datasetVersion.getFileMetadatas() == null) {
            this.datasetVersion.setFileMetadatas(new ArrayList());
        }
        this.datasetVersion.getFileMetadatas().add(fileMetadata);

        // Add the datasetVersion to the fileMetadata
        fileMetadata.setDatasetVersion(this.datasetVersion);
        
        // Add the data file to the datasetVersion
        this.datasetVersion.getDataset().getFiles().add(datafile);

        // Generate Storage Identifier
        fileService.generateStorageIdentifier(datafile);
        
        return datafile;
/*
        BufferedOutputStream outputStream = null;

            // Once again, at this point we are dealing with *temp*
            // files only; these are always stored on the local filesystem, 
            // so we are using FileInput/Output Streams to read and write
            // these directly, instead of going through the Data Access 
            // framework. 
            //      -- L.A.

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(getFilesTempDirectory() + "/" + datafile.getFileSystemName()));

            byte[] dataBuffer = new byte[8192];
            int i = 0;

            while ((i = inputStream.read(dataBuffer)) > 0) {
                outputStream.write(dataBuffer, 0, i);
                outputStream.flush();
            }
        } catch (IOException ioex) {
            datafile = null; 
        } finally {
            try {
                outputStream.close();
            } catch (IOException ioex) {}
        }

        
        // End: Read in the actual file contents
        
        return datafile;
        */
    };
    
}
