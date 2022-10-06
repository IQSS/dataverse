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
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.common.files.mime.ImageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.PackageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static edu.harvard.iq.dataverse.common.FileSizeUtil.bytesToHumanReadable;


/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation,
 * but the old code is ported creatively on the method-by-method basis.
 *
 * @author Leonid Andreev
 */
public class FileUtil implements java.io.Serializable {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);


    /**
     * This string can be prepended to a Base64-encoded representation of a PNG
     * file in order to imbed an image directly into an HTML page using the
     * "img" tag. See also https://en.wikipedia.org/wiki/Data_URI_scheme
     */
    public static final String DATA_URI_SCHEME = "data:image/png;base64,";

    // -------------------- LOGIC --------------------

    public static String replaceExtension(String originalName, String newExtension) {
        int extensionIndex = originalName.lastIndexOf(".");
        return extensionIndex != -1
                ? originalName.substring(0, extensionIndex) + "." + newExtension
                : originalName + "." + newExtension;
    }

    public static String getFacetFileTypeForIndex(DataFile dataFile, Locale locale) {
        String fileType = dataFile.getContentType();

        if (fileType.contains(";")) {
            fileType = fileType.substring(0, fileType.indexOf(";"));
        }
        if (fileType.split("/")[0].isEmpty()) {
            return BundleUtil.getStringFromNonDefaultBundleWithLocale("application/octet-stream", "MimeTypeFacets", locale);
        }

        return Optional.ofNullable(BundleUtil.getStringFromNonDefaultBundleWithLocale(fileType, "MimeTypeFacets", locale))
                .filter(bundleName -> !bundleName.isEmpty())
                .orElse(BundleUtil.getStringFromNonDefaultBundleWithLocale("application/octet-stream", "MimeTypeFacets", locale));
    }

    // from MD5Checksum.java
    public static String calculateChecksum(Path filePath, ChecksumType checksumType) {

        try (InputStream fis = Files.newInputStream(filePath)) {
            return FileUtil.calculateChecksum(fis, checksumType);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // from MD5Checksum.java
    public static String calculateChecksum(InputStream in, ChecksumType checksumType) {
        MessageDigest md;
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
                logger.warn("Exception while closing stream: ", e);
            }
        }

        return checksumDigestToString(md.digest());
    }

    public static String calculateChecksum(byte[] dataBytes, ChecksumType checksumType) {
        MessageDigest md;
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
        for (byte digestByte : digestBytes) {
            sb.append(Integer.toString((digestByte & 0xff) + 0x100, 16).substring(1));
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
        if (mimeType == null) {
            return false;
        }

        boolean isMimeAmongIngestableAppTypes = ApplicationMimeType.retrieveIngestableMimes().stream()
                .anyMatch(appMime -> appMime.getMimeValue().equals(mimeType));

        boolean isMimeAmongIngestableTextTypes = TextMimeType.retrieveIngestableMimes().stream()
                .anyMatch(appMime -> appMime.getMimeValue().equals(mimeType));

        return isMimeAmongIngestableAppTypes || isMimeAmongIngestableTextTypes;
    }

    public static String getFilesTempDirectory() {
        String filesRootDirectory = SystemConfig.getFilesDirectoryStatic();
        String filesTempDirectory = filesRootDirectory + "/temp";

        if (!Files.exists(Paths.get(filesTempDirectory))) {
            /* Note that "createDirectories()" must be used - not
             * "createDirectory()", to make sure all the parent
             * directories that may not yet exist are created as well.
             */
            try {
                Files.createDirectories(Paths.get(filesTempDirectory));
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to create files temp directory: " + filesTempDirectory, ex);
            }
        }
        return filesTempDirectory;
    }

    public static Path limitedInputStreamToTempFile(InputStream inputStream, Long fileSizeLimit)
            throws IOException, FileExceedsMaxSizeException {
        Path tempFile = Files.createTempFile(Paths.get(getFilesTempDirectory()), "tmp", "upload");

        try (OutputStream outStream = Files.newOutputStream(tempFile)) {
            logger.info("Will attempt to save the file as: " + tempFile.toString());
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (fileSizeLimit != null && totalBytesRead > fileSizeLimit) {
                    try {
                        tempFile.toFile().delete();
                    } catch (Exception ex) {
                        logger.error("There was a problem with deleting temporary file");
                    }
                    throw new FileExceedsMaxSizeException(fileSizeLimit, MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"),
                                                                               bytesToHumanReadable(fileSizeLimit)));
                }
                outStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    public static File inputStreamToFile(InputStream inputStream) throws IOException {
        return inputStreamToFile(inputStream, 1024);
    }

    public static File inputStreamToFile(InputStream inputStream, int bufferSize) throws IOException {
        if (inputStream == null) {
            logger.info("In inputStreamToFile but inputStream was null! Returning null rather than a File.");
            return null;
        }
        File file = File.createTempFile(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] bytes = new byte[bufferSize];
            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            return file;
        }
    }

    public static String generateStorageIdentifier() {
        UUID uid = UUID.randomUUID();

        logger.trace("UUID value: {}", uid.toString());

        // last 6 bytes, of the random UUID, in hex:
        String hexRandom = uid.toString().substring(24);

        logger.trace("UUID (last 6 bytes, 12 hex digits): {}", hexRandom);

        String hexTimestamp = Long.toHexString(new Date().getTime());

        logger.trace("(not UUID) timestamp in hex: {}", hexTimestamp);

        String storageIdentifier = hexTimestamp + "-" + hexRandom;

        logger.debug("Storage identifier (timestamp/UUID hybrid): {}", storageIdentifier);
        return storageIdentifier;
    }

    public static String getCiteDataFileFilename(String fileTitle, FileCitationExtension fileCitationExtension) {
        if (fileTitle == null || fileCitationExtension == null) {
            return null;
        }
        return fileTitle.endsWith("tab")
                ? fileTitle.replaceAll("\\.tab$", fileCitationExtension.getSuffix())
                : fileTitle + fileCitationExtension.getSuffix();
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
            logger.trace("Download popup required because datasetVersion is null.");
            return false;
        }
        // 0. if version is draft then Popup "not required"
        if (!datasetVersion.isReleased()) {
            logger.trace("Download popup required because datasetVersion has not been released.");
            return false;
        }

        // 1. Guest Book:
        if (datasetVersion.getDataset() != null && datasetVersion.getDataset().getGuestbook() != null && datasetVersion.getDataset().getGuestbook().isEnabled() && datasetVersion.getDataset().getGuestbook().getDataverse() != null) {
            logger.trace("Download popup required because of guestbook.");
            return true;
        }

        logger.trace("Download popup is not required.");
        return false;
    }

    public static boolean isRequestAccessPopupRequired(FileMetadata fileMetadata) {
        Preconditions.checkNotNull(fileMetadata);
        // Each of these conditions is sufficient reason to have to
        // present the user with the popup:

        //0. if version is draft then Popup "not required"
        if (!fileMetadata.getDatasetVersion().isReleased()) {
            logger.trace("Request access popup not required because datasetVersion has not been released.");
            return false;
        }

        // 1. Terms of Use:
        FileTermsOfUse termsOfUse = fileMetadata.getTermsOfUse();
        if (termsOfUse.getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
            logger.trace("Request access popup required because file is restricted.");
            return true;
        }
        logger.trace("Request access popup is not required.");
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
            logger.debug(msg);
            return false;
        }
        return !isDownloadPopupRequired(fileMetadata.getDatasetVersion());
        /*
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
    public static String getFileDownloadUrlPath(ApiDownloadType downloadType, Long fileId, boolean gbRecordsWritten) {
        Preconditions.checkNotNull(downloadType);
        Preconditions.checkNotNull(fileId);

        String fileDownloadUrl = "/api/access/datafile/" + fileId;
        if (downloadType == ApiDownloadType.BUNDLE) {
            fileDownloadUrl = "/api/access/datafile/bundle/" + fileId;
        }
        if (downloadType == ApiDownloadType.ORIGINAL) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=original";
        }
        if (downloadType == ApiDownloadType.RDATA) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=RData";
        }
        if (downloadType == ApiDownloadType.VAR) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "/metadata";
        }
        if (downloadType == ApiDownloadType.TAB) {
            fileDownloadUrl = "/api/access/datafile/" + fileId + "?format=tab";
        }
        if (gbRecordsWritten) {
            if (fileDownloadUrl.contains("?")) {
                fileDownloadUrl += "&gbrecs=true";
            } else {
                fileDownloadUrl += "?gbrecs=true";
            }
        }
        logger.debug("Returning file download url: " + fileDownloadUrl);
        return fileDownloadUrl;
    }

    public static String getBatchFilesDownloadUrlPath(List<Long> fileIds, boolean guestbookRecordsAlreadyWritten, ApiBatchDownloadType downloadType) {

        String fileDownloadUrl = "/api/access/datafiles/" + StringUtils.join(fileIds, ',');
        if (guestbookRecordsAlreadyWritten && downloadType == ApiBatchDownloadType.DEFAULT) {
            fileDownloadUrl += "?gbrecs=true";
        } else if (guestbookRecordsAlreadyWritten && downloadType == ApiBatchDownloadType.ORIGINAL) {
            fileDownloadUrl += "?gbrecs=true&format=original";
        } else if (!guestbookRecordsAlreadyWritten && downloadType == ApiBatchDownloadType.ORIGINAL) {
            fileDownloadUrl += "?format=original";
        }

        return fileDownloadUrl;
    }

    public static String getDownloadWholeDatasetUrlPath(DatasetVersion dsv, boolean guestbookRecordsAlreadyWritten, ApiBatchDownloadType downloadType) {

        String fileDownloadUrl = String.format("/api/datasets/%s/versions/%s/files/download", dsv.getDataset().getId(), dsv.getId());

        if (guestbookRecordsAlreadyWritten && downloadType == ApiBatchDownloadType.DEFAULT) {
            fileDownloadUrl += "?gbrecs=true";
        } else if (guestbookRecordsAlreadyWritten && downloadType == ApiBatchDownloadType.ORIGINAL) {
            fileDownloadUrl += "?gbrecs=true&format=original";
        } else if (!guestbookRecordsAlreadyWritten && downloadType == ApiBatchDownloadType.ORIGINAL) {
            fileDownloadUrl += "?format=original";
        }

        return fileDownloadUrl;
    }

    /**
     * This method tells you if thumbnail generation is *supported*
     * on this type of file. i.e., if true, it does not guarantee that a thumbnail
     * can/will be generated; but it means that we can try.
     */
    public static boolean isThumbnailSupported(DataFile file) {
        if (file == null || file.isHarvested() || "".equals(file.getStorageIdentifier())) {
            return false;
        }
        String contentType = file.getContentType();

        // Some browsers (Chrome?) seem to identify FITS files as mime
        // type "image/fits" on upload; this is both incorrect (the official
        // mime type for FITS is "application/fits", and problematic: then
        // the file is identified as an image, and the page will attempt to
        // generate a preview - which of course is going to fail...
        if (ImageMimeType.FITSIMAGE.getMimeValue().equalsIgnoreCase(contentType)) {
            return false;
        }
        // besides most image/* types, we can generate thumbnails for
        // pdf and "world map" files:

        return contentType != null &&
                (contentType.startsWith("image/")
                        || contentType.equalsIgnoreCase("application/pdf")
                        || (file.isTabularData() && file.hasGeospatialTag())
                        || contentType.equalsIgnoreCase(ApplicationMimeType.GEO_SHAPE.getMimeValue()));
    }

    public static boolean isPackageFile(DataFile dataFile) {
        return PackageMimeType.DATAVERSE_PACKAGE.getMimeValue().equalsIgnoreCase(dataFile.getContentType());
    }

    public static byte[] getFileFromResources(String path) {
        return Try.of(() -> Files.readAllBytes(Paths.get(FileUtil.class.getResource(path).getPath())))
                .getOrElseThrow(throwable -> new RuntimeException("Unable to get file from resources", throwable));
    }

    // -------------------- INNER CLASSES --------------------

    public enum FileCitationExtension {
        ENDNOTE("-endnote", ".xml"),
        RIS(".ris"),
        BIBTEX(".bib");

        private final String text;
        private final String extension;

        FileCitationExtension(String text, String extension) {
            this.text = text;
            this.extension = extension;
        }

        FileCitationExtension(String extension) {
            this(StringUtils.EMPTY, extension);
        }

        public String getSuffix() {
            return text + extension;
        }

        public String getExtension() {
            return extension;
        }
    }

    public enum ApiBatchDownloadType {
        DEFAULT,
        ORIGINAL
    }

    public enum ApiDownloadType {
        DEFAULT,
        BUNDLE,
        ORIGINAL,
        RDATA,
        VAR,
        TAB
    }
}