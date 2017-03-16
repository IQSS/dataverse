package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;
import javax.imageio.ImageIO;

public class DatasetUtil {

    private static final Logger logger = Logger.getLogger(DatasetUtil.class.getCanonicalName());
    public static String datasetLogoFilenameFinal = "dataset_logo_original";
    public static String datasetLogoThumbnail = "dataset_logo";
    public static String thumb48addedByImageThumbConverter = ".thumb48";
    public static String defaultIconAsBase64 = "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";

    public static List<DatasetThumbnail> getThumbnailCandidates(Dataset dataset, boolean considerDatasetLogoAsCandidate) {
        List<DatasetThumbnail> thumbnails = new ArrayList<>();
        if (dataset == null) {
            return thumbnails;
        }
        if (considerDatasetLogoAsCandidate) {
//            Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoFilenameFinal);
            Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
            if (Files.exists(path)) {
                logger.info("Thumbnail created from dataset logo exists!");
                File file = path.toFile();
                try {
//                    String base64image = FileUtil.rescaleImage(file);
//                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(base64image, null);
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String base64image = Base64.getEncoder().encodeToString(bytes);
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(FileUtil.rfc2397dataUrlSchemeBase64Png + base64image, null);
                    thumbnails.add(datasetThumbnail);
                } catch (IOException ex) {
                    logger.info("Unable to rescale image: " + ex);
                }
            } else {
                logger.info("There is no thumbnail created from a dataset logo");
            }
        }
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            if (dataFile != null && dataFile.isImage() && !dataFile.isRestricted()) {
                String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(dataFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, dataFile);
                thumbnails.add(datasetThumbnail);
            }
        }
        return thumbnails;
    }

    public static DatasetThumbnail getThumbnail(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        String title = dataset.getLatestVersion().getTitle();
//        Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoFilenameFinal);

        Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
        if (Files.exists(path)) {
            try {
//                File file = path.toFile();
//                String base64image = FileUtil.rescaleImage(file);
//                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(base64image, null);
                byte[] bytes = Files.readAllBytes(path);
                String base64image = Base64.getEncoder().encodeToString(bytes);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(FileUtil.rfc2397dataUrlSchemeBase64Png + base64image, null);
                logger.fine(title + " will get thumbnail from dataset logo.");
                return datasetThumbnail;
            } catch (IOException ex) {
                logger.info("Unable to rescale image: " + ex);
                return null;
            }
        } else {
            DataFile thumbnailFile = dataset.getThumbnailFile();
            if (thumbnailFile == null) {
                if (dataset.isUseGenericThumbnail()) {
                    logger.fine(title + " does not have a thumbnail and is 'Use Generic'.");
                    return null;
                } else {
                    thumbnailFile = dataset.getDefaultDatasetThumbnailFile();
                    if (thumbnailFile == null) {
                        logger.fine(title + " does not have a default thumbnail available.");
                        return null;
                    } else {
                        String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(thumbnailFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                        DatasetThumbnail defaultDatasetThumbnail = new DatasetThumbnail(imageSourceBase64, thumbnailFile);
                        logger.fine(title + " will get thumbnail through automatic selection from DataFile id " + thumbnailFile.getId());
                        return defaultDatasetThumbnail;
                    }
                }
            } else if (thumbnailFile.isRestricted()) {
                logger.fine(title + " has a thumbnail the user selected but the file must have later been restricted. Returning null.");
                return null;
            } else {
                String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(thumbnailFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                DatasetThumbnail userSpecifiedDatasetThumbnail = new DatasetThumbnail(imageSourceBase64, thumbnailFile);
                logger.fine(title + " will get thumbnail the user specified from DataFile id " + thumbnailFile.getId());
                return userSpecifiedDatasetThumbnail;

            }
        }
    }

    public static boolean deleteDatasetLogo(Dataset dataset) {
        if (dataset == null) {
            return false;
        }
        File originalFile = new File(dataset.getFileSystemDirectory().toString(), datasetLogoFilenameFinal);
        boolean originalFileDeleted = originalFile.delete();
//        File thumb48 = new File(dataset.getFileSystemDirectory().toString(), File.separator + "FIXME.thumb48");
        File thumb48 = new File(dataset.getFileSystemDirectory().toString(), File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
        boolean thumb48Deleted = thumb48.delete();
        if (originalFileDeleted && thumb48Deleted) {
            return true;
        } else {
            logger.info("One of the files wasn't deleted. Original deleted: " + originalFileDeleted + ". thumb48 deleted: " + thumb48Deleted + ".");
            return false;
        }
    }

    public static DataFile getDefaultThumbnailFile(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        if (dataset.isUseGenericThumbnail()) {
            logger.info("Bypassing logic to find a thumbnail because a generic icon for the dataset is desired.");
            return null;
        }
        for (FileMetadata fmd : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile testFile = fmd.getDataFile();
            if (testFile.isPreviewImageAvailable() && !testFile.isRestricted()) {
                return testFile;
            }
        }
        return null;
    }

    public static Dataset persistDatasetLogoToDiskAndCreateThumbnail(Dataset dataset, InputStream inputStream) {
        if (dataset == null) {
            return null;
        }
        File tmpFile = null;
        try {
            tmpFile = FileUtil.inputStreamToFile(inputStream);
        } catch (IOException ex) {
            Logger.getLogger(DatasetUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        Path datasetDirectory = dataset.getFileSystemDirectory();
        if (datasetDirectory != null && !Files.exists(datasetDirectory)) {
            try {
                Files.createDirectories(datasetDirectory);
            } catch (IOException ex) {
                Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
                logger.info("Dataset directory " + datasetDirectory + " does not exist but couldn't create it. Exception: " + ex);
                return null;
            }
        }
        /**
         * *
         * @todo Somehow the dataset logo becomes zero bytes. Do we want the
         * original file to be written to disk? Should we blow it away?
         */
        File originalFile = new File(datasetDirectory.toString(), datasetLogoFilenameFinal);
        try {
            Files.copy(tmpFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.severe("Failed to original file from " + tmpFile.getAbsolutePath() + " to " + originalFile.getAbsolutePath() + ": " + ex);
        }
        String fileLocation = dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail;
//                File tmpFile = null;
//        try {
//            tmpFile = File.createTempFile("tempFileToRescale", ".tmp");
//        } catch (IOException ex) {
//            Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
//        }
        BufferedImage fullSizeImage = null;
        try {
            fullSizeImage = ImageIO.read(originalFile);
        } catch (IOException ex) {
            Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        if (fullSizeImage == null) {
            logger.info("fullSizeImage was null!");
            return null;
        }
        int width = fullSizeImage.getWidth();
        int height = fullSizeImage.getHeight();
        FileChannel src = null;
        try {
            src = new FileInputStream(originalFile).getChannel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        FileChannel dest = null;
        try {
//            dest = new FileOutputStream(tmpFile).getChannel();
            dest = new FileOutputStream(originalFile).getChannel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        try {
            dest.transferFrom(src, 0, src.size());
        } catch (IOException ex) {
            Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        String thumbFileLocation = ImageThumbConverter.rescaleImage(fullSizeImage, width, height, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE, fileLocation);
        boolean originalFileWasDeleted = originalFile.delete();
        logger.info("Thumbnail saved to " + thumbFileLocation + ". Original file was deleted: " + originalFileWasDeleted);
        return dataset;
    }

    public static InputStream getThumbnailAsInputStream(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        byte[] defaultDatasetIconBytes = Base64.getDecoder().decode(defaultIconAsBase64);
        logger.fine("Thumbnail could not be found for dataset id " + dataset.getId() + ". Returning default icon: " + defaultIconAsBase64);
        ByteArrayInputStream defaultDatasetIconInputStream = new ByteArrayInputStream(defaultDatasetIconBytes);
        DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail();
        if (datasetThumbnail == null) {
            logger.info("For dataset id " + dataset.getId() + " returning the default dataset icon because no thumbnail could be found.");
            return defaultDatasetIconInputStream;
        } else {
            String base64Image = datasetThumbnail.getBase64image();
            String leadingStringToRemove = FileUtil.rfc2397dataUrlSchemeBase64Png;
            String encodedImg = base64Image.substring(leadingStringToRemove.length());
            byte[] decodedImg = null;
            try {
                decodedImg = Base64.getDecoder().decode(encodedImg.getBytes("UTF-8"));
                logger.fine("returning this many bytes for  " + "dataset id: " + dataset.getId() + ", persistentId: " + dataset.getIdentifier() + " :" + decodedImg.length);
            } catch (UnsupportedEncodingException ex) {
                logger.info("dataset thumbnail could not be decoded for dataset id " + dataset.getId() + ": " + ex);
                return null;
            }
            ByteArrayInputStream nonDefaultDatasetThumbnail = new ByteArrayInputStream(decodedImg);
            logger.info("For dataset id " + dataset.getId() + " a thumbnail was found and is being returned.");
            return nonDefaultDatasetThumbnail;
        }
    }

    /**
     * The dataset logo is the file that a user uploads which is *not* one of
     * the data files. Compare to the datavese logo. We do not save the original
     * file that is uploaded. Rather, we delete it after first creating at least
     * one thumbnail from it.
     */
    public static boolean isDatasetLogoPresent(Dataset dataset) {
        if (dataset == null) {
            return false;
        }
        return Files.exists(Paths.get(dataset.getFileSystemDirectory() + File.separator + datasetLogoFilenameFinal));
    }

}
