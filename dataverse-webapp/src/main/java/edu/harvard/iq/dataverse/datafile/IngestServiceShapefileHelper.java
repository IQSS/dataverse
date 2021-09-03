package edu.harvard.iq.dataverse.datafile;

import com.google.common.base.Preconditions;
import edu.harvard.iq.dataverse.util.ShapefileHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private File zippedShapefile;
    private File rezipFolderBase;

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
    public IngestServiceShapefileHelper(File zippedShapefile, File rezipFolderBase) {
        Preconditions.checkArgument(isValidFile(zippedShapefile));
        Preconditions.checkArgument(isValidFolder(rezipFolderBase));

        this.zippedShapefile = zippedShapefile;
        this.rezipFolderBase = rezipFolderBase;

    }

    public List<File> processFile() {

        // (1) Use the ShapefileHandler to the .zip for a shapefile
        //
        ShapefileHandler shpHandler = new ShapefileHandler(zippedShapefile);
        if (!shpHandler.containsShapefile()) {
            logger.severe("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");
            throw new IllegalStateException("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");
        }

        //  (2) Rezip the shapefile pieces
        File rezipFolder = getShapefileUnzipTempDirectory(rezipFolderBase);
        logger.info("rezipFolder: " + rezipFolderBase.getAbsolutePath());
        boolean rezipSuccess;
        try {
            rezipSuccess = shpHandler.rezipShapefileSets(rezipFolder);
        } catch (IOException ex) {
            logger.severe("Shapefile was not correctly unpacked/repacked");
            logger.severe("shpHandler message: " + shpHandler.errorMessage);
            throw new IllegalStateException("Shapefile was not correctly unpacked/repacked: " + shpHandler.errorMessage, ex);
        }

        if (!rezipSuccess) {
            throw new IllegalStateException("Shapefile was not correctly unpacked/repacked: " + shpHandler.errorMessage);
        }
        
        return shpHandler.getFinalRezippedFiles();

    }

    private static File getShapefileUnzipTempDirectory(File tempDirectoryBase) {

        String datestampedFileName = "shp_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());

        File datestampedFolder = new File(tempDirectoryBase, datestampedFileName);
        if (!datestampedFolder.isDirectory()) {
            /* Note that "createDirectories()" must be used - not
             * "createDirectory()", to make sure all the parent
             * directories that may not yet exist are created as well.
             */
            try {
                Files.createDirectories(datestampedFolder.toPath());
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to create temp. directory to unzip shapefile: " + datestampedFolder.toString(), ex);
            }
        }
        return datestampedFolder;
    }
}
