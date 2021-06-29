package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.util.ShapefileHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;


/**
 * Used by the IngestServiceBean to redistribute a zipped Shapefile*
 * <p>
 * - Accomplish the actions described in the ShapefileHandler function "rezipShapefileSets"
 * - Return a list of DataFile objects
 *
 * @author raprasad
 */
public class IngestServiceShapefileHelper {

    private static final Logger logger = Logger.getLogger(IngestServiceShapefileHelper.class.getCanonicalName());

    private ShapefileHandler shpHandler;
    private File zippedShapefile;
    private File rezipFolder;

    private boolean isValidFile(File fileObject) {

        if (fileObject == null) {
            logger.warning("fileObject was null");
            return false;
        }
        if (!fileObject.isFile()) {
            logger.warning("fileObject was not a file.  Failed \"isFile()\": " + fileObject.getAbsolutePath());
            return false;
        }
        return true;
    }


    private boolean isValidFolder(File fileObject) {

        if (fileObject == null) {
            logger.warning("fileObject was null");
            return false;
        }
        if (!fileObject.isDirectory()) {
            logger.warning("fileObject was not a directory.  Failed \"isFile()\": " + fileObject.getAbsolutePath());
            return false;
        }
        return true;
    }

    /*
        Constructor that accepts a file object
    */
    public IngestServiceShapefileHelper(File zippedShapefile, File rezipFolder) {

        if ((!isValidFile(zippedShapefile)) || (!isValidFolder(rezipFolder))) {
            return;
        }
        this.zippedShapefile = zippedShapefile;
        this.rezipFolder = rezipFolder;
        //this.datasetVersion = version;

        //this.processFile(zippedShapefile, rezipFolder);

    }

    public boolean processFile() {

        if ((!isValidFile(this.zippedShapefile)) || (!isValidFolder(this.rezipFolder))) {
            return false;
        }

        // (1) Use the ShapefileHandler to the .zip for a shapefile
        //
        this.shpHandler = new ShapefileHandler(zippedShapefile);
        if (!shpHandler.containsShapefile()) {
            logger.severe("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");
            return false;
        }

        //  (2) Rezip the shapefile pieces
        logger.info("rezipFolder: " + rezipFolder.getAbsolutePath());
        boolean rezipSuccess;
        try {
            rezipSuccess = shpHandler.rezipShapefileSets(rezipFolder);
        } catch (IOException ex) {
            logger.severe("Shapefile was not correctly unpacked/repacked");
            logger.severe("shpHandler message: " + shpHandler.errorMessage);
            return false;
        }

        return rezipSuccess;

    }


    public List<File> getFinalRezippedFiles() {
        if (this.shpHandler == null) {
            return null;
        }
        return this.shpHandler.getFinalRezippedFiles();
    }

}
