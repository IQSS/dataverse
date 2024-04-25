package edu.harvard.iq.dataverse.datafile;

import com.google.common.base.Preconditions;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Used by the IngestServiceBean to redistribute a zipped Shapefile*
 * <p>
 * - Accomplish the actions described in the ShapefileHandler function "rezipShapefileSets"
 * - Return a list of DataFile objects
 *
 * @author raprasad
 */
public class IngestServiceShapefileHelper implements Closeable {

    private static final Logger logger = Logger.getLogger(IngestServiceShapefileHelper.class.getCanonicalName());

    private final File zippedShapefile;
    private final File reZipFolder;
    private final File unZipFolder;
    private final Long fileSizeLimit;
    private final Long zipFileUnpackFilesLimit;

    // -------------------- CONSTRUCTOR --------------------

    /**
     * Constructor that accepts a file object
     */
    public IngestServiceShapefileHelper(File zippedShapefile, File workingFolderBase, Long fileSizeLimit, Long zipFileUnpackFilesLimit) {
        Preconditions.checkArgument(isValidFile(zippedShapefile));
        Preconditions.checkArgument(isValidFolder(workingFolderBase));

        this.zippedShapefile = zippedShapefile;
        String id = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());
        this.reZipFolder = getShapefileUnzipTempDirectory(workingFolderBase, "shp_" + id + "_rezip");
        this.unZipFolder = getShapefileUnzipTempDirectory(workingFolderBase, "shp_" + id + "_unzip");
        this.fileSizeLimit = fileSizeLimit;
        this.zipFileUnpackFilesLimit = zipFileUnpackFilesLimit;

    }

    // -------------------- LOGIC --------------------

    public List<File> processFile() {
        try {
            // (1) Use the ShapefileHandler to the .zip for a shapefile
            //
            ShapefileHandler shpHandler = new ShapefileHandler(zippedShapefile, fileSizeLimit, zipFileUnpackFilesLimit);
            if (!shpHandler.containsShapefile()) {
                logger.severe("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");
                throw new IllegalStateException("Shapefile was incorrectly detected upon Ingest (FileUtil) and passed here");
            }

            //  (2) Rezip the shapefile pieces
            return shpHandler.reZipShapefileSets(unZipFolder, reZipFolder);
        } catch (Exception ex) {
            throw new IllegalStateException("Shapefile was not correctly unpacked/repacked", ex);
        }
    }

    @Override
    public void close() throws IOException {
        deleteDirectory(unZipFolder);
        deleteDirectory(reZipFolder);
    }

    // -------------------- PRIVATE --------------------

    private void deleteDirectory(File directory) {
        Try.run(() -> FileUtils.deleteDirectory(directory))
                .onFailure(ex -> logger.log(Level.SEVERE, "Error cleaning shapefile working directory:" + directory, ex));
    }

    private static File getShapefileUnzipTempDirectory(File tempDirectoryBase, String directoryName) {
        File datestampedFolder = new File(tempDirectoryBase, directoryName);
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
}
