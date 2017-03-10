package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
            if (dataFile != null && dataFile.isImage()) {
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
                logger.fine(title + " does not have a thumbnail.");
                return null;
            }
            String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(thumbnailFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, thumbnailFile);
            logger.fine(title + " will get thumbnail from DataFile id " + thumbnailFile.getId());
            return datasetThumbnail;
        }
    }

    public static boolean deleteDatasetLogo(Dataset dataset) {
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
        logger.info("Thumbnail saved to " + thumbFileLocation);
        return dataset;
    }

}
