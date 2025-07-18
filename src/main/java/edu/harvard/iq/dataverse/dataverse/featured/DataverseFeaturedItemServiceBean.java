package edu.harvard.iq.dataverse.dataverse.featured;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@Named
public class DataverseFeaturedItemServiceBean implements Serializable {
    private static final Logger logger = Logger.getLogger(DataverseFeaturedItemServiceBean.class.getCanonicalName());

    public static class InvalidImageFileException extends Exception {
        public InvalidImageFileException(String message) {
            super(message);
        }
    }

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @EJB
    protected DataFileServiceBean fileService;
    @EJB
    protected DatasetServiceBean datasetService;
    @EJB
    protected PermissionServiceBean permissionService;

    public DataverseFeaturedItem findById(Long id) {
        return em.find(DataverseFeaturedItem.class, id);
    }

    public DataverseFeaturedItem save(DataverseFeaturedItem dataverseFeaturedItem) {
        if (dataverseFeaturedItem.getId() == null) {
            em.persist(dataverseFeaturedItem);
            em.flush();
        } else {
            dataverseFeaturedItem = em.merge(dataverseFeaturedItem);
        }
        return dataverseFeaturedItem;
    }

    public void delete(Long id) {
        em.createNamedQuery("DataverseFeaturedItem.deleteById", DataverseFeaturedItem.class)
                .setParameter("id", id)
                .executeUpdate();
    }

    public void deleteAllByDvObjectId(Long id) {
        em.createNamedQuery("DataverseFeaturedItem.deleteByDvObjectId", DataverseFeaturedItem.class)
                .setParameter("id", id)
                .executeUpdate();
    }

    public void deleteInvalidatedFeaturedItemsByDataset(Dataset dataset) {
        // Delete any Featured Items that contain Datafiles that were removed or restricted in the latest published version
        List<DataverseFeaturedItem> featuredItems = findAllByDataverseOrdered(dataset.getOwner());
        DatasetVersion latestVersion = dataset.getLatestVersion();
        
        for (DataverseFeaturedItem featuredItem : featuredItems) {
            if (featuredItem.getDvObject() != null && featuredItem.getType().equalsIgnoreCase(DataverseFeaturedItem.TYPES.DATAFILE.name())) {
                DataFile df = (DataFile) featuredItem.getDvObject();
                
                // Check if the file is restricted or deleted
                if (df.isRestricted() || df.isInDatasetVersion(latestVersion)) {
                    logger.fine("Deleting invalidated Featured Item for " + (df.isRestricted() ? "Restricted" : "Deleted") + " Datafile ID: " + df.getId());
                    deleteAllByDvObjectId(df.getId());
                    continue;
                }
            }
        }
    }

    public List<DataverseFeaturedItem> findAllByDataverseOrdered(Dataverse dataverse) {
        List<DataverseFeaturedItem> items = em
                .createNamedQuery("DataverseFeaturedItem.findByDataverseOrderedByDisplayOrder", DataverseFeaturedItem.class)
                .setParameter("dataverse", dataverse)
                .getResultList();
        return items;
    }

    public InputStream getImageFileAsInputStream(DataverseFeaturedItem dataverseFeaturedItem) throws IOException {
        Path imagePath = Path.of(JvmSettings.DOCROOT_DIRECTORY.lookup(),
                JvmSettings.FEATURED_ITEMS_IMAGE_UPLOADS_DIRECTORY.lookup(),
                dataverseFeaturedItem.getDataverse().getId().toString(),
                dataverseFeaturedItem.getImageFileName());
        return Files.newInputStream(imagePath);
    }

    public void saveDataverseFeaturedItemImageFile(InputStream inputStream, String imageFileName, Long dataverseId) throws IOException, InvalidImageFileException {
        File tempFile = FileUtil.inputStreamToFile(inputStream);
        validateImageFile(tempFile);

        Path imageDir = FileUtil.createDirStructure(
                JvmSettings.DOCROOT_DIRECTORY.lookup(),
                JvmSettings.FEATURED_ITEMS_IMAGE_UPLOADS_DIRECTORY.lookup(),
                dataverseId.toString()
        );
        File uploadedFile = new File(imageDir.toFile(), imageFileName);

        if (!uploadedFile.exists()) {
            uploadedFile.createNewFile();
        }

        Files.copy(tempFile.toPath(), uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void validateImageFile(File file) throws IOException, InvalidImageFileException {
        if (!FileUtil.isFileOfImageType(file)) {
            throw new InvalidImageFileException(
                    BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.invalidFileType")
            );
        }
        Integer maxAllowedSize = JvmSettings.FEATURED_ITEMS_IMAGE_MAXSIZE.lookup(Integer.class);
        if (file.length() > maxAllowedSize) {
            throw new InvalidImageFileException(
                    BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.fileSizeExceedsLimit", List.of(maxAllowedSize.toString()))
            );
        }
    }
}
