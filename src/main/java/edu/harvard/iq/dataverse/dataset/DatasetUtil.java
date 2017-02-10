package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatasetUtil {

    private static final Logger logger = Logger.getLogger(DatasetUtil.class.getCanonicalName());
    public static String datasetLogoFilename = "dataset_logo";
    public static String datasetLogoNameInGUI = BundleUtil.getStringFromBundle("dataset.thumbnailsAndWidget.thumbnailImage.nonDatasetFile");

    public static List<DatasetThumbnail> getThumbnailCandidates(Dataset dataset, boolean considerDatasetLogoAsCandidate) {
        List<DatasetThumbnail> thumbnails = new ArrayList<>();
        if (dataset == null) {
            return thumbnails;
        }
        if (considerDatasetLogoAsCandidate) {
            Path datasetLogo = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoFilename);
            if (Files.exists(datasetLogo)) {
                File file = datasetLogo.toFile();
                String imageSourceBase64 = null;
                try {
                    imageSourceBase64 = FileUtil.rescaleImage(file);
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(DatasetUtil.datasetLogoNameInGUI, imageSourceBase64, null);
                    thumbnails.add(datasetThumbnail);
                } catch (IOException ex) {
                    logger.info("Unable to rescale image: " + ex);
                }
            }
        }
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            if (dataFile != null && dataFile.isImage()) {
                String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(dataFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(fileMetadata.getLabel(), imageSourceBase64, dataFile);
                thumbnails.add(datasetThumbnail);
            }
        }
        return thumbnails;
    }

    public static DatasetThumbnail getThumbnail(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        Path datasetLogo = Paths.get(dataset.getFileSystemDirectory() + File.separator + DatasetUtil.datasetLogoFilename);
        if (Files.exists(datasetLogo)) {
            File file = datasetLogo.toFile();
//            String imageSourceBase64 = ImageThumbConverter.getImageAsBase64FromFile(file);
            String imageSourceBase64 = null;
            try {
                imageSourceBase64 = FileUtil.rescaleImage(file);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(DatasetUtil.datasetLogoNameInGUI, imageSourceBase64, null);
                return datasetThumbnail;
            } catch (IOException ex) {
                logger.info("Unable to rescale image: " + ex);
                return null;
            }
        } else {
            for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
                DataFile dataFile = fileMetadata.getDataFile();
                if (dataFile.equals(dataset.getThumbnailFile())) {
                    String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(dataFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                    String filename = fileMetadata.getLabel();
                    logger.info("dataset.getThumbnailFile() is equal to " + filename);
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(filename, imageSourceBase64, dataFile);
                    return datasetThumbnail;
                }
            }
            logger.info("No dataset logo and no dataset file as a thumbnail. Returning null");
            return null;
        }
    }

    public static Dataset writeDatasetLogoToDisk(Dataset dataset, File file) {
        if (dataset == null) {
            return null;
        }
        if (file == null) {
            return dataset;
        }
//        String base64image = ImageThumbConverter.getImageAsBase64FromFile(file);
//        DatasetThumbnail datasetThumbnail = new DatasetThumbnail(file.getName(), base64image);
//        DatasetThumbnail datasetThumbnail = new DatasetThumbnail(datasetLogoNameInGUI, base64image, null);
//        dataset.setDatasetThumbnail(datasetThumbnail);
        // copied from IngestServiceBean.addFiles
        try {
            if (dataset.getFileSystemDirectory() != null && !Files.exists(dataset.getFileSystemDirectory())) {
                /**
                 * Note that "createDirectories()" must be used - not
                 * "createDirectory()", to make sure all the parent directories
                 * that may not yet exist are created as well.
                 */
                Path directoryCreated = Files.createDirectories(dataset.getFileSystemDirectory());
                logger.fine("Dataset directory created: " + directoryCreated);
            }
        } catch (IOException ex) {
            logger.severe("Failed to create dataset directory " + dataset.getFileSystemDirectory() + " - " + ex);
            return dataset;
        }
        File newFile = new File(dataset.getFileSystemDirectory().toString(), datasetLogoFilename);
        try {
            Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.severe("Failed to copy file to " + dataset.getFileSystemDirectory() + ": " + ex);
            return dataset;
        }
        return dataset;
    }

    public static boolean deleteDatasetLogo(Dataset dataset) {
        File doomed = new File(dataset.getFileSystemDirectory().toString(), datasetLogoFilename);
        return doomed.delete();
    }

}
