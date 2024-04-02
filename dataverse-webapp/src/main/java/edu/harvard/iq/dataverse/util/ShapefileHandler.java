package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.common.files.mime.ShapefileMimeType;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Used to identify, "repackage", and extract data from Shapefiles in .zip format
 * <p>
 * (1) Identify if a .zip contains a shapefile:
 * boolean containsShapefile()
 * <p>
 * <p>
 * <p>
 * (2) Unpack/"Repackage" .zip:
 * (a) All files extracted
 * (b) Each group of files that make up a shapefile are made into individual .zip files
 * (c) Non shapefile-related files left on their own
 * <p>
 * If the original .zip contains:  "shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.ain",  "shape1.aih",
 * "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",
 * "shape1.pdf", "README.md", "shape_notes.txt"
 * The repackaging results in a folder containing:
 * "shape1.zip",
 * "shape2.zip",
 * "shape1.pdf", "README.md", "shape_notes.txt"
 * <p>
 * Code Example:
 * <pre>{@code
 * try {
 *   ShapefileHandler shp_handler = new ShapefileHandler(new File("zipped_shapefile.zip"));
 *   if (shp_handler.containsShapefile()){
 *     File rezip_folder = new File("~/folder_for_rezipping");
 *     List<File> reZippedFiles = shp_handler.reZipShapefileSets(rezip_folder);
 *     // ...
 *   }
 * } catch(Exception e) {
 *   System.out.println(e.getMessage());
 * }
 * }</pre>
 *
 * @author raprasad
 */
public class ShapefileHandler {

    private static final Logger logger = Logger.getLogger(ShapefileHandler.class.getCanonicalName());

    // Reference for these extensions: http://en.wikipedia.org/wiki/Shapefile
    public final static String SHAPEFILE_FILE_TYPE = ShapefileMimeType.SHAPEFILE_FILE_TYPE.getMimeValue();
    public final static List<String> SHAPEFILE_MANDATORY_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj");
    public final static String SHP_XML_EXTENSION = "shp.xml";
    public final static String BLANK_EXTENSION = "__PLACEHOLDER-FOR-BLANK-EXTENSION__";
    public final static List<String> SHAPEFILE_ALL_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj", "sbn", "sbx", "fbn", "fbx", "ain", "aih", "ixs", "mxs", "atx", "cpg", SHP_XML_EXTENSION);
    private final File zipfile;

    /**
     * Hash of file basenames and a list of extensions.
     * e.g.  { "subway_shapefile" : [ ".dbf", ".prj", ".sbn", ".sbx", ".shp", ".shx"],
     *         "shapefile_info" : [".docx"],
     *,        "README" : ["md"],
     *         "Notes" : [""]
     * }
     */
    private final Map<String, List<String>> baseNameExtensions = new HashMap<>();

    // -------------------- CONSTRUCTOR --------------------

    public ShapefileHandler(File zipFile) {
        this.zipfile = zipFile;

        examineZipFile();
    }

    // -------------------- GETTERS --------------------

    Map<String, List<String>> getBaseNameExtensions() {
        return this.baseNameExtensions;
    }

    // -------------------- LOGIC --------------------

    /**
     * Re-group the shapefile(s) into a given directory.
     *
     * Creates to subdirectories:
     * - unzipped: directory into which the zip-file is extracted
     * - rezipped: contains the resulting re-pack
     *
     * @return List of resulting files after re-packaging.
     */
    public List<File> reZipShapefileSets(File unzipDirectory, File reZipDirectory) throws IOException {
        logger.fine("rezipShapefileSets");

        if (!containsShapefile()) {
            throw new IllegalArgumentException("No shapefiles in zip");
        }

        verifyDestinationDirectories(unzipDirectory, reZipDirectory);
        try {
            // Unzip files!
            unzipFilesToDirectory(unzipDirectory.toPath());

            // Redistribute files!
            redistributeFilesFromZip(unzipDirectory.toPath(), reZipDirectory.toPath());

            return Optional.ofNullable(reZipDirectory.listFiles()).map(Arrays::asList).orElse(Collections.emptyList());
        } finally {
            logger.fine(() -> "Post redistribute, unzipped files:" + Optional.ofNullable(unzipDirectory.listFiles())
                    .map(Arrays::stream).orElse(Stream.empty()).map(File::getName)
                    .collect(Collectors.joining(",")));
        }
    }

    /**
     * Does this zip file contain a shapefile set?
     */
    public boolean containsShapefile() {
        for (Map.Entry<String, List<String>> entry : baseNameExtensions.entrySet()) {
            List<String> extenstionList = entry.getValue();
            if (doesListContainShapefileExtensions(extenstionList)) {
                return true;
            }
        }

        return false;
    }

    // -------------------- PRIVATE --------------------

    private void verifyDestinationDirectories(File... directories) throws IOException {
        for(File dir : directories) {
            if (dir == null || !dir.isDirectory() || !FileUtils.isEmptyDirectory(dir)) {
                throw new IllegalArgumentException("Invalid target directory:" + dir);
            }
        }
    } // createDirectories

    private String getFileBasename(String fileName) {
        if (fileName == null) {
            return null;
        }
        String unzipFileName = new File(fileName).getName();
        if (unzipFileName.isEmpty()) {
            logger.info("getFileBasename.  fileName is an empty string: " + fileName);
            return null;
        }
        return unzipFileName;
    }

    /**
     * Unzip the files to the directory, FLATTENING the directory structure
     */
    private void unzipFilesToDirectory(Path unzipDirectory) {
        try(ZipArchiveInputStream zipStream = new ZipArchiveInputStream(Files.newInputStream(zipfile.toPath()))) {
            ZipEntry origEntry;
            while ((origEntry = zipStream.getNextEntry()) != null) {
                String zentryFileName = origEntry.getName();
                String unzipFileName = getFileBasename(zentryFileName);

                if (isFileToSkip(unzipFileName)) {
                    logger.fine("Skip file");
                    continue;
                }

                // Create sub-directory, if needed
                if (origEntry.isDirectory()) {
                    logger.fine("Skip directory");
                    continue; // Continue to next Entry
                }

                logger.fine("file found!");

                // Write the file
                Path outpath = unzipDirectory.resolve(unzipFileName);
                logger.fine("Write zip file: " + outpath);
                try (OutputStream fileOutputStream = Files.newOutputStream(outpath)) {
                    IOUtils.copy(zipStream, fileOutputStream);
                }
            } // end outer while
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to open ZipInputStream entry", ex);
            throw new IllegalStateException("Failed to unzip:" + ex.getMessage());
        }
    }

    private Path getFilePath(Path directory, String file_basename, String file_ext) {
        if (file_ext.equals(BLANK_EXTENSION)) {
            return directory.resolve(file_basename);
        }
        return directory.resolve(file_basename + "." + file_ext);
    }

    /**
     * Create new zipped shapefile
     */
    private void redistributeFilesFromZip(Path unzipDirectory, Path rezipDirectory) throws IOException {
        logger.fine("redistributeFilesFromZip. source: '" + unzipDirectory + "'  target: '" + rezipDirectory + "'");

        int cnt = 0;
        /* START: Redistribute files by iterating through the Map of basenames + extensions
           example key: "shape1"
           example ext_list: ["shp", "shx", "dbf", "prj"]
        */
        for (Map.Entry<String, List<String>> entry : baseNameExtensions.entrySet()) {
            cnt++;
            String baseName = entry.getKey();
            List<String> ext_list = entry.getValue();

            logger.fine("\n(" + cnt + ") Basename: " + baseName);
            logger.fine("Extensions: " + Arrays.toString(ext_list.toArray()));

            // Is this a shapefile?  If so, rezip it
            if (doesListContainShapefileExtensions(ext_list)) {
                Path reZippedFileName = rezipDirectory.resolve(baseName + ".zip");
                try (ZipFileBuilder shapefileZip = new ZipFileBuilder(reZippedFileName)) {
                    for (String ext_name : ext_list) {
                        Path sourceFile = getFilePath(unzipDirectory, baseName, ext_name);
                        if (!isShapefileExtension(ext_name)) {
                            // Another file with similar basename as shapefile.
                            // e.g. if shapefile basename is "census", this might be "census.xls", "census.pdf", or another non-shapefile extension
                            moveFile(sourceFile, getFilePath(rezipDirectory, baseName, ext_name));
                        } else {
                            shapefileZip.addToZipFile(sourceFile);
                            Files.delete(sourceFile);
                        }
                    }
                }
                // rezip it
            } else {
                // Non-shapefiles
                for (String ext_name : ext_list) {
                    moveFile(getFilePath(unzipDirectory, baseName, ext_name),
                            getFilePath(rezipDirectory, baseName, ext_name));
                }
            }
        }

    }  // end: redistributeFilesFromZip

    private void moveFile(Path sourceFileName, Path targetFileName) {
        try {
            Files.move(sourceFileName, targetFileName);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to move file. Source: " + sourceFileName + " Target: " + targetFileName, ex);
        }
    }

    private boolean isShapefileExtension(String ext_name) {
        if (ext_name == null) {
            return false;
        }
        return SHAPEFILE_ALL_EXTENSIONS.contains(ext_name);
    }

    /**
     * Does a list of file extensions match those required for a shapefile set?
     */
    private boolean doesListContainShapefileExtensions(List<String> ext_list) {
        return new HashSet<>(ext_list).containsAll(SHAPEFILE_MANDATORY_EXTENSIONS);
    }

    private void addToFileGroupHash(String basename, String ext) {
        if ((basename == null) || (ext == null)) {
            return;
        }
        List<String> extension_list = baseNameExtensions.computeIfAbsent(basename, k -> new ArrayList<>());
        if (!(extension_list.contains(ext))) {
            extension_list.add(ext);
        }
    }   // end addToFileGroupHash

    /**
     * Update the fileGroup hash which contains a { base_filename : [ext1, ext2, etc ]}
     * This is used to determine whether a .zip contains a shapefile set
     * #
     *
     * @param fname filename in String format
     */
    private void updateFileGroupHash(String fname) {
        if (fname == null) {
            return;
        }

        // Split filename into basename and extension.  No extension yields only basename
        //
        if (fname.toLowerCase().endsWith(SHP_XML_EXTENSION)) {
            int idx = fname.toLowerCase().indexOf("." + SHP_XML_EXTENSION);
            if (idx >= 1) {   // if idx==0, then the file name is ".shp.xml""
                String basename = fname.substring(0, idx);
                String ext = fname.substring(idx + 1);
                addToFileGroupHash(basename, ext);
                return;
            }
        }

        String[] tokens = fname.split("\\.(?=[^\\.]+$)");
        if (tokens.length == 1) {
            addToFileGroupHash(tokens[0], BLANK_EXTENSION);      // file basename, no extension

        } else if (tokens.length == 2) {
            addToFileGroupHash(tokens[0], tokens[1]);  // file basename, extension
        }
    } // end updateFileGroupHash

    private boolean isFileToSkip(String fname) {
        if ((fname == null) || (fname.equals(""))) {
            return true;
        }

        if (fname.startsWith("__")) {
            return true;
        }

        if (fname.startsWith("._")) {
            return true;
        }

        return fname.endsWith(".DS_Store");
    }

    /**
     * Iterate through the zip file contents.
     * Does it contain any shapefiles?
     */
    private void examineZipFile() {
        if (zipfile == null || !zipfile.isFile()) {
            throw new IllegalArgumentException("Invalid zip file: " + zipfile);
        }

        try (ZipFile zipFile = ZipFile.builder().setFile(zipfile).get()) {
            Set<String> fileNamesInZip = new HashSet<>();
            Enumeration<ZipArchiveEntry> zipFileEntries = zipFile.getEntries();
            while(zipFileEntries.hasMoreElements()) {
                ZipArchiveEntry zipFileEntry = zipFileEntries.nextElement();
                String unzipFileName = getFileBasename(zipFileEntry.getName());

                if (isFileToSkip(unzipFileName) || zipFileEntry.isDirectory()) {
                    continue;
                }
                
                if (fileNamesInZip.contains(unzipFileName)) {
                    throw new IllegalStateException("Found file-name collision: " + unzipFileName);
                }
                fileNamesInZip.add(unzipFileName);
                updateFileGroupHash(unzipFileName);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Error inspecting zip file", ex);
        }
    }
} // end ShapefileHandler