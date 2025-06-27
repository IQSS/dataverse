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

package edu.harvard.iq.dataverse.util;


import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.S3AccessIO;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;

import static edu.harvard.iq.dataverse.api.ApiConstants.DS_VERSION_DRAFT;
import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;
import edu.harvard.iq.dataverse.ingest.IngestReport;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceShapefileHelper;
import edu.harvard.iq.dataverse.ingest.IngestableDataChecker;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.settings.ConfigCheckService;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.file.BagItFileHandler;
import edu.harvard.iq.dataverse.util.file.CreateDataFileResult;
import edu.harvard.iq.dataverse.util.file.BagItFileHandlerFactory;
import edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatDoc;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.HTML_H1;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.HTML_TABLE_HDR;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatTitle;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatTable;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatTableCell;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatLink;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatTableCellAlignRight;
import static edu.harvard.iq.dataverse.util.xml.html.HtmlFormatUtil.formatTableRow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.ejb.EJBException;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FilenameUtils;

import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.util.file.FileExceedsStorageQuotaException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation, 
 * but the old code is ported creatively on the method-by-method basis.
 * 
 * @author Leonid Andreev
 */
public class FileUtil implements java.io.Serializable  {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getCanonicalName());
    
    private static final String[] TABULAR_DATA_FORMAT_SET = {"POR", "SAV", "DTA", "RDA"};
    
    private static Map<String, String> STATISTICAL_FILE_EXTENSION = new HashMap<String, String>();
    
    /*
     * The following are Stata, SAS and SPSS syntax/control cards: 
     * These are recognized as text files (because they are!) so 
     * we check all the uploaded "text/plain" files for these extensions, and 
     * assign the following types when they are matched;
     * Note that these types are only used in the metadata displayed on the 
     * dataset page. We don't support ingest on control cards. 
     * -- L.A. 4.0 Oct. 2014
    */
    
    static {
        STATISTICAL_FILE_EXTENSION.put("do",  "application/x-stata-syntax");
        STATISTICAL_FILE_EXTENSION.put("sas", "application/x-sas-syntax");
        STATISTICAL_FILE_EXTENSION.put("sps", "application/x-spss-syntax");
        STATISTICAL_FILE_EXTENSION.put("csv", "text/csv");
        STATISTICAL_FILE_EXTENSION.put("tsv", "text/tsv");
    }
    
    private static MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();
    
    public static final String MIME_TYPE_STATA   = "application/x-stata";
    public static final String MIME_TYPE_STATA13 = "application/x-stata-13";
    public static final String MIME_TYPE_STATA14 = "application/x-stata-14";
    public static final String MIME_TYPE_STATA15 = "application/x-stata-15";
    public static final String MIME_TYPE_RDATA   = "application/x-rlang-transport";
    
    public static final String MIME_TYPE_CSV     = "text/csv";
    public static final String MIME_TYPE_CSV_ALT = "text/comma-separated-values";
    public static final String MIME_TYPE_TSV     = "text/tsv";
    public static final String MIME_TYPE_TSV_ALT = "text/tab-separated-values";
    public static final String MIME_TYPE_XLSX    = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    
    public static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    public static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    
    
    public static final String MIME_TYPE_FITS  = "application/fits";
    
    public static final String MIME_TYPE_ZIP   = "application/zip";
    
    public static final String MIME_TYPE_FITSIMAGE = "image/fits";
    // SHAPE file type: 
    // this is the only supported file type in the GEO DATA class:
    
    public static final String MIME_TYPE_GEO_SHAPE = "application/zipped-shapefile";
    
    public static final String MIME_TYPE_UNDETERMINED_DEFAULT = "application/octet-stream";
    public static final String MIME_TYPE_UNDETERMINED_BINARY = "application/binary";
    
    public static final String SAVED_ORIGINAL_FILENAME_EXTENSION = "orig";
    
    //Todo - this is the same as MIME_TYPE_TSV_ALT
    public static final String MIME_TYPE_INGESTED_FILE = "text/tab-separated-values";

    public static final String MIME_TYPE_NETCDF = "application/netcdf";
    public static final String MIME_TYPE_XNETCDF = "application/x-netcdf";
    public static final String MIME_TYPE_HDF5 = "application/x-hdf5";
    public static final String MIME_TYPE_RO_CRATE = "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#flattened http://www.w3.org/ns/json-ld#compacted https://w3id.org/ro/crate\"";

    // File type "thumbnail classes" tags:
    
    public static final String FILE_THUMBNAIL_CLASS_AUDIO = "audio";
    public static final String FILE_THUMBNAIL_CLASS_CODE = "code";
    public static final String FILE_THUMBNAIL_CLASS_DOCUMENT = "document";
    public static final String FILE_THUMBNAIL_CLASS_ASTRO = "astro";
    public static final String FILE_THUMBNAIL_CLASS_IMAGE = "image";
    public static final String FILE_THUMBNAIL_CLASS_NETWORK = "network";
    public static final String FILE_THUMBNAIL_CLASS_GEOSHAPE = "geodata";
    public static final String FILE_THUMBNAIL_CLASS_TABULAR = "tabular";
    public static final String FILE_THUMBNAIL_CLASS_VIDEO = "video";
    public static final String FILE_THUMBNAIL_CLASS_PACKAGE = "package";
    public static final String FILE_THUMBNAIL_CLASS_OTHER = "other";
    
    // File type facets, as returned by the getFacetFileType() method in this utility: 
    
    private static final String FILE_FACET_CLASS_ARCHIVE = "Archive";
    private static final String FILE_FACET_CLASS_AUDIO = "Audio";
    private static final String FILE_FACET_CLASS_CODE = "Code";
    private static final String FILE_FACET_CLASS_DATA = "Data";
    private static final String FILE_FACET_CLASS_DOCUMENT = "Document";
    private static final String FILE_FACET_CLASS_ASTRO = "FITS";
    private static final String FILE_FACET_CLASS_IMAGE = "Image";
    private static final String FILE_FACET_CLASS_NETWORK = "Network Data";
    private static final String FILE_FACET_CLASS_GEOSHAPE = "Shape";
    private static final String FILE_FACET_CLASS_TABULAR = "Tabular Data";
    private static final String FILE_FACET_CLASS_VIDEO = "Video";
    private static final String FILE_FACET_CLASS_TEXT = "Text";
    private static final String FILE_FACET_CLASS_OTHER = "Other";
    private static final String FILE_FACET_CLASS_UNKNOWN = "Unknown";

    // The file type facets and type-specific thumbnail classes (above) are
    // very similar, but not exactly 1:1; so the following map is for 
    // maintaining the relationship between the two:
    
    public static Map<String, String> FILE_THUMBNAIL_CLASSES = new HashMap<String, String>();
    
    static {
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_VIDEO, FILE_THUMBNAIL_CLASS_VIDEO);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_AUDIO, FILE_THUMBNAIL_CLASS_AUDIO);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_CODE, FILE_THUMBNAIL_CLASS_CODE);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_DATA, FILE_THUMBNAIL_CLASS_TABULAR);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_NETWORK, FILE_THUMBNAIL_CLASS_NETWORK);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_ASTRO, FILE_THUMBNAIL_CLASS_ASTRO);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_IMAGE, FILE_THUMBNAIL_CLASS_IMAGE);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_DOCUMENT, FILE_THUMBNAIL_CLASS_DOCUMENT);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_GEOSHAPE, FILE_THUMBNAIL_CLASS_GEOSHAPE);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_TABULAR, FILE_THUMBNAIL_CLASS_TABULAR);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_TEXT, FILE_THUMBNAIL_CLASS_DOCUMENT);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_OTHER, FILE_THUMBNAIL_CLASS_OTHER);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_UNKNOWN, FILE_THUMBNAIL_CLASS_OTHER);
        FILE_THUMBNAIL_CLASSES.put(FILE_FACET_CLASS_ARCHIVE, FILE_THUMBNAIL_CLASS_PACKAGE);
    }
    
    private static final String FILE_LIST_DATE_FORMAT = "d-MMMM-yyyy HH:mm";

    /**
     * This string can be prepended to a Base64-encoded representation of a PNG
     * file in order to imbed an image directly into an HTML page using the
     * "img" tag. See also https://en.wikipedia.org/wiki/Data_URI_scheme
     */
    public static String DATA_URI_SCHEME = "data:image/png;base64,";

    public FileUtil() {
    }
    
   
    public static String getFileExtension(String fileName){
        String ext = null;
        if ( fileName.lastIndexOf(".") != -1){
            ext = (fileName.substring( fileName.lastIndexOf(".") + 1 )).toLowerCase();
        }
        return ext;
    } 

    public static String replaceExtension(String originalName) {
       return replaceExtension(originalName, "tab");
    }   
    
    public static String replaceExtension(String originalName, String newExtension) {
        int extensionIndex = originalName.lastIndexOf(".");
        if (extensionIndex != -1 ) {
            return originalName.substring(0, extensionIndex) + "."+newExtension ;
        } else {
            return originalName +"."+newExtension ;
        }
    }
    
    public static String getUserFriendlyFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();
         
        if (fileType != null) {
            if (fileType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)){
                return ShapefileHandler.SHAPEFILE_FILE_TYPE_FRIENDLY_NAME;
            }
            try {
                return BundleUtil.getStringFromPropertyFile(fileType,"MimeTypeDisplay" );
            } catch (MissingResourceException e) {
                //NOOP: we will try again after trimming ";"
            }
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }
            try {
                return BundleUtil.getStringFromPropertyFile(fileType,"MimeTypeDisplay" );
            } catch (MissingResourceException e) {
                return fileType;
            }
        }

        return fileType;
    }

    public static String getIndexableFacetFileType(DataFile dataFile) {
        try {
            return BundleUtil.getStringFromDefaultPropertyFile(dataFile.getContentType(),"MimeTypeFacets" );
        } catch (MissingResourceException e) {
            //NOOP: we will try again after trimming ";"
        }
        String fileType = getFileType(dataFile);
        try {
            return BundleUtil.getStringFromDefaultPropertyFile(fileType,"MimeTypeFacets"  );
        } catch (MissingResourceException ex) {
            // if there's no defined "facet-friendly" form of this mime type
            // we'll truncate the available type by "/", e.g., all the
            // unknown image/* types will become "image".
            // Since many other, quite different types would then all become
            // "application" - we will use the facet "Other" for all the
            // application/* types not specifically defined in the properties file.
            //
            // UPDATE, MH 4.9.2
            // Since production is displaying both "tabulardata" and "Tabular Data"
            // we are going to try to add capitalization here to this function
            // in order to capitalize all the unknown types that are not called
            // out in MimeTypeFacets.properties

            if (!StringUtil.isEmpty(fileType)) {
                String typeClass = fileType.split("/")[0];
                if ("application".equalsIgnoreCase(typeClass)) {
                    return FILE_FACET_CLASS_OTHER;
                }

                return Character.toUpperCase(typeClass.charAt(0)) + typeClass.substring(1);
            } else {
                return null;
            }
        }
    }

    public static String getFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();

        if (!StringUtil.isEmpty(fileType)) {
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }
            return fileType;
        } else {
            return "application/octet-stream";
        }

    }

    public static String getFacetFileType(DataFile dataFile) {
        String fileType = getFileType(dataFile);
        try {
            return BundleUtil.getStringFromPropertyFile(fileType,"MimeTypeFacets"  );
        } catch (MissingResourceException ex) {
            // if there's no defined "facet-friendly" form of this mime type
            // we'll truncate the available type by "/", e.g., all the
            // unknown image/* types will become "image".
            // Since many other, quite different types would then all become
            // "application" - we will use the facet "Other" for all the
            // application/* types not specifically defined in the properties file.
            //
            // UPDATE, MH 4.9.2
            // Since production is displaying both "tabulardata" and "Tabular Data"
            // we are going to try to add capitalization here to this function
            // in order to capitalize all the unknown types that are not called
            // out in MimeTypeFacets.properties

            if (!StringUtil.isEmpty(fileType)) {
                String typeClass = fileType.split("/")[0];
                if ("application".equalsIgnoreCase(typeClass)) {
                    return FILE_FACET_CLASS_OTHER;
                }

                return Character.toUpperCase(typeClass.charAt(0)) + typeClass.substring(1);
            }
            else
            {
                return  null;
            }
        }
    }
    
    public static String getUserFriendlyOriginalType(DataFile dataFile) {
        if (!dataFile.isTabularData()) {
            return null; 
        }
        
        String fileType = dataFile.getOriginalFileFormat();
         
        if (fileType != null && !fileType.equals("")) {
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }
            try {
                return BundleUtil.getStringFromPropertyFile(fileType,"MimeTypeDisplay" );
            } catch (MissingResourceException e) {
                return fileType;
            }
        } 
        
        return "UNKNOWN";
    }
    
    /**
     *  Returns a content type string for a FileObject
     * 
     */
    public static String determineContentType(File fileObject) {
        if (fileObject==null){
            return null;
        }
        String contentType;
        try {
            contentType = determineFileType(fileObject, fileObject.getName());
        } catch (Exception ex) {
            logger.warning("FileUtil.determineFileType failed for file with name: " + fileObject.getName());
            contentType = null;
        }

       if ((contentType==null)||(contentType.equals(""))){
            contentType = MIME_TYPE_UNDETERMINED_DEFAULT;
       }
       return contentType;
        
    }
    
    public static String retestIngestableFileType(File file, String fileType) {
        IngestableDataChecker tabChecker = new IngestableDataChecker(TABULAR_DATA_FORMAT_SET);
        String newType = tabChecker.detectTabularDataFormat(file);
        
        return newType != null ? newType : fileType;
    }
    
    public static String determineRemoteFileType(DataFile df, String fileName) {
        String fileType = determineFileTypeByNameAndExtension(fileName);

        if (!StringUtils.isBlank(fileType) && fileType.startsWith("application/x-stata")) {
            String driverId = DataAccess
                    .getStorageDriverFromIdentifier(df.getStorageIdentifier());
            if (StorageIO.isDataverseAccessible(driverId)) {
                try {
                    StorageIO<DataFile> storage = df.getStorageIO();
                    storage.open(DataAccessOption.READ_ACCESS);
                    try (InputStream is = storage.getInputStream()) {

                        // Read the first 42 bytes of the file to determine the file type
                        byte[] buffer = new byte[42];
                        is.read(buffer, 0, 42);
                        ByteBuffer bb = ByteBuffer.allocate(42);
                        bb.put(buffer);

                        // step 1:
                        // Apply our custom methods to try and recognize data files that can be
                        // converted to tabular data
                        logger.fine("Attempting to identify potential tabular data files;");
                        IngestableDataChecker tabChk = new IngestableDataChecker(new String[] { "DTA" });
                        fileType = tabChk.detectTabularDataFormat(bb);
                    } catch (IOException ex) {
                        logger.warning("Unable to getInputStream for storageIdentifier: " + df.getStorageIdentifier());
                    }
                } catch (IOException ex) {
                    logger.warning("Unable to open storageIO for storageIdentifier: " + df.getStorageIdentifier());
                }
            }
        }
        return fileType;

    }

    public static String determineFileType(File f, String fileName) throws IOException {
        String fileType = lookupFileTypeByFileName(fileName);
        if (fileType != null) {
            return fileType;
        }
        String fileExtension = getFileExtension(fileName);
        
        
        
        // step 1: 
        // Apply our custom methods to try and recognize data files that can be 
        // converted to tabular data, or can be parsed for extra metadata 
        // (such as FITS).
        logger.fine("Attempting to identify potential tabular data files;");
        IngestableDataChecker tabChk = new IngestableDataChecker(TABULAR_DATA_FORMAT_SET);
        
        fileType = tabChk.detectTabularDataFormat(f);
        
        logger.fine("determineFileType: tabular data checker found "+fileType);
                
        // step 2: If not found, check if graphml or FITS
        if (fileType==null) {
            if (isGraphMLFile(f))  {
                fileType = "text/xml-graphml";
            } else // Check for FITS:
            // our check is fairly weak (it appears to be hard to really
            // really recognize a FITS file without reading the entire 
            // stream...), so in version 3.* we used to nsist on *both* 
            // the ".fits" extension and the header check;
            // in 4.0, we'll accept either the extension, or the valid 
            // magic header:
            if (isFITSFile(f) || (fileExtension != null
                    && fileExtension.equalsIgnoreCase("fits"))) {
                fileType = "application/fits";
            }
        }

        // step 3a: Check if NetCDF or HDF5
        if (fileType == null) {
            fileType = checkNetcdfOrHdf5(f);
        }
       
        // step 3: check the mime type of this file with Jhove
        if (fileType == null){
            JhoveFileType jw = new JhoveFileType();
            String mimeType = jw.getFileMimeType(f);
            if (mimeType != null) {
                fileType = mimeType;
            }
        }
        
        // step 4: 
        // Additional processing; if we haven't gotten much useful information 
        // back from Jhove, we'll try and make an educated guess based on 
        // the file name and extension:

        if ( fileExtension != null) {
            logger.fine("fileExtension="+fileExtension);

            if (fileType == null || fileType.startsWith("text/plain") || "application/octet-stream".equals(fileType)) {
                if (fileType != null && fileType.startsWith("text/plain") && STATISTICAL_FILE_EXTENSION.containsKey(fileExtension)) {
                    fileType = STATISTICAL_FILE_EXTENSION.get(fileExtension);
                } else {
                    fileType = lookupFileTypeByExtension(fileName);
                }

                logger.fine("mime type recognized by extension: "+fileType);
            }
        } else {
            //ToDo - if the extension is null, how can this call do anything
            logger.fine("fileExtension is null");
            final String fileTypeByExtension = lookupFileTypeByExtensionFromPropertiesFile(fileName);
            if(!StringUtil.isEmpty(fileTypeByExtension)) {
                logger.fine(String.format("mime type: %s recognized by extension: %s", fileTypeByExtension, fileName));
                fileType = fileTypeByExtension;
            }
        }
        
        // step 5: 
        // if this is a compressed file - zip or gzip - we'll check the 
        // file(s) inside the compressed stream and see if it's one of our
        // recognized formats that we want to support compressed:

        if ("application/x-gzip".equals(fileType)) {
            logger.fine("we'll run additional checks on this gzipped file.");
            try (FileInputStream gzippedIn = new FileInputStream(f);
                     InputStream uncompressedIn = new GZIPInputStream(gzippedIn)) {
                if (isFITSFile(uncompressedIn)) {
                    fileType = "application/fits-gzipped";
                }
            } catch (IOException ioex) {
                logger.warning("IOException while processing gzipped FITS file: " + ioex.getMessage());
            }
        }
        if ("application/zip".equals(fileType)) {
            
            // Is this a zipped Shapefile?
            // Check for shapefile extensions as described here: http://en.wikipedia.org/wiki/Shapefile
            //logger.info("Checking for shapefile");

            ShapefileHandler shp_handler = new ShapefileHandler(f);
             if (shp_handler.containsShapefile()){
              //  logger.info("------- shapefile FOUND ----------");
                 fileType = ShapefileHandler.SHAPEFILE_FILE_TYPE; //"application/zipped-shapefile";
             }
             try {
                 Optional<BagItFileHandler> bagItFileHandler = CDI.current().select(BagItFileHandlerFactory.class).get().getBagItFileHandler();
                 if (bagItFileHandler.isPresent() && bagItFileHandler.get().isBagItPackage(fileName, f)) {
                     fileType = BagItFileHandler.FILE_TYPE;
                 }
             } catch (Exception e) {
                 logger.warning("Error checking for BagIt package: " + e.getMessage());
             }
        } 
        
        if(fileType==null) {
            fileType = MIME_TYPE_UNDETERMINED_DEFAULT;
        }
        logger.fine("returning fileType "+fileType);
        return fileType;
    }

    public static String determineFileTypeByNameAndExtension(final String fileName) {
        final String fileType = lookupFileTypeByFileName(fileName);
        if (fileType != null) {
            return fileType;
        }
        return lookupFileTypeByExtension(fileName);
    }

    private static String lookupFileTypeByExtension(final String fileName) {
        final String mimetypesFileTypeMapResult = MIME_TYPE_MAP.getContentType(fileName);
        logger.fine("MimetypesFileTypeMap type by extension, for " + fileName + ": " + mimetypesFileTypeMapResult);
        if (mimetypesFileTypeMapResult == null) {
            return null;
        }
        if ("application/octet-stream".equals(mimetypesFileTypeMapResult)) {
            return lookupFileTypeByExtensionFromPropertiesFile(fileName);
        }
        return mimetypesFileTypeMapResult;
    }

    private static String lookupFileTypeByFileName(final String fileName) {
        return lookupFileTypeFromPropertiesFile(fileName, false);
    }

    private static String lookupFileTypeByExtensionFromPropertiesFile(final String fileName) {
        final String fileKey = FilenameUtils.getExtension(fileName);
        return lookupFileTypeFromPropertiesFile(fileKey, true);
    }

    private static String lookupFileTypeFromPropertiesFile(final String fileKey, boolean byExtension) {
        final String propertyFileName = byExtension ? "MimeTypeDetectionByFileExtension" : "MimeTypeDetectionByFileName";
        final String propertyFileNameOnDisk =  propertyFileName + ".properties";
        try {
            logger.fine("checking " + propertyFileNameOnDisk + " for file key " + fileKey);
            return BundleUtil.getStringFromPropertyFile(fileKey, propertyFileName);
        } catch (final MissingResourceException ex) {
            //Only use info level if it's for an extension
            logger.log(byExtension ? Level.INFO : Level.FINE, fileKey + " is a filename/extension Dataverse doesn't know about. Consider adding it to the " + propertyFileNameOnDisk + " file.");
            return null;
        }
    }

    /* 
     * Custom method for identifying FITS files: 
     * TODO: 
     * the existing check for the "magic header" is very weak (see below); 
     * it should probably be replaced by attempting to parse and read at 
     * least the primary HDU, using the NOM fits parser. 
     * -- L.A. 4.0 alpha
    */
    private static boolean isFITSFile(File file) {

        try (BufferedInputStream ins = new BufferedInputStream(new FileInputStream(file))) {
            return isFITSFile(ins);
        } catch (IOException ex) {
            logger.fine("IOException: "+ ex.getMessage());
        } 
        
        return false;
    }
     
    private static boolean isFITSFile(InputStream ins) {
        boolean isFITS = false;

        // number of header bytes read for identification: 
        int magicWordLength = 6;
        String magicWord = "SIMPLE";

        try {
            byte[] b = new byte[magicWordLength];
            logger.fine("attempting to read "+magicWordLength+" bytes from the FITS format candidate stream.");
            if (ins.read(b, 0, magicWordLength) != magicWordLength) {
                throw new IOException();
            }

            if (magicWord.equals(new String(b))) {
                logger.fine("yes, this is FITS file!");
                isFITS = true;
            }
        } catch (IOException ex) {
            isFITS = false; 
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
        }
    
        return isFITS;
    }
    
    private static boolean isGraphMLFile(File file) {
        boolean isGraphML = false;
        logger.fine("begin isGraphMLFile()");
        FileReader fileReader = null;
        try{
            fileReader = new FileReader(file);
            javax.xml.stream.XMLInputFactory xmlif = javax.xml.stream.XMLInputFactory.newInstance();
            xmlif.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);

            XMLStreamReader xmlr = xmlif.createXMLStreamReader(fileReader);
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("graphml")) {
                        String schema = xmlr.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
                        logger.fine("schema = "+schema);
                        if (schema!=null && schema.contains("http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd")){
                            logger.fine("graphML is true");
                            isGraphML = true;
                        }
                    }
                    break;
                }
            }
        } catch(XMLStreamException e) {
            logger.fine("XML error - this is not a valid graphML file.");
            isGraphML = false;
        } catch(IOException e) {
            throw new EJBException(e);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ioex) {
                    logger.warning("IOException closing file reader in GraphML type checker");
                }
            }
        }
        logger.fine("end isGraphML()");
        return isGraphML;
    }

    public static String checkNetcdfOrHdf5(File file) {
        try ( NetcdfFile netcdfFile = NetcdfFiles.open(file.getAbsolutePath())) {
            if (netcdfFile == null) {
                // Can't open as a NetCDF or HDF5 file.
                return null;
            }
            String type = netcdfFile.getFileTypeId();
            if (type == null) {
                return null;
            }
            switch (type) {
                case "NetCDF":
                    return "application/netcdf";
                case "NetCDF-4":
                    return "application/netcdf";
                case "HDF5":
                    return "application/x-hdf5";
                default:
                    break;
            }
        } catch (IOException ex) {
            /**
             * When an HDF4 file is passed, it won't be detected. Instead, we've
             * seen exceptions like this:
             *
             * ucar.nc2.internal.iosp.hdf4.H4header makeDimension WARNING:
             * **dimension length=0 for TagVGroup= *refno=124 tag= VG (1965)
             * Vgroup length=28 class= Dim0.0 name= ixx using data 123
             *
             * java.lang.IllegalArgumentException: Dimension length =0 must be >
             * 0
             */
            return null;
        }
        return null;
    }

    // from MD5Checksum.java
    public static String calculateChecksum(String datafile, ChecksumType checksumType) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(datafile);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        return FileUtil.calculateChecksum(fis, checksumType);
    }

    // from MD5Checksum.java
    public static String calculateChecksum(InputStream in, ChecksumType checksumType) {
        MessageDigest md = null;
        try {
            // Use "SHA-1" (toString) rather than "SHA1", for example.
            md = MessageDigest.getInstance(checksumType.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] dataBytes = new byte[1024];

        int nread;
        try {
            while ((nread = in.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }

        return checksumDigestToString(md.digest());
    }
    
    public static String calculateChecksum(byte[] dataBytes, ChecksumType checksumType) {
        MessageDigest md = null;
        try {
            // Use "SHA-1" (toString) rather than "SHA1", for example.
            md = MessageDigest.getInstance(checksumType.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        md.update(dataBytes);

        return checksumDigestToString(md.digest());
        
    }
    
    public static String checksumDigestToString(byte[] digestBytes) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < digestBytes.length; i++) {
            sb.append(Integer.toString((digestBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static String generateOriginalExtension(String fileType) {
        if (fileType.equalsIgnoreCase("application/x-spss-sav")) {
            return ".sav";
        } else if (fileType.equalsIgnoreCase("application/x-spss-por")) {
            return ".por";    
        // in addition to "application/x-stata" we want to support 
        // "application/x-stata-13" ... etc.:
        } else if (fileType.toLowerCase().startsWith("application/x-stata")) {
            return ".dta";
        } else if (fileType.equalsIgnoreCase("application/x-dvn-csvspss-zip")) {
            return ".zip";
        } else if (fileType.equalsIgnoreCase("application/x-dvn-tabddi-zip")) {
            return ".zip";
        } else if (fileType.equalsIgnoreCase("application/x-rlang-transport")) {
            return ".RData";
        } else if (fileType.equalsIgnoreCase("text/csv") || fileType.equalsIgnoreCase("text/comma-separated-values")) {
            return ".csv";
        } else if (fileType.equalsIgnoreCase("text/tsv") || fileType.equalsIgnoreCase("text/tab-separated-values")) {
            return ".tsv";
        } else if (fileType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ".xlsx";
        }
        return "";
    }

	public static boolean useRecognizedType(String suppliedContentType, String recognizedType) {
		// is it any better than the type that was supplied to us,
		// if any?
		// This is not as trivial a task as one might expect...
		// We may need a list of "good" mime types, that should always
		// be chosen over other choices available. Maybe it should
		// even be a weighed list... as in, "application/foo" should
		// be chosen over "application/foo-with-bells-and-whistles".

		// For now the logic will be as follows:
		//
		// 1. If the contentType supplied (by the browser, most likely)
		// is some form of "unknown", we always discard it in favor of
		// whatever our own utilities have determined;
		// 2. We should NEVER trust the browser when it comes to the
		// following "ingestable" types: Stata, SPSS, R;
		// 2a. We are willing to TRUST the browser when it comes to
		// the CSV and XSLX ingestable types.
		// 3. We should ALWAYS trust our utilities when it comes to
		// ingestable types.
		if (suppliedContentType == null || suppliedContentType.equals("")
				|| suppliedContentType.equalsIgnoreCase(MIME_TYPE_UNDETERMINED_DEFAULT)
				|| suppliedContentType.equalsIgnoreCase(MIME_TYPE_UNDETERMINED_BINARY)
				|| (canIngestAsTabular(suppliedContentType) 
						&& !suppliedContentType.equalsIgnoreCase(MIME_TYPE_CSV)
						&& !suppliedContentType.equalsIgnoreCase(MIME_TYPE_CSV_ALT)
						&& !suppliedContentType.equalsIgnoreCase(MIME_TYPE_XLSX))
				|| canIngestAsTabular(recognizedType) || recognizedType.equals("application/fits-gzipped")
				|| recognizedType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)
				|| recognizedType.equalsIgnoreCase(BagItFileHandler.FILE_TYPE)
				|| recognizedType.equals(MIME_TYPE_ZIP)
                || recognizedType.equals(MIME_TYPE_RO_CRATE)) {
			return true;
		}
		return false;
	}

	public static File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit)
            throws IOException, FileExceedsMaxSizeException, FileExceedsStorageQuotaException {
            return saveInputStreamInTempFile(inputStream, fileSizeLimit, null);
        }
        
        public static File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit, Long storageQuotaLimit)
            throws IOException, FileExceedsMaxSizeException, FileExceedsStorageQuotaException {
        Path tempFile = Files.createTempFile(Paths.get(getFilesTempDirectory()), "tmp", "upload");
        
        if (inputStream != null && tempFile != null) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // size check: 
            // (note that "no size limit set" = "unlimited")
            Long fileSize = tempFile.toFile().length();
            if (fileSizeLimit != null && fileSize > fileSizeLimit) {
                try {tempFile.toFile().delete();} catch (Exception ex) {}
                throw new FileExceedsMaxSizeException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"), bytesToHumanReadable(fileSize), bytesToHumanReadable(fileSizeLimit)));  
            }
            
            if (storageQuotaLimit != null && fileSize > storageQuotaLimit) {
                try {tempFile.toFile().delete();} catch (Exception ex) {}
                throw new FileExceedsStorageQuotaException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.quota_exceeded"), bytesToHumanReadable(fileSize), bytesToHumanReadable(storageQuotaLimit)));  
            }
            
            return tempFile.toFile();
        }
        throw new IOException("Failed to save uploaded file.");
    }
    
    /* 
     * This method creates a DataFile; 
     * The bytes from the suppplied InputStream have already been saved in the temporary location. 
     * This method should only be called by the upper-level methods that handle 
     * file upload and creation for individual use cases - a single file upload, 
     * an upload of a zip archive that needs to be unpacked and turned into 
     * individual files, etc., and once the file name and mime type have already 
     * been figured out. 
    */

    public static DataFile createSingleDataFile(DatasetVersion version, File tempFile, String fileName, String contentType, DataFile.ChecksumType checksumType) {
        return createSingleDataFile(version, tempFile, null, fileName, contentType, checksumType, null, false);
    }

    public static DataFile createSingleDataFile(DatasetVersion version, File tempFile, String storageIdentifier,  String fileName, String contentType, DataFile.ChecksumType checksumType, String checksum) {
        return createSingleDataFile(version, tempFile, storageIdentifier, fileName, contentType, checksumType, checksum, false);
    }
    
    public static DataFile createSingleDataFile(DatasetVersion version, File tempFile, String storageIdentifier, String fileName, String contentType, DataFile.ChecksumType checksumType, String checksum, boolean addToDataset) {

        if ((tempFile == null) && (storageIdentifier == null)) {
            return null;
        }

        DataFile datafile = new DataFile(contentType);
        datafile.setModificationTime(new Timestamp(new Date().getTime()));
        /**
         * @todo Think more about when permissions on files are modified.
         * Obviously, here at create time files have some sort of permissions,
         * even if these permissions are *implied*, by ViewUnpublishedDataset at
         * the dataset level, for example.
         */
        datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        FileMetadata fmd = new FileMetadata();

        fmd.setLabel(fileName);

        if (addToDataset) {
            datafile.setOwner(version.getDataset());
        }
        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);
        if (addToDataset) {
            if (version.getFileMetadatas() == null) {
                version.setFileMetadatas(new ArrayList<>());
            }
            version.getFileMetadatas().add(fmd);
            fmd.setDatasetVersion(version);
            version.getDataset().getFiles().add(datafile);
        }
        if (storageIdentifier == null) {
            generateStorageIdentifier(datafile);
            if (!tempFile.renameTo(new File(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier()))) {
                return null;
            }
        } else {
            datafile.setStorageIdentifier(storageIdentifier);
        }

        if ((checksum !=null)&&(!checksum.isEmpty())) {
        	datafile.setChecksumType(checksumType);
            datafile.setChecksumValue(checksum);
        } else {
        	try {
        		// We persist "SHA1" rather than "SHA-1".
        		datafile.setChecksumType(checksumType);
        		datafile.setChecksumValue(calculateChecksum(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier(), datafile.getChecksumType()));
        	} catch (Exception cksumEx) {
        		logger.warning("Could not calculate " + checksumType + " signature for the new file " + fileName);
        	}
        }
        return datafile;
    }
    
    
    /**
        For the restructuring of zipped shapefiles, create a timestamped directory.
        This directory is deleted after successful restructuring.
    
        Naming convention: getFilesTempDirectory() + "shp_" + "yyyy-MM-dd-hh-mm-ss-SSS"
    */
    public static File getShapefileUnzipTempDirectory(){
        
        String tempDirectory = getFilesTempDirectory();
        if (tempDirectory == null){
            logger.severe("Failed to retrieve tempDirectory, null was returned" );
            return null;
        }
        String datestampedFileName =  "shp_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());
        String datestampedFolderName = tempDirectory + "/" + datestampedFileName;
        
        File datestampedFolder = new File(datestampedFolderName);
        if (!datestampedFolder.isDirectory()) {
            /* Note that "createDirectories()" must be used - not 
             * "createDirectory()", to make sure all the parent 
             * directories that may not yet exist are created as well. 
             */
            try {
                Files.createDirectories(Paths.get(datestampedFolderName));
            } catch (IOException ex) {
                logger.severe("Failed to create temp. directory to unzip shapefile: " + datestampedFolderName );
                return null;
            }
        }
        return datestampedFolder;        
    }
    
    public static boolean canIngestAsTabular(DataFile dataFile) {
        String mimeType = dataFile.getContentType();
        
        return canIngestAsTabular(mimeType);
    } 
    
    public static boolean canIngestAsTabular(String mimeType) {
        /* 
         * In the final 4.0 we'll be doing real-time checks, going through the 
         * available plugins and verifying the lists of mime types that they 
         * can handle. In 4.0 beta, the ingest plugins are still built into the 
         * main code base, so we can just go through a hard-coded list of mime 
         * types. -- L.A. 
         */
        
        if (mimeType == null) {
            return false;
        }
        
        switch (mimeType) {
            case MIME_TYPE_STATA:
            case MIME_TYPE_STATA13:
            case MIME_TYPE_STATA14:
            case MIME_TYPE_STATA15:
            case MIME_TYPE_RDATA:
            case MIME_TYPE_CSV:
            case MIME_TYPE_CSV_ALT:
            case MIME_TYPE_TSV:
            //case MIME_TYPE_TSV_ALT:
            case MIME_TYPE_XLSX:
            case MIME_TYPE_SPSS_SAV:
            case MIME_TYPE_SPSS_POR:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Return the location where data should be stored temporarily after uploading (UI or API)
     * for local processing (ingest, unzip, ...) and transfer to final destination (see storage subsystem).
     *
     * This location is checked to be configured, does exist, and is writeable via
     * {@link ConfigCheckService#checkSystemDirectories()}.
     *
     * @return String with a path to the temporary location. Will not be null (former versions did to indicate failure)
     */
    public static String getFilesTempDirectory() {
        return JvmSettings.FILES_DIRECTORY.lookup() + File.separator + "temp";
    }
    
    public static void generateS3PackageStorageIdentifier(DataFile dataFile) {
    	String driverId = dataFile.getOwner().getEffectiveStorageDriverId();
		
        String bucketName = System.getProperty("dataverse.files." + driverId + ".bucket-name");
        String storageId = driverId + DataAccess.SEPARATOR + bucketName + ":" + dataFile.getFileMetadata().getLabel();
        dataFile.setStorageIdentifier(storageId);
    }
    
    public static void generateStorageIdentifier(DataFile dataFile) {
    	//Is it true that this is only used for temp files and we could safely prepend "tmp://" to indicate that?
        dataFile.setStorageIdentifier(generateStorageIdentifier());
    }
    
    public static String generateStorageIdentifier() {
        
        UUID uid = UUID.randomUUID();
                
        logger.log(Level.FINE, "UUID value: {0}", uid.toString());
        
        // last 6 bytes, of the random UUID, in hex: 
        
        String hexRandom = uid.toString().substring(24);
        
        logger.log(Level.FINE, "UUID (last 6 bytes, 12 hex digits): {0}", hexRandom);
        
        String hexTimestamp = Long.toHexString(new Date().getTime());
        
        logger.log(Level.FINE, "(not UUID) timestamp in hex: {0}", hexTimestamp);
            
        String storageIdentifier = hexTimestamp + "-" + hexRandom;
        
        logger.log(Level.FINE, "timestamp/UUID hybrid: {0}", storageIdentifier);
        return storageIdentifier; 
    }
    
    public static void createIngestFailureReport(DataFile dataFile, String message) {
        createIngestReport(dataFile, IngestReport.INGEST_STATUS_FAILURE, message);
    }
    
    private static void createIngestReport (DataFile dataFile, int status, String message) {
        IngestReport errorReport = new IngestReport();
        if (status == IngestReport.INGEST_STATUS_FAILURE) {
                errorReport.setFailure();
                errorReport.setReport(message);
                errorReport.setDataFile(dataFile);
                dataFile.setIngestReport(errorReport);
        }
    }

    public enum FileCitationExtension {

        ENDNOTE("-endnote.xml"),
        RIS(".ris"),
        BIBTEX(".bib");

        private final String text;

        private FileCitationExtension(final String text) {
            this.text = text;
        }
    }

    public static String getCiteDataFileFilename(String fileTitle, FileCitationExtension fileCitationExtension) {
    	if((fileTitle==null) || (fileCitationExtension == null)) {
    		return null;
    	}
        if (fileTitle.endsWith("tab")) {
            return fileTitle.replaceAll("\\.tab$", fileCitationExtension.text);
        } else {
            return fileTitle + fileCitationExtension.text;
        }
    }

    /**
     * @todo Consider returning not only the boolean but the human readable
     * reason why the popup is required, which could be used in the GUI to
     * elaborate on the text "This file cannot be downloaded publicly."
     */
    public static boolean isDownloadPopupRequired(DatasetVersion datasetVersion) {
        logger.fine("Checking if download popup is required.");
        Boolean answer = popupDueToStateOrTerms(datasetVersion);
        if (answer != null) {
            return answer;
        }
        // 3. Guest Book:
        if (datasetVersion.getDataset() != null && datasetVersion.getDataset().getGuestbook() != null && datasetVersion.getDataset().getGuestbook().isEnabled() && datasetVersion.getDataset().getGuestbook().getDataverse() != null) {
            logger.fine("Download popup required because of guestbook.");
            return true;
        }
        logger.fine("Download popup is not required.");
        return false;
    }
    
    public static boolean isRequestAccessPopupRequired(DatasetVersion datasetVersion) {
        
        Boolean answer = popupDueToStateOrTerms(datasetVersion);
        if (answer != null) {
            return answer;
        }
     // 3. Guest Book:
        if (datasetVersion.getDataset() != null && datasetVersion.getDataset().getGuestbook() != null && datasetVersion.getDataset().getGuestbook().isEnabled() && datasetVersion.getDataset().getGuestbook().getDataverse() != null) {
            logger.fine("Request access popup required because of guestbook.");
            return true;
        }
        logger.fine("Request access popup is not required.");
        return false;
    }
    
    /* Code shared by isDownloadPopupRequired and isRequestAccessPopupRequired.
     * 
     * Returns Boolean to allow null = no decision. This allows the isDownloadPopupRequired method to then add another check w.r.t. guestbooks before returning its value.
     * 
     */
    private static Boolean popupDueToStateOrTerms(DatasetVersion datasetVersion) {

        // Each of these conditions is sufficient reason to have to
        // present the user with the popup:
        if (datasetVersion == null) {
            logger.fine("Popup not required because datasetVersion is null.");
            return false;
        }
        // 0. if version is draft then Popup "not required"
        if (!datasetVersion.isReleased()) {
            logger.fine("Popup not required because datasetVersion has not been released.");
            return false;
        }
        // 1. License and Terms of Use:
        if (datasetVersion.getTermsOfUseAndAccess() != null) {
            License license = DatasetUtil.getLicense(datasetVersion);
            if ((license == null && StringUtils.isNotBlank(datasetVersion.getTermsOfUseAndAccess().getTermsOfUse()))
                    || (license != null && !license.isDefault())) {
                logger.fine("Popup required because of license or terms of use.");
                return true;
            }

            // 2. Terms of Access:
            if (!(datasetVersion.getTermsOfUseAndAccess().getTermsOfAccess() == null) && !datasetVersion.getTermsOfUseAndAccess().getTermsOfAccess().equals("")) {
                logger.fine("Popup required because of terms of access.");
                return true;
            }
        }
        //No decision based on the criteria above
        return null;
    }

    /**
     * isGuestbookAndTermsPopupRequired
     * meant to replace both isDownloadPopupRequired() and isRequestAccessDownloadPopupRequired() when the guestbook-terms-popup-fragment.xhtml
     * replaced file-download-popup-fragment.xhtml and file-request-access-popup-fragment.xhtml
     * @param datasetVersion
     * @return boolean
     */

    public static boolean isGuestbookAndTermsPopupRequired(DatasetVersion datasetVersion) {
        return isGuestbookPopupRequired(datasetVersion) || isTermsPopupRequired(datasetVersion);
    }

    public static boolean isGuestbookPopupRequired(DatasetVersion datasetVersion) {

        if (datasetVersion == null) {
            logger.fine("GuestbookPopup not required because datasetVersion is null.");
            return false;
        }
        //0. if version is draft then Popup "not required"
        if (!datasetVersion.isReleased()) {
            logger.fine("GuestbookPopup not required because datasetVersion has not been released.");
            return false;
        }

        // 3. Guest Book:
        if (datasetVersion.getDataset() != null && datasetVersion.getDataset().getGuestbook() != null && datasetVersion.getDataset().getGuestbook().isEnabled() && datasetVersion.getDataset().getGuestbook().getDataverse() != null) {
            logger.fine("GuestbookPopup required because an enabled guestbook exists.");
            return true;
        }

        logger.fine("GuestbookPopup is not required.");
        return false;
    }

    public static boolean isTermsPopupRequired(DatasetVersion datasetVersion) {
        Boolean answer = popupDueToStateOrTerms(datasetVersion);
        if(answer == null) {
            logger.fine("TermsPopup is not required.");
            return false;
        }
        return answer;
    }
    
    /**
     * Provide download URL if no Terms of Use, no guestbook, and not
     * restricted.
     */
    public static boolean isPubliclyDownloadable(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            return false;
        }
        if (fileMetadata.isRestricted()) {
            String msg = "Not publicly downloadable because the file is restricted.";
            logger.fine(msg);
            return false;
        }
        if (isActivelyEmbargoed(fileMetadata)) {
            return false;
        }
        if (isRetentionExpired(fileMetadata)) {
            return false;
        }
        boolean popupReasons = isDownloadPopupRequired(fileMetadata.getDatasetVersion());
        if (popupReasons == true) {
            /**
             * @todo The user clicking publish may have a bad "Dude, where did
             * the file Download URL go" experience in the following scenario:
             *
             * - The user creates a dataset and uploads a file.
             *
             * - The user sets Terms of Use, which means a Download URL should
             * not be displayed.
             *
             * - While the dataset is in draft, the Download URL is displayed
             * due to the rule "Download popup required because datasetVersion
             * has not been released."
             *
             * - Once the dataset is published the Download URL disappears due
             * to the rule "Download popup required because of license or terms
             * of use."
             *
             * In short, the Download URL disappears on publish in the scenario
             * above, which is weird. We should probably attempt to see into the
             * future to when the dataset is published to see if the file will
             * be publicly downloadable or not.
             */
            return false;
        }
        return true;
    }

    /**
     * This is what the UI displays for "Download URL" on the file landing page
     * (DOIs rather than file IDs.
     */
    public static String getPublicDownloadUrl(String dataverseSiteUrl, String persistentId, Long fileId) {
        String path = null;
        if(persistentId != null) {
            path = dataverseSiteUrl + "/api/access/datafile/:persistentId?persistentId=" + persistentId;
        } else if( fileId != null) {
            path = dataverseSiteUrl + "/api/access/datafile/" + fileId;
        } else {
            logger.info("In getPublicDownloadUrl but persistentId & fileId are both null!");
        }
        return path;
    }
    
    /**
     * The FileDownloadServiceBean operates on file IDs, not DOIs.
     */
    public static String getFileDownloadUrlPath(String downloadType, Long fileId, boolean gbRecordsWritten, Long fileMetadataId) {
        String fileDownloadUrl = "/api/access/datafile/" + fileId;
        if (downloadType != null) {
            switch(downloadType) {
            case "original":
            case"RData":
            case "tab":
            case "GlobusTransfer":
                    fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=" + downloadType;
                    break;
            case "bundle":
                    if (fileMetadataId == null) {
                        fileDownloadUrl = "/api/access/datafile/bundle/" + fileId;
                    } else {
                        fileDownloadUrl = "/api/access/datafile/bundle/" + fileId + "?fileMetadataId=" + fileMetadataId;
                    }
                    break;
            case "var":
                    if (fileMetadataId == null) {
                        fileDownloadUrl = "/api/access/datafile/" + fileId + "/metadata";
                    } else {
                        fileDownloadUrl = "/api/access/datafile/" + fileId + "/metadata?fileMetadataId=" + fileMetadataId;
                    }
                    break;
                }
                
            }
        if (gbRecordsWritten) {
            if (fileDownloadUrl.contains("?")) {
                fileDownloadUrl += "&gbrecs=true";
            } else {
                fileDownloadUrl += "?gbrecs=true";
            }
        }
        logger.fine("Returning file download url: " + fileDownloadUrl);
        return fileDownloadUrl;
    }

    public static File inputStreamToFile(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            logger.info("In inputStreamToFile but inputStream was null! Returning null rather than a File.");
            return null;
        }
        File file = File.createTempFile(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        try(OutputStream outputStream = new FileOutputStream(file)){
        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        return file;
	}
    }

    /* 
     * This method tells you if thumbnail generation is *supported* 
     * on this type of file. i.e., if true, it does not guarantee that a thumbnail 
     * can/will be generated; but it means that we can try. 
     */
    public static boolean isThumbnailSupported (DataFile file) {
        if (file == null) {
            return false;
        }
        
        if (file.isHarvested() || StringUtil.isEmpty(file.getStorageIdentifier())) {
            return false;
        }
        
        String contentType = file.getContentType();
        
        // Some browsers (Chrome?) seem to identify FITS files as mime
        // type "image/fits" on upload; this is both incorrect (the official
        // mime type for FITS is "application/fits", and problematic: then
        // the file is identified as an image, and the page will attempt to 
        // generate a preview - which of course is going to fail...
        if (MIME_TYPE_FITSIMAGE.equalsIgnoreCase(contentType)) {
            return false;
        }
        // besides most image/* types, we can generate thumbnails for
        // pdf and "world map" files:
        
        return (contentType != null && 
                (contentType.startsWith("image/") || 
                contentType.equalsIgnoreCase("application/pdf") ||
                (file.isTabularData() && file.hasGeospatialTag()) ||
                contentType.equalsIgnoreCase(MIME_TYPE_GEO_SHAPE)));
    }
    
    
    /* 
     * The method below appears to be unnecessary; 
     * it duplicates the method generateImageThumbnailFromFileAsBase64() from ImageThumbConverter;
     * plus it creates an unnecessary temp file copy of the source file.    
    public static String rescaleImage(File file) throws IOException {
        if (file == null) {
            logger.info("file was null!!");
            return null;
        }
        File tmpFile = File.createTempFile("tempFileToRescale", ".tmp");
        BufferedImage fullSizeImage = ImageIO.read(file);
        if (fullSizeImage == null) {
            logger.info("fullSizeImage was null!");
            return null;
        }
        int width = fullSizeImage.getWidth();
        int height = fullSizeImage.getHeight();
        FileChannel src = new FileInputStream(file).getChannel();
        FileChannel dest = new FileOutputStream(tmpFile).getChannel();
        dest.transferFrom(src, 0, src.size());
        String pathToResizedFile = ImageThumbConverter.rescaleImage(fullSizeImage, width, height, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE, tmpFile.getAbsolutePath());
        File resizedFile = new File(pathToResizedFile);
        return ImageThumbConverter.getImageAsBase64FromFile(resizedFile);
    }
    */
    
    public static DatasetThumbnail getThumbnail(DataFile file) {

        String imageSourceBase64 = ImageThumbConverter.getImageThumbnailAsBase64(file, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
        DatasetThumbnail defaultDatasetThumbnail = new DatasetThumbnail(imageSourceBase64, file);
        return defaultDatasetThumbnail;

    }
    
    public static boolean isPackageFile(DataFile dataFile) {
        return DataFileServiceBean.MIME_TYPE_PACKAGE_FILE.equalsIgnoreCase(dataFile.getContentType());
    }
    
    public static S3AccessIO getS3AccessForDirectUpload(Dataset dataset) {
    	String driverId = dataset.getEffectiveStorageDriverId();
    	boolean directEnabled = Boolean.getBoolean("dataverse.files." + driverId + ".upload-redirect");
    	//Should only be requested when it is allowed, but we'll log a warning otherwise
    	if(!directEnabled) {
    		logger.warning("Direct upload not supported for files in this dataset: " + dataset.getId());
    		return null;
    	}
    	S3AccessIO<DataFile> s3io = null;
    	String bucket = System.getProperty("dataverse.files." + driverId + ".bucket-name") + "/";
    	String sid = null;
    	int i=0;
    	while (s3io==null && i<5) {
    		sid = bucket+ dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage() + "/" + FileUtil.generateStorageIdentifier();
    		try {
    			s3io = new S3AccessIO<DataFile>(sid, driverId);
    			if(s3io.exists()) {
    				s3io=null;
    				i=i+1;
    			} 

    		} catch (Exception e) {
    			i=i+1;
    		}

    	}
    	return s3io;
    }
    
    private static InputStream getOriginalFileInputStream(StorageIO<DataFile> storage, boolean isTabularData) throws IOException {
        storage.open(DataAccessOption.READ_ACCESS);
        if (!isTabularData) {
            return storage.getInputStream();
        } else {
            // if this is a tabular file, read the preserved original "auxiliary file"
            // instead:
            return storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
        }
    }

    public static void validateDataFileChecksum(DataFile dataFile) throws IOException {
        DataFile.ChecksumType checksumType = dataFile.getChecksumType();
        if (checksumType == null) {
            String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.noChecksumType", Arrays.asList(dataFile.getId().toString()));
            logger.log(Level.INFO, info);
            throw new IOException(info);
        }

        StorageIO<DataFile> storage = dataFile.getStorageIO();
        String recalculatedChecksum = null;

        try (InputStream inputStream = getOriginalFileInputStream(storage, dataFile.isTabularData())) {
            recalculatedChecksum = FileUtil.calculateChecksum(inputStream, checksumType);
        } catch (IOException ioex) {
            String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.failRead", Arrays.asList(dataFile.getId().toString()));
            logger.log(Level.INFO, info);
            throw new IOException(info);
        } catch (RuntimeException rte) {
            logger.log(Level.SEVERE, "failed to calculated checksum, one retry", rte);
            recalculatedChecksum = null;
        }

        if (recalculatedChecksum == null) { //retry once
            storage = dataFile.getStorageIO();
            try (InputStream inputStream = getOriginalFileInputStream(storage, dataFile.isTabularData())) {
                recalculatedChecksum = FileUtil.calculateChecksum(inputStream, checksumType);
            }
        }

        if (recalculatedChecksum == null) {
            String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.failCalculateChecksum", Arrays.asList(dataFile.getId().toString()));
            logger.log(Level.INFO, info);
            throw new IOException(info);
        }

        // TODO? What should we do if the datafile does not have a non-null checksum?
        // Should we fail, or should we assume that the recalculated checksum
        // is correct, and populate the checksumValue field with it?
        if (!recalculatedChecksum.equals(dataFile.getChecksumValue())) {
            // There's one possible condition that is 100% recoverable and can
            // be automatically fixed (issue #6660):
            boolean fixed = false;
            if (!dataFile.isTabularData() && dataFile.getIngestReport() != null) {
                // try again, see if the .orig file happens to be there:
                try (InputStream in = storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION)) {
                    recalculatedChecksum = FileUtil.calculateChecksum(in, checksumType);
                } catch (RuntimeException rte) {
                    recalculatedChecksum = null;
                }
                if (recalculatedChecksum != null) {
                    // try again:
                    if (recalculatedChecksum.equals(dataFile.getChecksumValue())) {
                        fixed = true;
                        try {
                            storage.revertBackupAsAux(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                        } catch (IOException ioex) {
                            fixed = false;
                        }
                    }
                }
            }

            if (!fixed) {
                String info = BundleUtil.getStringFromBundle("dataset.publish.file.validation.error.wrongChecksumValue", Arrays.asList(dataFile.getId().toString()));
                logger.log(Level.INFO, info);
                logger.fine("Expected: " + dataFile.getChecksumValue() +", calculated: " + recalculatedChecksum);
                throw new IOException(info);
            }
        }

        logger.log(Level.INFO, "successfully validated DataFile {0}; checksum {1}", new Object[]{dataFile.getId(), recalculatedChecksum});
    }
    
    public static String getStorageIdentifierFromLocation(String location) {
    	int driverEnd = location.indexOf(DataAccess.SEPARATOR) + DataAccess.SEPARATOR.length();
    	int bucketEnd = driverEnd + location.substring(driverEnd).indexOf("/");
    	return location.substring(0,bucketEnd) + ":" + location.substring(location.lastIndexOf("/") + 1);
    }
    
    public static void deleteTempFile(DataFile dataFile, Dataset dataset, IngestServiceBean ingestService) {
    	logger.info("Deleting " + dataFile.getStorageIdentifier());
    	// Before we remove the file from the list and forget about 
    	// it:
    	// The physical uploaded file is still sitting in the temporary
    	// directory. If it were saved, it would be moved into its 
    	// permanent location. But since the user chose not to save it,
    	// we have to delete the temp file too. 
    	// 
    	// Eventually, we will likely add a dedicated mechanism
    	// for managing temp files, similar to (or part of) the storage 
    	// access framework, that would allow us to handle specialized
    	// configurations - highly sensitive/private data, that 
    	// has to be kept encrypted even in temp files, and such. 
    	// But for now, we just delete the file directly on the 
    	// local filesystem: 

    	try {
    		List<Path> generatedTempFiles = ingestService.listGeneratedTempFiles(
    				Paths.get(getFilesTempDirectory()), dataFile.getStorageIdentifier());
    		if (generatedTempFiles != null) {
    			for (Path generated : generatedTempFiles) {
    				logger.fine("(Deleting generated thumbnail file " + generated.toString() + ")");
    				try {
    					Files.delete(generated);
    				} catch (IOException ioex) {
    					logger.warning("Failed to delete generated file " + generated.toString());
    				}
    			}
    		}
    		String si = dataFile.getStorageIdentifier();
    		if (si.contains(DataAccess.SEPARATOR)) {
    			//Direct upload files will already have a store id in their storageidentifier
    			//but they need to be associated with a dataset for the overall storagelocation to be calculated
    			//so we temporarily set the owner
    			if(dataFile.getOwner()!=null) {
    				logger.warning("Datafile owner was not null as expected");
    			}
    			dataFile.setOwner(dataset);
    			//Use one StorageIO to get the storageLocation and then create a direct storage storageIO class to perform the delete 
    			// (since delete is forbidden except for direct storage)
    			String sl = DataAccess.getStorageIO(dataFile).getStorageLocation();
    			DataAccess.getDirectStorageIO(sl).delete();
    		} else {
    			//Temp files sent to this method have no prefix, not even "tmp://"
    			Files.delete(Paths.get(FileUtil.getFilesTempDirectory() + "/" + dataFile.getStorageIdentifier()));
    		}
    	} catch (IOException ioEx) {
    		// safe to ignore - it's just a temp file. 
    		logger.warning(ioEx.getMessage());
    		if(dataFile.getStorageIdentifier().contains(DataAccess.SEPARATOR)) {
    			logger.warning("Failed to delete temporary file " + dataFile.getStorageIdentifier());
    		} else {
    			logger.warning("Failed to delete temporary file " + FileUtil.getFilesTempDirectory() + "/"
    					+ dataFile.getStorageIdentifier());
    		}
    	} finally {
    		dataFile.setOwner(null);
    	}
    }
    
    public static boolean isFileAlreadyUploaded(DataFile dataFile, Map checksumMapNew, Map fileAlreadyExists) {
        if (checksumMapNew == null) {
            checksumMapNew = new HashMap<>();
        }
        
        if (fileAlreadyExists == null) {
            fileAlreadyExists = new HashMap<>();
        }
        
        String chksum = dataFile.getChecksumValue();
        
        if (chksum == null) {
            return false;
        }
        
        if (checksumMapNew.get(chksum) != null) {
            fileAlreadyExists.put(dataFile, checksumMapNew.get(chksum));
            return true;
        }
        
        checksumMapNew.put(chksum, dataFile);
        return false;
    }
    
    public static String formatFolderListingHtml(String folderName, DatasetVersion version, String apiLocation, boolean originals) {
        String title = formatTitle("Index of folder /" + folderName);
        List<FileMetadata> fileMetadatas = version.getFileMetadatasFolderListing(folderName);
        
        if (fileMetadatas == null || fileMetadatas.isEmpty()) {
            return "";
        }
        
        String persistentId = version.getDataset().getGlobalId().asString();
        
        StringBuilder sb = new StringBuilder();
        
        String versionTag = version.getFriendlyVersionNumber();
        versionTag = "DRAFT".equals(versionTag) ? "Draft Version" : "v. " + versionTag;
        sb.append(HtmlFormatUtil.formatTag("Index of folder /" + folderName + 
                " in dataset " + persistentId + 
                " (" + versionTag + ")", HTML_H1));
        sb.append("\n");
        sb.append(formatFolderListingTableHtml(folderName, fileMetadatas, apiLocation, originals));
        
        String body = sb.toString();
                 
        return formatDoc(title, body);
    }
    
    private static String formatFolderListingTableHtml(String folderName, List<FileMetadata> fileMetadatas, String apiLocation, boolean originals) {
        StringBuilder sb = new StringBuilder(); 
        
        sb.append(formatFolderListingTableHeaderHtml());
        
        for (FileMetadata fileMetadata : fileMetadatas) {
            String localFolder = fileMetadata.getDirectoryLabel() == null ? "" : fileMetadata.getDirectoryLabel(); 
            
            if (folderName.equals(localFolder)) {
                String accessUrl = getFileAccessUrl(fileMetadata, apiLocation, originals);
                sb.append(formatFileListEntryHtml(fileMetadata, accessUrl));
                sb.append("\n");

            } else if (localFolder.startsWith(folderName)){
                String subFolder = "".equals(folderName) ? localFolder : localFolder.substring(folderName.length() + 1);
                if (subFolder.indexOf('/') > 0) {
                    subFolder = subFolder.substring(0, subFolder.indexOf('/'));
                }
                String folderAccessUrl = getFolderAccessUrl(fileMetadata.getDatasetVersion(), folderName, subFolder, apiLocation, originals);
                sb.append(formatFileListFolderHtml(subFolder, folderAccessUrl));
                sb.append("\n");
            }
        }
        
        return formatTable(sb.toString());
    }
        
    private static String formatFolderListingTableHeaderHtml() {
        
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlFormatUtil.formatTag("Name", HTML_TABLE_HDR));
        sb.append(HtmlFormatUtil.formatTag("Last Modified", HTML_TABLE_HDR));
        sb.append(HtmlFormatUtil.formatTag("Size", HTML_TABLE_HDR));
        sb.append(HtmlFormatUtil.formatTag("Description", HTML_TABLE_HDR));
        
        String hdr = formatTableRow(sb.toString());
        
        // add a separator row (again, we want it to look just like Apache index)
        return hdr.concat(formatTableRow(HtmlFormatUtil.formatTag("<hr>", HTML_TABLE_HDR,"colspan=\"4\""))); 
        
    }
    
    private static String formatFileListEntryHtml(FileMetadata fileMetadata, String accessUrl) {
        StringBuilder sb = new StringBuilder(); 
        
        String fileName = fileMetadata.getLabel();
        String dateString =  new SimpleDateFormat(FILE_LIST_DATE_FORMAT).format(fileMetadata.getDataFile().getCreateDate()); 
        String sizeString = fileMetadata.getDataFile().getFriendlySize();
        
        sb.append(formatTableCell(formatLink(fileName, accessUrl))); 
        sb.append(formatTableCellAlignRight(dateString));
        sb.append(formatTableCellAlignRight(sizeString));
        sb.append(formatTableCellAlignRight("&nbsp;"));
        
        return formatTableRow(sb.toString());
    }
    
    private static String formatFileListFolderHtml(String folderName, String listApiUrl) {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(formatTableCell(formatLink(folderName+"/", listApiUrl)));
        sb.append(formatTableCellAlignRight(" - "));
        sb.append(formatTableCellAlignRight(" - "));
        sb.append(formatTableCellAlignRight("&nbsp;"));
        
        return formatTableRow(sb.toString());
    }
    
    private static String getFileAccessUrl(FileMetadata fileMetadata, String apiLocation, boolean original) {
        String fileId = fileMetadata.getDataFile().getId().toString();
        
        if (StringUtil.nonEmpty(fileMetadata.getDirectoryLabel())) {
            fileId = fileMetadata.getDirectoryLabel().concat("/").concat(fileId);
        }
        
        String formatArg = fileMetadata.getDataFile().isTabularData() && original ? "?format=original" : ""; 
        
        return apiLocation + "/api/access/datafile/" + fileId + formatArg; 
    }
    
    private static String getFolderAccessUrl(DatasetVersion version, String currentFolder, String subFolder, String apiLocation, boolean originals) {
        String datasetId = version.getDataset().getId().toString();
        String versionTag = version.getFriendlyVersionNumber();
        versionTag = versionTag.replace("DRAFT", DS_VERSION_DRAFT);
        if (!"".equals(currentFolder)) {
            subFolder = currentFolder + "/" + subFolder;
        }
        
        return apiLocation + "/api/datasets/" + datasetId + 
                "/dirindex/?version=" + versionTag + "&" +
                "folder=" + subFolder + 
                (originals ? "&original=true" : "");
    }

    /**
     * This method takes a JsonArray of JsonObjects and extracts the fields of those
     * objects corresponding to the supplied list of headers to create a rectangular
     * CSV file (with headers) that lists the values from each object in rows. It
     * was developed for use with the metrics API but could be useful in other calls
     * that require the same transformation.
     * 
     * @param jsonArray
     *            - the array of JsonObjects containing key/value pairs with keys
     *            matching the headers. Keys that don't match a header are
     *            ignored/not included.
     * @param headers
     *            - strings to use as CSV headers
     * @return - the CSV file as a string, rows separated by '\n'
     */
    public static String jsonArrayOfObjectsToCSV(JsonArray jsonArray, String... headers) {
        StringBuilder csvSB = new StringBuilder(String.join(",", headers));
        jsonArray.forEach((jv) -> {
            JsonObject jo = (JsonObject) jv;
            String[] values = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                if(jo.containsKey(headers[i])) {
                    values[i] = jo.get(headers[i]).toString();
                }
            }
            csvSB.append("\n").append(String.join(",", values));
        });
        return csvSB.toString();
    }

    public static boolean isActivelyEmbargoed(DataFile df) {
        Embargo e = df.getEmbargo();
        if (e != null) {
            LocalDate endDate = e.getDateAvailable();
            if (endDate != null && endDate.isAfter(LocalDate.now())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isActivelyEmbargoed(FileMetadata fileMetadata) {
        return isActivelyEmbargoed(fileMetadata.getDataFile());
    }
    
    public static boolean isActivelyEmbargoed(List<FileMetadata> fmdList) {
        for (FileMetadata fmd : fmdList) {
            if (isActivelyEmbargoed(fmd)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRetentionExpired(DataFile df) {
        Retention e = df.getRetention();
        if (e != null) {
            LocalDate endDate = e.getDateUnavailable();
            if (endDate != null && endDate.isBefore(LocalDate.now())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRetentionExpired(FileMetadata fileMetadata) {
        return isRetentionExpired(fileMetadata.getDataFile());
    }

    public static boolean isRetentionExpired(List<FileMetadata> fmdList) {
        for (FileMetadata fmd : fmdList) {
            if (isRetentionExpired(fmd)) {
                return true;
            }
        }
        return false;
    }

    public static String getStorageDriver(DataFile dataFile) {
        String storageIdentifier = dataFile.getStorageIdentifier();
        return storageIdentifier.substring(0, storageIdentifier.indexOf(DataAccess.SEPARATOR));
    }

    /**
     * Replace spaces with "_" and remove invalid chars
     * @param fileNameIn - Name before sanitization NOTE: not full path since this method removes '/' and '\'
     * @return filename without spaces or invalid chars
     */
    public static String sanitizeFileName(String fileNameIn) {
        return fileNameIn == null ? null : fileNameIn.replace(' ', '_').replaceAll("[\\\\/:*?\"<>|,;]", "");
    }

    public static Path createDirStructure(String rootDirectory, String... subdirectories) throws IOException {
        Path path = Path.of(rootDirectory, subdirectories);
        Files.createDirectories(path);
        return path;
    }

    public static boolean isFileOfImageType(File file) throws IOException {
        Tika tika = new Tika();
        String mimeType = tika.detect(file);
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Converts a filename from ISO_8859_1 to UTF_8 to handle encoding issues.
     * Ref: https://github.com/eclipse-ee4j/jersey/issues/1700
     *
     * @param originalFileName the original filename
     * @return the filename converted to UTF_8
     */
    public static String decodeFileName(String originalFileName) {
        if (originalFileName == null) {
            return null;
        }
        return new String(originalFileName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

}
