/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
//import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
//import jakarta.ejb.EJB;

/**
 *  Used by the IngestServiceBean to redistribute a zipped Shapefile*
 * 
 *  - Accomplish the actions described in the ShapefileHandler function "rezipShapefileSets"
 *  - Return a list of DataFile objects
 * 
 * @author raprasad
 */
public class IngestServiceShapefileHelper {
    
    //@EJB
    //DataFileServiceBean fileService; 

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
   public IngestServiceShapefileHelper(File zippedShapefile, File rezipFolder){
        
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
    /*
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
    */
    
    
}
