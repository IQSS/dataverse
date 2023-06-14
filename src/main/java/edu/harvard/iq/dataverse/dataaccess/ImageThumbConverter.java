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
package edu.harvard.iq.dataverse.dataaccess;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Iterator;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
//import org.primefaces.util.Base64;
import java.util.Base64;

/**
 *
 * @author Leonid Andreev
 */
public class ImageThumbConverter {
    public static String THUMBNAIL_SUFFIX = "thumb";
    public static String THUMBNAIL_MIME_TYPE = "image/png";
    public static String THUMBNAIL_FILE_EXTENSION = ".png";

    public static int DEFAULT_CARDIMAGE_SIZE = 48;
    public static int DEFAULT_THUMBNAIL_SIZE = 64;
    public static int DEFAULT_DATASETLOGO_SIZE = 140;
    public static int DEFAULT_PREVIEW_SIZE = 400;

    private static final Logger logger = Logger.getLogger(ImageThumbConverter.class.getCanonicalName());

    public ImageThumbConverter() {
    }

    public static boolean isThumbnailAvailable(DataFile file) {
        return isThumbnailAvailable(file, DEFAULT_THUMBNAIL_SIZE);
    }

    public static boolean isThumbnailAvailable(DataFile file, int size) {

        try {

            StorageIO<DataFile> storageIO = file.getStorageIO();
            return isThumbnailAvailable(storageIO, size);
        } catch (IOException ioEx) {
            return false;
        }

    }

    private static boolean isThumbnailAvailable(StorageIO<DataFile> storageIO, int size) {

        if (storageIO == null || storageIO.getDvObject() == null) {
            return false;
        }

        DataFile file = storageIO.getDataFile();

        // if thumbnails are not even supported on this file type, no need
        // to check anything else:
        if (!FileUtil.isThumbnailSupported(file)) {
            logger.fine("No thumbnail support for " + file.getContentType());
            return false;
        }

        // similarly, if this is a harvested file: 
        if (file.isHarvested()) {
            logger.fine("thumbnails are not supported on harvested files at this time.");
            return false;
        }

        if (isThumbnailCached(storageIO, size)) {
            return true;
        }

        logger.fine("Checking for thumbnail, file type: " + file.getContentType());

        if (file.getContentType().substring(0, 6).equalsIgnoreCase("image/")) {
            return generateImageThumbnail(storageIO, size);
        } else if (file.getContentType().equalsIgnoreCase("application/pdf")) {
            return generatePDFThumbnail(storageIO, size);
        }

        return false;

    }

    // Note that this method works on ALL file types for which thumbnail 
    // generation is supported - image/*, pdf; 
    // not just on images! The type differentiation is handled inside 
    // isThumbnailAvailable(); if the thumbnail is not yet cached, that 
    // method will attempt to generate and cache it. And once it's cached, 
    // it is the same "auxiliary file", or an extra file with the .thumb[size]
    // extension - which is the same for all supported types.
    // Note that this method is mainly used by the data access API methods. 
    // Whenever a page needs a thumbnail, we prefer to rely on the Base64
    // string version.
    public static InputStreamIO getImageThumbnailAsInputStream(StorageIO<DataFile> storageIO, int size) {

        if (!isThumbnailAvailable(storageIO, size)) {
            return null;
        }

        // If we got that far, it's now reasonable to expect that the thumbnail 
        // has been generated cached. 
        InputStream cachedThumbnailInputStream = null;

        try {
            storageIO.open();
            cachedThumbnailInputStream = storageIO.getAuxFileAsInputStream(THUMBNAIL_SUFFIX + size);
            if (cachedThumbnailInputStream == null) {
                logger.warning("Null stream for aux object " + THUMBNAIL_SUFFIX + size);
                return null;
            }
            int cachedThumbnailSize = (int) storageIO.getAuxObjectSize(THUMBNAIL_SUFFIX + size);

            InputStreamIO inputStreamIO = new InputStreamIO(cachedThumbnailInputStream, cachedThumbnailSize);

            inputStreamIO.setMimeType(THUMBNAIL_MIME_TYPE);

            String fileName = storageIO.getFileName();
            if (fileName != null) {
                fileName = fileName.replaceAll("\\.[^\\.]*$", THUMBNAIL_FILE_EXTENSION);
                inputStreamIO.setFileName(fileName);
            }
            return inputStreamIO;
        } catch (Exception ioex) {
            if (cachedThumbnailInputStream != null) {
                try {
                    cachedThumbnailInputStream.close();
                } catch (IOException e) {
                }
            }
            return null;
        }
    }

    private static boolean generatePDFThumbnail(StorageIO<DataFile> storageIO, int size) {
        if (isPdfFileOverSizeLimit(storageIO.getDataFile().getFilesize())) {
            logger.fine("PDF file too large (" + storageIO.getDataFile().getFilesize() + " bytes) - skipping");
            return false;
        }

        // We rely on ImageMagick to convert PDFs; so if it's not installed, 
        // better give up right away: 
        if (!isImageMagickInstalled()) {
            return false;
        }

        File sourcePdfFile = null;

        // We'll to get a local Path for this file - but if that is not available 
        // (i.e., if it's a file that's stored by a driver that does not provide 
        // direct file access - e.g., swift), we'll save this PDF in a temp file, 
        // will run the ImageMagick on it, and will save its output in another temp 
        // file, and will save it as an "auxiliary" file via the driver. 
        boolean tempFilesRequired = false;
        File tempFile = null;

        try {
            Path pdfFilePath = storageIO.getFileSystemPath();
            sourcePdfFile = pdfFilePath.toFile();
            logger.fine("Opened the source pdf file as a local File.");
        } catch (UnsupportedDataAccessOperationException uoex) {
            // this means there is no direct filesystem path for this object;
            logger.fine("Could not open source pdf file as a local file - will go the temp file route.");
            tempFilesRequired = true;

        } catch (IOException ioex) {
            // this on the other hand is likely a fatal condition :(
            return false;
        }

        if (tempFilesRequired) {
            ReadableByteChannel pdfFileChannel;

            try {
                storageIO.open();
                //inputStream = storageIO.getInputStream();
                pdfFileChannel = storageIO.getReadChannel();
            } catch (Exception ioex) {
                logger.warning("caught Exception trying to open an input stream for " + storageIO.getDataFile().getStorageIdentifier());
                return false;
            }


            FileChannel tempFileChannel = null;
            try {
                tempFile = File.createTempFile("tempFileToRescale", ".tmp");
                tempFileChannel = new FileOutputStream(tempFile).getChannel();

                tempFileChannel.transferFrom(pdfFileChannel, 0, storageIO.getSize());
            } catch (IOException ioex) {
                logger.warning("GenerateImageThumb: failed to save pdf bytes in a temporary file.");
                return false;
            } finally {
                IOUtils.closeQuietly(tempFileChannel);
                IOUtils.closeQuietly(pdfFileChannel);
            }
            sourcePdfFile = tempFile;
        }

        String imageThumbFileName = generatePDFThumbnailFromFile(sourcePdfFile.getAbsolutePath(), size);

        if (imageThumbFileName == null) {
            return false;
        }

        // If there was a local Path to the permanent location of the PDF file on the 
        // filesystem, the generatePDFThumbnailFromFile() method must have already saved 
        // the generated thumbnail as that Path with the .thumb* extension. But 
        // if this file is stored without a local Path, we'll have to save the 
        // generated thumbnail with via the storage driver: 
        if (tempFilesRequired) {
            try {
                logger.fine("attempting to save generated pdf thumbnail, as AUX file " + THUMBNAIL_SUFFIX + size);
                storageIO.savePathAsAux(Paths.get(imageThumbFileName), THUMBNAIL_SUFFIX + size);

            } catch (IOException ioex) {
                logger.warning("failed to save generated pdf thumbnail, as AUX file " + THUMBNAIL_SUFFIX + size + "!");
                return false;
            }
            finally {
                tempFile.delete();
            }
        }

        return true;
    }

    private static boolean generateImageThumbnail(StorageIO<DataFile> storageIO, int size) {

        if (isImageOverSizeLimit(storageIO.getDataFile().getFilesize())) {
            logger.fine("Image file too large - skipping");
            return false;
        }

        try {
            storageIO.open();
            try(InputStream inputStream = storageIO.getInputStream()) {
              return generateImageThumbnailFromInputStream(storageIO, size, inputStream);
            }
        } catch (IOException ioex) {
            logger.warning("caught IOException trying to open an input stream for " + storageIO.getDataFile().getStorageIdentifier() + ioex);
            return false;
        }
        
    }

    /*
     * This is the actual workhorse method that does the rescaling of the full 
     * size image: 
     */
    private static boolean generateImageThumbnailFromInputStream(StorageIO<DataFile> storageIO, int size, InputStream inputStream) {

        BufferedImage fullSizeImage;

        try {
            logger.fine("attempting to read the image file with ImageIO.read(InputStream), " + storageIO.getDataFile().getStorageIdentifier());
            fullSizeImage = ImageIO.read(inputStream);
        } catch (Exception ioex) {
            logger.warning("Caught exception attempting to read the image file with ImageIO.read(InputStream)");
            return false;
        }

        if (fullSizeImage == null) {
            logger.warning("could not read image with ImageIO.read()");
            return false;
        }

        int width = fullSizeImage.getWidth(null);
        int height = fullSizeImage.getHeight(null);

        logger.fine("image dimensions: " + width + "x" + height + "(" + storageIO.getDataFile().getStorageIdentifier() + ")");

        OutputStream outputStream = null;

        // With some storage drivers, we can open a WritableChannel, or OutputStream 
        // to directly write the generated thumbnail that we want to cache; 
        // Some drivers (like Swift) do not support that, and will give us an
        // "operation not supported" exception. If that's the case, we'll have 
        // to save the output into a temp file, and then copy it over to the 
        // permanent storage using the DataAccess IO "save" command: 
        boolean tempFileRequired = false;
        File tempFile = null;

        try {
            Channel outputChannel = storageIO.openAuxChannel(THUMBNAIL_SUFFIX + size, DataAccessOption.WRITE_ACCESS);
            outputStream = Channels.newOutputStream((WritableByteChannel) outputChannel);
            logger.fine("Opened an auxiliary channel/output stream " + THUMBNAIL_SUFFIX + size + " on " + storageIO.getDataFile().getStorageIdentifier());
        } catch (Exception ioex) {
            logger.fine("Failed to open an auxiliary channel/output stream " + THUMBNAIL_SUFFIX + size + " on " + storageIO.getDataFile().getStorageIdentifier());
            tempFileRequired = true;
        }

        if (tempFileRequired) {
            try {
                tempFile = File.createTempFile("tempFileToRescale", ".tmp");
                outputStream = new FileOutputStream(tempFile);
            } catch (IOException ioex) {
                logger.fine("GenerateImageThumb: failed to open a temporary file.");
                return false;
            }
        }

        try {

            rescaleImage(fullSizeImage, width, height, size, outputStream);
            /*
            // while we are at it, let's make sure other size thumbnails are 
            // generated too:
            for (int s : (new int[]{DEFAULT_PREVIEW_SIZE, DEFAULT_THUMBNAIL_SIZE, DEFAULT_CARDIMAGE_SIZE})) {
                if (size != s && !thumbnailFileExists(fileLocation, s)) {
                    rescaleImage(fullSizeImage, width, height, s, fileLocation);
                }
            }
             */

            if (tempFileRequired) {
                storageIO.savePathAsAux(Paths.get(tempFile.getAbsolutePath()), THUMBNAIL_SUFFIX + size);

            }

        } catch (Exception ioex) {
            logger.warning("Failed to rescale and/or save the image: " + ioex.getMessage());
            return false;
        }
        finally {
            if(tempFileRequired) {
                tempFile.delete();
            }
        }

        return true;

    }

    private static boolean isThumbnailCached(StorageIO<DataFile> storageIO, int size) {
        boolean cached;
        try {
            cached = storageIO.isAuxObjectCached(THUMBNAIL_SUFFIX + size);
        } catch (Exception ioex) {
            logger.fine("caught Exception while checking for a cached thumbnail (file " + storageIO.getDataFile().getStorageIdentifier() + "): " + ioex.getMessage());
            return false;
        }

        if (cached) {
            logger.fine("thumbnail is cached for " + storageIO.getDataFile().getStorageIdentifier());
        } else {
            logger.fine("no thumbnail cached for " + storageIO.getDataFile().getStorageIdentifier());
        }

        return cached;
    }

    /**
     * This method is suitable for returning a string to embed in an HTML img
     * tag (or JSF h:graphicImage tag) because the string begins with
     * "data:image/png;base64," but it is not suitable for returning a
     * downloadable image via an API call.
     */
    public static String getImageThumbnailAsBase64(DataFile file, int size) {

        logger.fine("entering getImageThumbnailAsBase64, size " + size + ", for " + file.getStorageIdentifier());

        // if thumbnails are not even supported on this file type, no need
        // to check anything else:
        if (file == null || !FileUtil.isThumbnailSupported(file)) {
            logger.fine("No thumbnail support for " + file.getContentType());
            return null;
        }

        StorageIO<DataFile> storageIO = null;

        try {
            storageIO = file.getStorageIO();
        } catch (Exception ioEx) {
            logger.fine("Caught an exception while trying to obtain a thumbnail as Base64 string - could not open StorageIO on the datafile.");
            return null;
        }

        if (storageIO == null) {
            return null;
        }

        // skip the "isAvailable()" check - and just try to open the cached object. 
        // if we can't open it, then we'll try to generate it. In other words, we are doing it in 
        // the reverse order - and his way we can save one extra lookup, for a thumbnail 
        // that's already cached - and on some storage media (specifically, S3)
        // lookups are actually more expensive than reads. 
        // (an experiment...)
        //if (!isThumbnailAvailable(storageIO, size)) {
        //    logger.info("no thumbnail available for " + file.getStorageIdentifier());
        //    return null;
        //}
        // we are skipping this StorageIO.open() call as well - since this 
        // is another (potentially expensive) S3/swift lookup.
        //storageIO.open(); 
        
        Channel cachedThumbnailChannel = null;
        try {
            cachedThumbnailChannel = storageIO.openAuxChannel(THUMBNAIL_SUFFIX + size);
        } catch (Exception ioEx) {
            cachedThumbnailChannel = null;
        }

        if (cachedThumbnailChannel == null) {
            logger.fine("Null channel for aux object " + THUMBNAIL_SUFFIX + size);

            // try to generate, if not available: 
            boolean generated = false;
            if (file.getContentType().substring(0, 6).equalsIgnoreCase("image/")) {
                generated = generateImageThumbnail(storageIO, size);
            } else if (file.getContentType().equalsIgnoreCase("application/pdf")) {
                generated = generatePDFThumbnail(storageIO, size);
            }

            if (generated) {
                // try to open again: 
                try {
                    cachedThumbnailChannel = storageIO.openAuxChannel(THUMBNAIL_SUFFIX + size);
                } catch (Exception ioEx) {
                    cachedThumbnailChannel = null;
                }
            }

            // if still null - give up:
            if (cachedThumbnailChannel == null) {
                return null;
            }
        }

        InputStream cachedThumbnailInputStream = Channels.newInputStream((ReadableByteChannel) cachedThumbnailChannel);

        return getImageAsBase64FromInputStream(cachedThumbnailInputStream); //, cachedThumbnailSize);

    }

    private static String getImageAsBase64FromInputStream(InputStream inputStream) { //, int thumbSize) {
        try {
            if (inputStream != null) {

                byte[] buffer = new byte[8192];
                ByteArrayOutputStream cachingByteStream = new ByteArrayOutputStream();
                int bytes = 0;
                int total = 0;

                // No, you don't want to try and inputStream.read() the entire thumbSize
                // bytes at once; it's a thumbnail, but it can still be several K in size. 
                // And with some input streams - notably, with swift - you CANNOT read 
                // more than 8192 bytes in one .read().
                
                while ((bytes = inputStream.read(buffer)) > -1) {
                    cachingByteStream.write(buffer, 0, bytes);
                    total += bytes;
                }
                logger.fine("inside getImageThumbnailAsBase64FromInputStream; read " + total + " bytes of raw thumbnail image.");

                if (buffer != null) {
                    //String imageDataBase64 = Base64.encodeToString(cachingByteStream.toByteArray(), false);
                    String imageDataBase64 = Base64.getEncoder().encodeToString(cachingByteStream.toByteArray());
                    // TODO: 
                    // verify that the base64-encoded thumbnails on the dataset and dataverse pages are
                    // still working; PrimeFace's Base64 implementation was discontinued in 7.0, 
                    // in favor of java.util.Base64 available in Java 1.8. However, the former does not seem to 
                    // offer a way to generate a base64 string without line breaks - and that's how we used to generate these 
                    // thumbnail strings (the "false" argument in the commented-out line above). 
                    // Need to verify that new lines in these strings don't break the pages. 
                    return FileUtil.DATA_URI_SCHEME + imageDataBase64;
                }
            }
        } catch (IOException ex) {
            logger.warning("getImageAsBase64FromFile: Failed to read data from input stream.");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    /**
     * This method is suitable for returning a string to embed in an HTML img
     * tag (or JSF h:graphicImage tag) because the string begins with
     * "data:image/png;base64," but it is not suitable for returning a
     * downloadable image via an API call.
     */
    /*
     * This is a version of the getImageAsBase64...() method that operates on 
     * a File; it's used for generating Dataverse and Dataset thumbnails 
     * from usr-uploaded images (i.e., from files not associated with datafiles)
     */
    public static String getImageAsBase64FromFile(File imageFile) {
        InputStream imageInputStream = null;
        try {

            int imageSize = (int) imageFile.length();

            imageInputStream = new FileInputStream(imageFile);

            return getImageAsBase64FromInputStream(imageInputStream); //, imageSize);
        } catch (IOException ex) {
            // too bad - but not fatal
            logger.warning("getImageAsBase64FromFile: Failed to read data from thumbnail file");
        } finally {
            if (imageInputStream != null) {
                try {
                    imageInputStream.close();
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    /*
     * This is a version of generateImageThumbnail...() that works directly on 
     * local files, for input and output. We still need it for various places 
     * in the application - when we process uploaded images that are not 
     * datafiles, etc. 
     * 
     */
    public static String generateImageThumbnailFromFile(String fileLocation, int size) {

        String thumbFileLocation = fileLocation + ".thumb" + size;

        // see if the thumb is already generated and saved:
        if (new File(thumbFileLocation).exists()) {
            return thumbFileLocation;
        }

        // if not, let's attempt to generate the thumb:
        // (but only if the size is below the limit, or there is no limit...
        long fileSize;

        try {
            fileSize = new File(fileLocation).length();
        } catch (Exception ex) {
            fileSize = 0;
        }

        if (isImageOverSizeLimit(fileSize)) {
            return null;
        }

        try {
            logger.fine("attempting to read the image file " + fileLocation + " with ImageIO.read()");
            BufferedImage fullSizeImage = ImageIO.read(new File(fileLocation));

            if (fullSizeImage == null) {
                logger.warning("could not read image with ImageIO.read()");
                return null;
            }

            int width = fullSizeImage.getWidth(null);
            int height = fullSizeImage.getHeight(null);

            logger.fine("image dimensions: " + width + "x" + height);

            thumbFileLocation = rescaleImage(fullSizeImage, width, height, size, fileLocation);

            if (thumbFileLocation != null) {
                return thumbFileLocation;
            }
        } catch (Exception e) {
            logger.warning("Failed to read in an image from " + fileLocation + ": " + e.getMessage());
        }
        return null;

    }

    /*
     * This is another public version of generateImageThumbnail...() that works directly on 
     * local files, for input and output. This one returns the output as Base64.
     * Used by the DatasetWidgetsPage, to rescale the uploaded dataset logo. 
     * 
     */
    public static String generateImageThumbnailFromFileAsBase64(File file, int size) {
        String thumbnailFileLocation = generateImageThumbnailFromFile(file.getAbsolutePath(), size);

        if (thumbnailFileLocation != null) {
            File thumbnailFile = new File(thumbnailFileLocation);
            if (thumbnailFile.exists()) {
                return getImageAsBase64FromFile(thumbnailFile);
            }
        }
        return null;
    }

    // Public version of the rescaleImage() method; it takes the location of the output
    // file as a string argument. This method is used by external utilities for 
    // rescaling the non-datafile Dataverse and Dataset logos. 
    public static String rescaleImage(BufferedImage fullSizeImage, int width, int height, int size, String fileLocation) {
        String outputLocation = fileLocation + "." + THUMBNAIL_SUFFIX + size;
        File outputFile = new File(outputLocation);
        OutputStream outputFileStream = null;

        try {
            outputFileStream = new FileOutputStream(outputFile);
        } catch (IOException ioex) {
            logger.warning("caught IO exception trying to open output stream for " + outputLocation);
            return null;
        }

        try {
            rescaleImage(fullSizeImage, width, height, size, outputFileStream);
        } catch (Exception ioex) {
            logger.warning("caught Exceptiopn trying to create rescaled image " + outputLocation);
            return null;
        } finally {
            IOUtils.closeQuietly(outputFileStream);
        }

        return outputLocation;
    }

    private static void rescaleImage(BufferedImage fullSizeImage, int width, int height, int size, OutputStream outputStream) throws IOException {

        double scaleFactor = 0.0;
        int thumbHeight = size;
        int thumbWidth = size;

        if (width > height) {
            scaleFactor = ((double) size) / (double) width;
            thumbHeight = (int) (height * scaleFactor);
        } else {
            scaleFactor = ((double) size) / (double) height;
            thumbWidth = (int) (width * scaleFactor);
        }

        logger.fine("scale factor: " + scaleFactor);
        logger.fine("thumbnail dimensions: " + thumbWidth + "x" + thumbHeight);

        // If we are willing to spend a few extra CPU cycles to generate
        // better-looking thumbnails, we can the SCALE_SMOOTH flag. 
        // SCALE_FAST trades quality for speed. 
        //logger.fine("Start image rescaling ("+size+" pixels), SCALE_FAST used;");
        Image thumbImage = fullSizeImage.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_FAST);
        //logger.fine("Finished image rescaling.");

        // if transparency is defined, we should preserve it in the png:
        /*   
         OK, turns out *nothing* special needs to be done in order to preserve
         the transparency; the transparency is already there, because ImageIO.read()
         creates a BufferedImage with the color type BufferedImage.TYPE_INT_ARGB;
         all we need to do, is to create the output BufferedImage lowRes, 
         below, with this same color type. The transparency was getting lost 
         only because that BufferedImage was made with TYPE_INT_RGB, thus
         stripping the transparency off.
            
         BufferedImage bufferedImageForTransparency = new BufferedImage(thumbWidth, thumbgetHeight, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g2 = bufferedImageForTransparency.createGraphics();
         g2.drawImage(thumbImage, 0, 0, null);
         g2.dispose();
            
         int color = bufferedImageForTransparency.getRGB(0, 0);
            
         logger.info("color we'll be using for transparency: "+color);
            
         thumbImage = makeColorTransparent(bufferedImageForTransparency, new Color(color));
         */
        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersByFormatName("png");
        if (iter.hasNext()) {
            writer = (ImageWriter) iter.next();
        } else {
            throw new IOException("Failed to locatie ImageWriter plugin for image type PNG");
        }

        BufferedImage lowRes = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = lowRes.createGraphics();
        g2.drawImage(thumbImage, 0, 0, null);
        g2.dispose();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);) {
            
            writer.setOutput(ios);

            // finally, save thumbnail image:
            writer.write(lowRes);
            writer.dispose();

            thumbImage.flush();
            //fullSizeImage.flush();
            lowRes.flush();
        } catch (Exception ex) {
            logger.warning("Caught exception trying to generate thumbnail: " + ex.getMessage());
            throw new IOException("Caught exception trying to generate thumbnail: " + ex.getMessage());
        }
    }

    public static String generatePDFThumbnailFromFile(String fileLocation, int size) {
        logger.fine("entering generatePDFThumb");

        String thumbFileLocation = fileLocation + ".thumb" + size;

        // see if the thumb is already generated and saved:
        if (new File(thumbFileLocation).exists()) {
            return thumbFileLocation;
        }

        // it it doesn't exist yet, let's attempt to generate it:
        long sizeLimit = getThumbnailSizeLimitPDF();

        /* 
         * sizeLimit set to -1 means that generation of thumbnails on the fly 
         * is disabled: 
         */
        logger.fine("pdf size limit: " + sizeLimit);

        if (sizeLimit < 0) {
            logger.fine("returning null!");
            return null;
        }

        /* 
         * sizeLimit set to 0 means no limit - generate thumbnails on the fly
         * for all files, regardless of size. 
         */
        if (sizeLimit > 0) {
            long fileSize = 0;

            try {
                fileSize = new File(fileLocation).length();
            } catch (Exception ex) {
                // 
            }

            if (fileSize == 0 || fileSize > sizeLimit) {
                logger.fine("file size: " + fileSize + ", skipping.");
                // this file is too large, exiting.
                return null;
            }
        }

        String imageMagickExec = System.getProperty("dataverse.path.imagemagick.convert");

        if (imageMagickExec != null) {
            imageMagickExec = imageMagickExec.trim();
        }

        // default location:
        if (imageMagickExec == null || imageMagickExec.equals("")) {
            imageMagickExec = "/usr/bin/convert";
        }

        if (new File(imageMagickExec).exists()) {

            // Based on the lessons recently learned in production: 
            //  - use "-thumbnail" instead of "-resize";
            //  - use "-flatten"
            //  - use "-strip"
            //  - (maybe?) use jpeg instead of png - ?
            //
            // (what we observed in production - 3.6.3, June 2014 - was that 
            // for very large TIFF images, when processed wihout the options
            // above, thumbnails produced were still obscenely *huge*; for ex.,
            // for a 100MB 3000x4000 px. TIFF file, the 275 px. PNG thumbnail 
            // produced was 5MB! - which, for a page with multiple high-res 
            // images (Grad. School of Design dv) resulted in taking forever 
            // to load... JPG thumbnails, similarly produced, were smaller, but 
            // still unnecessarily large. I was never able to figure out what 
            // was going on (full-rez color profiles still stored in the 
            // resized version - ??), but the combination above takes care of 
            // it and brings down the thumbnail size to under 50K, were it 
            // belongs. :)
            //          -- L.A. June 2014
            String previewFileLocation = null;

            // check if the "preview size" image is already available - and 
            // if not, generate it. this 400 pixel image will be used to 
            // generate smaller-size thumbnails.
            previewFileLocation = fileLocation + ".thumb" + DEFAULT_PREVIEW_SIZE;

            if (!((new File(previewFileLocation)).exists())) {
                previewFileLocation = runImageMagick(imageMagickExec, fileLocation, DEFAULT_PREVIEW_SIZE, "pdf");
            }

            if (previewFileLocation == null) {
                return null;
            }

            if (size == DEFAULT_PREVIEW_SIZE) {
                return previewFileLocation;
            }

            // generate the thumbnail for the requested size, *using the already scaled-down
            // 400x400 png version, above*:
            if (!((new File(thumbFileLocation)).exists())) {
                thumbFileLocation = runImageMagick(imageMagickExec, previewFileLocation, thumbFileLocation, size, "png");
            }

            return thumbFileLocation;

            /*
             An alternative way of handling it: 
             while we are at it, let's generate *all* the smaller thumbnail sizes:
            for (int s : (new int[]{DEFAULT_THUMBNAIL_SIZE, DEFAULT_CARDIMAGE_SIZE})) {
                String thisThumbLocation = fileLocation + ".thumb" + s;
                if (!(new File(thisThumbLocation).exists())) {
                    thisThumbLocation = runImageMagick(imageMagickExec, previewFileLocation, thisThumbLocation, s, "png");
                }
            }
                    
            // return the location of the thumbnail for the requested size:
            if (new File(thumbFileLocation).exists()) {
                return thumbFileLocation;
            }*/
        }

        logger.fine("returning null");
        return null;

    }

    /*
    private static boolean thumbnailFileExists(String fileLocation, int size) {
        String thumbFileLocation = fileLocation + ".thumb" + size;
        return new File(thumbFileLocation).exists();
    }*/
    private static String runImageMagick(String imageMagickExec, String fileLocation, int size, String format) {
        String thumbFileLocation = fileLocation + ".thumb" + size;
        return runImageMagick(imageMagickExec, fileLocation, thumbFileLocation, size, format);
    }

    private static String runImageMagick(String imageMagickExec, String fileLocation, String thumbFileLocation, int size, String format) {
        String imageMagickCmd = null;

        if ("pdf".equals(format)) {
            imageMagickCmd = imageMagickExec + " pdf:" + fileLocation + "[0] -thumbnail " + size + "x" + size + " -flatten -strip png:" + thumbFileLocation;
        } else {
            imageMagickCmd = imageMagickExec + " " + format + ":" + fileLocation + " -thumbnail " + size + "x" + size + " -flatten -strip png:" + thumbFileLocation;
        }

        logger.fine("ImageMagick command line: " + imageMagickCmd);
        int exitValue = 1;

        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(imageMagickCmd);
            exitValue = process.waitFor();
        } catch (Exception e) {
            exitValue = 1;
        }

        if (exitValue == 0 && new File(thumbFileLocation).exists()) {
            logger.fine("returning " + thumbFileLocation);
            return thumbFileLocation;
        }

        return null;
    }

    private static boolean isImageOverSizeLimit(long fileSize) {
        return isFileOverSizeLimit("Image", fileSize);
    }

    private static boolean isPdfFileOverSizeLimit(long fileSize) {
        return isFileOverSizeLimit("PDF", fileSize);
    }

    private static boolean isFileOverSizeLimit(String fileType, long fileSize) {
        long sizeLimit = getThumbnailSizeLimit(fileType);

        /* 
         * sizeLimit set to -1 means that generation of thumbnails on the fly 
         * is disabled: 
         */
        if (sizeLimit < 0) {
            return true;
        }

        /* 
         * sizeLimit set to 0 means no limit - generate thumbnails on the fly
         * for all files, regardless of size. 
         */
        if (sizeLimit == 0) {
            return false;
        }

        if (fileSize == 0 || fileSize > sizeLimit) {
            // this is a broken file of size 0, or 
            // this file is too large - no thumbnail:
            return true;
        }

        return false;
    }

    private static long getThumbnailSizeLimitPDF() {
        return getThumbnailSizeLimit("PDF");
    }

    private static long getThumbnailSizeLimit(String type) {
        return SystemConfig.getThumbnailSizeLimit(type);
    }

    private static boolean isImageMagickInstalled() {
        return findImageMagickConvert() != null;
    }

    private static String findImageMagickConvert() {
        String imageMagickExec = System.getProperty("dataverse.path.imagemagick.convert");

        if (imageMagickExec != null) {
            imageMagickExec = imageMagickExec.trim();
        }

        // default/standard location:
        if (imageMagickExec == null || imageMagickExec.equals("")) {
            imageMagickExec = "/usr/bin/convert";
        }

        if (new File(imageMagickExec).exists()) {
            return imageMagickExec;
        }

        return null;
    }

    /*
       The method below takes a BufferedImage, and makes the specified color
       transparent. Turns out we don't really need to do this explicitly, since 
       the original transparency can easily be preserved. 
    
    private static Image makeColorTransparent(final BufferedImage im, final Color color) {
        final ImageFilter filter = new RGBImageFilter() {
            // the color we are looking for (white)... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFFFFFFFF;

            public final int filterRGB(final int x, final int y, final int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }
     */
}
