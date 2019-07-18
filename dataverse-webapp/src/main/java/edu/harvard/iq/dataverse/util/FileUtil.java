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


import com.google.common.base.Preconditions;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.ingest.IngestReport;
import edu.harvard.iq.dataverse.ingest.IngestableDataChecker;
import edu.harvard.iq.dataverse.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.license.FileTermsOfUse.TermsOfUseType;
import io.vavr.control.Try;

import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJBException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static edu.harvard.iq.dataverse.dataaccess.S3AccessIO.S3_IDENTIFIER_PREFIX;


/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation,
 * but the old code is ported creatively on the method-by-method basis.
 *
 * @author Leonid Andreev
 */
public class FileUtil implements java.io.Serializable {
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
        STATISTICAL_FILE_EXTENSION.put("do", "application/x-stata-syntax");
        STATISTICAL_FILE_EXTENSION.put("sas", "application/x-sas-syntax");
        STATISTICAL_FILE_EXTENSION.put("sps", "application/x-spss-syntax");
        STATISTICAL_FILE_EXTENSION.put("csv", "text/csv");
        STATISTICAL_FILE_EXTENSION.put("tsv", "text/tsv");
    }

    private static MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();

    public static final String MIME_TYPE_STATA = "application/x-stata";
    public static final String MIME_TYPE_STATA13 = "application/x-stata-13";
    public static final String MIME_TYPE_STATA14 = "application/x-stata-14";
    public static final String MIME_TYPE_STATA15 = "application/x-stata-15";
    public static final String MIME_TYPE_RDATA = "application/x-rlang-transport";

    public static final String MIME_TYPE_CSV = "text/csv";
    public static final String MIME_TYPE_CSV_ALT = "text/comma-separated-values";
    public static final String MIME_TYPE_TSV = "text/tsv";
    public static final String MIME_TYPE_TSV_ALT = "text/tab-separated-values";
    public static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    public static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";


    public static final String MIME_TYPE_FITS = "application/fits";

    public static final String MIME_TYPE_ZIP = "application/zip";

    public static final String MIME_TYPE_FITSIMAGE = "image/fits";
    // SHAPE file type: 
    // this is the only supported file type in the GEO DATA class:

    public static final String MIME_TYPE_GEO_SHAPE = "application/zipped-shapefile";

    public static final String MIME_TYPE_UNDETERMINED_DEFAULT = "application/octet-stream";
    public static final String MIME_TYPE_UNDETERMINED_BINARY = "application/binary";

    public static final String SAVED_ORIGINAL_FILENAME_EXTENSION = "orig";

    public static final String MIME_TYPE_INGESTED_FILE = "text/tab-separated-values";


    /**
     * This string can be prepended to a Base64-encoded representation of a PNG
     * file in order to imbed an image directly into an HTML page using the
     * "img" tag. See also https://en.wikipedia.org/wiki/Data_URI_scheme
     */
    public static String DATA_URI_SCHEME = "data:image/png;base64,";

    public FileUtil() {
    }

    public static void copyFile(File inputFile, File outputFile) throws IOException {
        FileChannel in = null;
        WritableByteChannel out = null;

        try {
            in = new FileInputStream(inputFile).getChannel();
            out = new FileOutputStream(outputFile).getChannel();
            long bytesPerIteration = 50000;
            long start = 0;
            while (start < in.size()) {
                in.transferTo(start, bytesPerIteration, out);
                start += bytesPerIteration;
            }

        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }


    public static String getFileExtension(String fileName) {
        String ext = null;
        if (fileName.lastIndexOf(".") != -1) {
            ext = (fileName.substring(fileName.lastIndexOf(".") + 1)).toLowerCase();
        }
        return ext;
    }

    public static String replaceExtension(String originalName) {
        return replaceExtension(originalName, "tab");
    }

    public static String replaceExtension(String originalName, String newExtension) {
        int extensionIndex = originalName.lastIndexOf(".");
        if (extensionIndex != -1) {
            return originalName.substring(0, extensionIndex) + "." + newExtension;
        } else {
            return originalName + "." + newExtension;
        }
    }

    public static String getUserFriendlyFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();

        if (fileType != null) {
            if (fileType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
                return ShapefileHandler.SHAPEFILE_FILE_TYPE_FRIENDLY_NAME;
            }
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }
            try {
                return BundleUtil.getStringFromPropertyFile(fileType, "MimeTypeDisplay");
            } catch (MissingResourceException e) {
                return fileType;
            }
        }

        return fileType;
    }

    public static String getFacetFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();

        if (!StringUtil.isEmpty(fileType)) {
            if (fileType.contains(";")) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }

            try {
                return BundleUtil.getStringFromPropertyFile(fileType, "MimeTypeFacets");
            } catch (MissingResourceException e) {
                // if there's no defined "facet-friendly" form of this mime type
                // we'll truncate the available type by "/", e.g., all the 
                // unknown image/* types will become "image"; many other, quite
                // different types will all become "application" this way - 
                // but it is probably still better than to tag them all as 
                // "uknown". 
                // -- L.A. 4.0 alpha 1
                //
                // UPDATE, MH 4.9.2
                // Since production is displaying both "tabulardata" and "Tabular Data"
                // we are going to try to add capitalization here to this function
                // in order to capitalize all the unknown types that are not called
                // out in MimeTypeFacets.properties
                String typeClass = fileType.split("/")[0];
                return Character.toUpperCase(typeClass.charAt(0)) + typeClass.substring(1);
            }
        } else {
            try {
                return BundleUtil.getStringFromPropertyFile("application/octet-stream", "MimeTypeFacets");
            } catch (MissingResourceException ex) {
                logger.warning("Could not find \"" + fileType + "\" in bundle file: ");
                logger.log(Level.CONFIG, ex.getMessage(), ex);
                return null;
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
                return BundleUtil.getStringFromPropertyFile(fileType, "MimeTypeDisplay");
            } catch (MissingResourceException e) {
                return fileType;
            }
        }

        return "UNKNOWN";
    }

    public static String retestIngestableFileType(File file, String fileType) {
        IngestableDataChecker tabChecker = new IngestableDataChecker(TABULAR_DATA_FORMAT_SET);
        String newType = tabChecker.detectTabularDataFormat(file);

        return newType != null ? newType : fileType;
    }

    public static String determineFileType(File f, String fileName) throws IOException {
        String fileType = null;
        String fileExtension = getFileExtension(fileName);


        // step 1: 
        // Apply our custom methods to try and recognize data files that can be 
        // converted to tabular data, or can be parsed for extra metadata 
        // (such as FITS).
        logger.fine("Attempting to identify potential tabular data files;");
        IngestableDataChecker tabChk = new IngestableDataChecker(TABULAR_DATA_FORMAT_SET);

        fileType = tabChk.detectTabularDataFormat(f);

        logger.fine("determineFileType: tabular data checker found " + fileType);

        // step 2: If not found, check if graphml or FITS
        if (fileType == null) {
            if (isGraphMLFile(f)) {
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

        // step 3: check the mime type of this file with Jhove
        if (fileType == null) {
            JhoveFileType jw = new JhoveFileType();
            String mimeType = jw.getFileMimeType(f);
            if (mimeType != null) {
                fileType = mimeType;
            }
        }

        // step 4: 
        // Additional processing; if we haven't gotten much useful information 
        // back from Jhove, we'll try and make an educated guess based on 
        // the file extension:

        if (fileExtension != null) {
            logger.fine("fileExtension=" + fileExtension);

            if (fileType == null || fileType.startsWith("text/plain") || "application/octet-stream".equals(fileType)) {
                if (fileType != null && fileType.startsWith("text/plain") && STATISTICAL_FILE_EXTENSION.containsKey(fileExtension)) {
                    fileType = STATISTICAL_FILE_EXTENSION.get(fileExtension);
                } else {
                    fileType = determineFileTypeByExtension(fileName);
                }

                logger.fine("mime type recognized by extension: " + fileType);
            }
        } else {
            logger.fine("fileExtension is null");
        }

        // step 5: 
        // if this is a compressed file - zip or gzip - we'll check the 
        // file(s) inside the compressed stream and see if it's one of our
        // recognized formats that we want to support compressed:

        if ("application/x-gzip".equals(fileType)) {
            logger.fine("we'll run additional checks on this gzipped file.");
            // We want to be able to support gzipped FITS files, same way as
            // if they were just regular FITS files:
            FileInputStream gzippedIn = new FileInputStream(f);
            // (new FileInputStream() can throw a "filen not found" exception;
            // however, if we've made it this far, it really means that the 
            // file does exist and can be opened)
            InputStream uncompressedIn = null;
            try {
                uncompressedIn = new GZIPInputStream(gzippedIn);
                if (isFITSFile(uncompressedIn)) {
                    fileType = "application/fits-gzipped";
                }
            } catch (IOException ioex) {
                if (uncompressedIn != null) {
                    try {
                        uncompressedIn.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        if ("application/zip".equals(fileType)) {

            // Is this a zipped Shapefile?
            // Check for shapefile extensions as described here: http://en.wikipedia.org/wiki/Shapefile
            //logger.info("Checking for shapefile");

            ShapefileHandler shp_handler = new ShapefileHandler(new FileInputStream(f));
            if (shp_handler.containsShapefile()) {
                //  logger.info("------- shapefile FOUND ----------");
                fileType = ShapefileHandler.SHAPEFILE_FILE_TYPE; //"application/zipped-shapefile";
            }
        }

        logger.fine("returning fileType " + fileType);
        return fileType;
    }

    public static String determineFileTypeByExtension(String fileName) {
        logger.fine("Type by extension, for " + fileName + ": " + MIME_TYPE_MAP.getContentType(fileName));
        return MIME_TYPE_MAP.getContentType(fileName);
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
        BufferedInputStream ins = null;

        try {
            ins = new BufferedInputStream(new FileInputStream(file));
            return isFITSFile(ins);
        } catch (IOException ex) {
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
            logger.fine("attempting to read " + magicWordLength + " bytes from the FITS format candidate stream.");
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
        try {
            FileReader fileReader = new FileReader(file);
            javax.xml.stream.XMLInputFactory xmlif = javax.xml.stream.XMLInputFactory.newInstance();
            xmlif.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);

            XMLStreamReader xmlr = xmlif.createXMLStreamReader(fileReader);
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("graphml")) {
                        String schema = xmlr.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
                        logger.fine("schema = " + schema);
                        if (schema != null && schema.contains("http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd")) {
                            logger.fine("graphML is true");
                            isGraphML = true;
                        }
                    }
                    break;
                }
            }
        } catch (XMLStreamException e) {
            logger.fine("XML error - this is not a valid graphML file.");
            isGraphML = false;
        } catch (IOException e) {
            throw new EJBException(e);
        }
        logger.fine("end isGraphML()");
        return isGraphML;
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

    private static String checksumDigestToString(byte[] digestBytes) {
        StringBuilder sb = new StringBuilder();
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
        } else if (fileType.equalsIgnoreCase("application/x-stata")) {
            return ".dta";
        } else if (fileType.equalsIgnoreCase("application/x-rlang-transport")) {
            return ".RData";
        } else if (fileType.equalsIgnoreCase("text/csv")) {
            return ".csv";
        } else if (fileType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ".xlsx";
        }

        return "";
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
            case MIME_TYPE_TSV_ALT:
            case MIME_TYPE_XLSX:
            case MIME_TYPE_SPSS_SAV:
            case MIME_TYPE_SPSS_POR:
                return true;
            default:
                return false;
        }
    }

    public static String getFilesTempDirectory() {
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        String filesTempDirectory = filesRootDirectory + "/temp";

        if (!Files.exists(Paths.get(filesTempDirectory))) {
            /* Note that "createDirectories()" must be used - not
             * "createDirectory()", to make sure all the parent
             * directories that may not yet exist are created as well.
             */
            try {
                Files.createDirectories(Paths.get(filesTempDirectory));
            } catch (IOException ex) {
                logger.severe("Failed to create filesTempDirectory: " + filesTempDirectory);
                return null;
            }
        }

        return filesTempDirectory;
    }

    public static void generateS3PackageStorageIdentifier(DataFile dataFile) {
        String bucketName = System.getProperty("dataverse.files.s3-bucket-name");
        String storageId = S3_IDENTIFIER_PREFIX + "://" + bucketName + ":" + dataFile.getFileMetadata().getLabel();
        dataFile.setStorageIdentifier(storageId);
    }

    public static void generateStorageIdentifier(DataFile dataFile) {
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

    private static void createIngestReport(DataFile dataFile, int status, String message) {
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

        FileCitationExtension(final String text) {
            this.text = text;
        }
    }

    public static String getCiteDataFileFilename(String fileTitle, FileCitationExtension fileCitationExtension) {
        if ((fileTitle == null) || (fileCitationExtension == null)) {
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
        // Each of these conditions is sufficient reason to have to 
        // present the user with the popup: 
        if (datasetVersion == null) {
            logger.fine("Download popup required because datasetVersion is null.");
            return false;
        }
        //0. if version is draft then Popup "not required"
        if (!datasetVersion.isReleased()) {
            logger.fine("Download popup required because datasetVersion has not been released.");
            return false;
        }
        // 1. License and Terms of Use:
        if (datasetVersion.getTermsOfUseAndAccess() != null) {
            if (!TermsOfUseAndAccess.License.CC0.equals(datasetVersion.getTermsOfUseAndAccess().getLicense())
                    && !(datasetVersion.getTermsOfUseAndAccess().getTermsOfUse() == null
                    || datasetVersion.getTermsOfUseAndAccess().getTermsOfUse().equals(""))) {
                logger.fine("Download popup required because of license or terms of use.");
                return true;
            }

            // 2. Terms of Access:
            if (!(datasetVersion.getTermsOfUseAndAccess().getTermsOfAccess() == null) && !datasetVersion.getTermsOfUseAndAccess().getTermsOfAccess().equals("")) {
                logger.fine("Download popup required because of terms of access.");
                return true;
            }
        }

        // 3. Guest Book:
        if (datasetVersion.getDataset() != null && datasetVersion.getDataset().getGuestbook() != null && datasetVersion.getDataset().getGuestbook().isEnabled() && datasetVersion.getDataset().getGuestbook().getDataverse() != null) {
            logger.fine("Download popup required because of guestbook.");
            return true;
        }

        logger.fine("Download popup is not required.");
        return false;
    }

    public static boolean isRequestAccessPopupRequired(FileMetadata fileMetadata) {
        Preconditions.checkNotNull(fileMetadata);
        // Each of these conditions is sufficient reason to have to 
        // present the user with the popup: 

        //0. if version is draft then Popup "not required"
        if (!fileMetadata.getDatasetVersion().isReleased()) {
            logger.fine("Request access popup not required because datasetVersion has not been released.");
            return false;
        }

        // 1. Terms of Use:
        FileTermsOfUse termsOfUse = fileMetadata.getTermsOfUse();
        if (termsOfUse.getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
            logger.fine("Request access popup required because file is restricted.");
            return true;
        }


        logger.fine("Request access popup is not required.");
        return false;
    }

    /**
     * Provide download URL if no Terms of Use, no guestbook, and not
     * restricted.
     */
    public static boolean isPubliclyDownloadable(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            return false;
        }
        if (fileMetadata.getTermsOfUse().getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
            String msg = "Not publicly downloadable because the file is restricted.";
            logger.fine(msg);
            return false;
        }
        boolean popupReasons = isDownloadPopupRequired(fileMetadata.getDatasetVersion());
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
        return popupReasons != true;
    }

    /**
     * This is what the UI displays for "Download URL" on the file landing page
     * (DOIs rather than file IDs.
     */
    public static String getPublicDownloadUrl(String dataverseSiteUrl, String persistentId, Long fileId) {
        String path = null;
        if (persistentId != null) {
            path = dataverseSiteUrl + "/api/access/datafile/:persistentId?persistentId=" + persistentId;
        } else if (fileId != null) {
            path = dataverseSiteUrl + "/api/access/datafile/" + fileId;
        } else {
            logger.info("In getPublicDownloadUrl but persistentId & fileId are both null!");
        }
        return path;
    }

    /**
     * The FileDownloadServiceBean operates on file IDs, not DOIs.
     */
    public static String getFileDownloadUrlPath(String downloadType, Long fileId, boolean gbRecordsWritten) {
        String fileDownloadUrl = "/api/access/datafile/" + fileId;
        if (downloadType != null && downloadType.equals("bundle")) {
            fileDownloadUrl = "/api/access/datafile/bundle/" + fileId;
        }
        if (downloadType != null && downloadType.equals("original")) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=original";
        }
        if (downloadType != null && downloadType.equals("RData")) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=RData";
        }
        if (downloadType != null && downloadType.equals("var")) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "/metadata";
        }
        if (downloadType != null && downloadType.equals("tab")) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=tab";
        }
        if (gbRecordsWritten) {
            if (downloadType != null && (downloadType.equals("original") || downloadType.equals("RData") || downloadType.equals("tab"))) {
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
        try (OutputStream outputStream = new FileOutputStream(file)) {
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
    public static boolean isThumbnailSupported(DataFile file) {
        if (file == null) {
            return false;
        }

        if (file.isHarvested() || "".equals(file.getStorageIdentifier())) {
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

    public static boolean isPackageFile(DataFile dataFile) {
        return DataFileServiceBean.MIME_TYPE_PACKAGE_FILE.equalsIgnoreCase(dataFile.getContentType());
    }

    public static byte[] getFileFromResources(String path) {
        return Try.of(() -> Files.readAllBytes(Paths.get(FileUtil.class.getResource(path).getPath())))
                .getOrElseThrow(throwable -> new RuntimeException("Unable to get file from resources", throwable));
    }

}
