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

    public static List<DatasetThumbnail> getThumbnailCandidates(Dataset dataset, boolean considerDatasetLogoAsCandidate) {
        List<DatasetThumbnail> thumbnails = new ArrayList<>();
        if (dataset == null) {
            return thumbnails;
        }
        if (considerDatasetLogoAsCandidate) {
            Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
            if (Files.exists(path)) {
                logger.fine("Thumbnail created from dataset logo exists!");
                File file = path.toFile();
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String base64image = Base64.getEncoder().encodeToString(bytes);
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(FileUtil.DATA_URI_SCHEME + base64image, null);
                    thumbnails.add(datasetThumbnail);
                } catch (IOException ex) {
                    logger.info("Unable to rescale image: " + ex);
                }
            } else {
                logger.fine("There is no thumbnail created from a dataset logo");
            }
        }
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();  
            
            if (dataFile != null && ImageThumbConverter.isThumbnailAvailable(dataFile)
                    && !dataFile.isRestricted()) {                
                String imageSourceBase64 = null;
                imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(dataFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

                if (imageSourceBase64 != null) {
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, dataFile);
                    thumbnails.add(datasetThumbnail);
                }
            }           
        }
        return thumbnails;
    }

    public static DatasetThumbnail getThumbnail(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        String title = dataset.getLatestVersion().getTitle();

        Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
        if (Files.exists(path)) {
            try {
                byte[] bytes = Files.readAllBytes(path);
                String base64image = Base64.getEncoder().encodeToString(bytes);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(FileUtil.DATA_URI_SCHEME + base64image, null);
                logger.fine(title + " will get thumbnail from dataset logo.");
                return datasetThumbnail;
            } catch (IOException ex) {
                logger.fine("Unable to rescale image: " + ex);
                return null;
            }
        } else {
            DataFile thumbnailFile = dataset.getThumbnailFile();

            if (thumbnailFile == null) {
                if (dataset.isUseGenericThumbnail()) {
                    logger.fine(title + " does not have a thumbnail and is 'Use Generic'.");
                    return null;
                } else {
                    thumbnailFile = attemptToAutomaticallySelectThumbnailFromDataFiles(dataset);
                    if (thumbnailFile == null) {
                        logger.fine(title + " does not have a thumbnail available that could be selected automatically.");
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
        File thumb48 = new File(dataset.getFileSystemDirectory().toString(), File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
        boolean thumb48Deleted = thumb48.delete();
        if (originalFileDeleted && thumb48Deleted) {
            return true;
        } else {
            logger.info("One of the files wasn't deleted. Original deleted: " + originalFileDeleted + ". thumb48 deleted: " + thumb48Deleted + ".");
            return false;
        }
    }

    public static DataFile attemptToAutomaticallySelectThumbnailFromDataFiles(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        if (dataset.isUseGenericThumbnail()) {
            logger.fine("Bypassing logic to find a thumbnail because a generic icon for the dataset is desired.");
            return null;
        }
        for (FileMetadata fmd : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile testFile = fmd.getDataFile();
            String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(testFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            if (imageSourceBase64 != null && !testFile.isRestricted()) {
                return testFile;
            }
        }
        logger.fine("In attemptToAutomaticallySelectThumbnailFromDataFiles and interated through all the files but couldn't find a thumbnail.");
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
        File originalFile = new File(datasetDirectory.toString(), datasetLogoFilenameFinal);
        try {
            Files.copy(tmpFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.severe("Failed to original file from " + tmpFile.getAbsolutePath() + " to " + originalFile.getAbsolutePath() + ": " + ex);
        }
        String fileLocation = dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail;
        BufferedImage fullSizeImage = null;
        try {
            fullSizeImage = ImageIO.read(originalFile);
        } catch (IOException ex) {
            Logger.getLogger(ImageThumbConverter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        if (fullSizeImage == null) {
            logger.fine("fullSizeImage was null!");
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
        logger.fine("Thumbnail saved to " + thumbFileLocation + ". Original file was deleted: " + originalFileWasDeleted);
        return dataset;
    }

    public static InputStream getThumbnailAsInputStream(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail();
        if (datasetThumbnail == null) {
            return null;
        } else {
            String base64Image = datasetThumbnail.getBase64image();
            String leadingStringToRemove = FileUtil.DATA_URI_SCHEME;
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
            logger.fine("For dataset id " + dataset.getId() + " a thumbnail was found and is being returned.");
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
