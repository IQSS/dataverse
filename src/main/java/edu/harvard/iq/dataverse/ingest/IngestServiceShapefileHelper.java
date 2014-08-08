/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *  Used by the IngestServiceBean to redistribute a zipped Shapefile*
 * 
 *  - Accomplish the actions described in the ShapefileHandler function "rezipShapefileSets"
 *  - Return a list of DataFile objects
 * 
 * @author rmp553
 */
public class IngestServiceShapefileHelper {
    
    /*
        code to add to IngestServiceBean
            File rezipFolder = this.getShapefileUnzipTempDirectory();
            IngestServiceShapefileHelper shapefileIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);
            List<DataFile> dataFiles = shapefileIngestHelper.getDataFiles();
            rezipFolder.delete();
     */
    
    private static final Logger logger = Logger.getLogger(IngestServiceShapefileHelper.class.getCanonicalName());

    private ShapefileHandler shpHandler;

    IngestServiceShapefileHelper(File toFile, File rezipFolder) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public void IngestServiceShapefileHelper(File zippedShapefile, File rezipFolder){
        
        if ((!isValidFile(zippedShapefile))||(!isValidFolder(rezipFolder))){
            return;
        }
        this.processFile(zippedShapefile, rezipFolder);

    }
    
    private boolean processFile(File zippedShapefile, File rezipFolder){
         if ((!isValidFile(zippedShapefile))||(!isValidFolder(rezipFolder))){
            return false;
        }

        
        FileInputStream shp_file_input_stream;
        try {
            shp_file_input_stream = new FileInputStream(zippedShapefile);
        } catch (FileNotFoundException ex) {
            logger.severe("Failed to convert File to a FileInputStream");
            return false;
        }
        
        this.shpHandler = new ShapefileHandler(shp_file_input_stream);
        if (!shpHandler.containsShapefile()){
            logger.severe("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");

            return false;
        }
        
        boolean rezip_success;
        try {
            rezip_success = shpHandler.rezipShapefileSets(shp_file_input_stream, rezipFolder);
        } catch (IOException ex) {
            logger.severe("Shapefile was not correctly unpacked/repacked");
            return false;
        }
        
        // Create DataFiles
        
     
        
        return true;
}
 /*  
    private List<DataFile> scratch_code(File tempFile){
        
           ZipInputStream unZippedIn = null; 
            ZipEntry zipEntry = null; 
            try {
                unZippedIn = new ZipInputStream(new FileInputStream(tempFile));

                if (unZippedIn == null) {
                    return null;
                }
                while ((zipEntry = unZippedIn.getNextEntry()) != null) {
                    // Note that some zip entries may be directories - we 
                    // simply skip them:
                    
                    if (!zipEntry.isDirectory()) {

                        String fileEntryName = zipEntry.getName();

                        if (fileEntryName != null && !fileEntryName.equals("")) {

                            fileEntryName = fileEntryName.replaceFirst("^.*[\\/]", "");

                            // Check if it's a "fake" file - a zip archive entry 
                            // created for a MacOS X filesystem element: (these 
                            // start with "._")
                            if (!fileEntryName.startsWith("._")) {
                                // OK, this seems like an OK file entry - we'll try 
                                // to read it and create a DataFile with it:

                                DataFile datafile = createSingleDataFile(version, unZippedIn, fileEntryName, "application/octet-stream");
                                // TODO: 
                                // Need to try to identify mime types for the individual 
                                // files inside the ZIP archive! -- L.A.
                                
                                if (datafile != null) {
                                    datafiles.add(datafile);
                                }
                            }
                        }
                    }
                    unZippedIn.closeEntry(); 
                }
                
            } catch (IOException ioex) {
                // just clear the datafiles list and let 
                // ingest default to creating a single DataFile out
                // of the unzipped file. 
                datafiles.clear();
            } finally {
                if (unZippedIn != null) {
                    try {unZippedIn.close();} catch (Exception zEx) {}
                }
            }
            if (datafiles.size() > 0) {
                return datafiles;
            }
            return null;
    */
    
}
