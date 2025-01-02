package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
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

@Stateless
@Named
public class DataverseFeaturedItemServiceBean implements Serializable {

    public static class InvalidImageFileException extends Exception {
        public InvalidImageFileException(String message) {
            super(message);
        }
    }

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataverseFeaturedItem findById(Long id) {
        return em.find(DataverseFeaturedItem.class, id);
    }

    public DataverseFeaturedItem save(DataverseFeaturedItem dataverseFeaturedItem) {
        if (dataverseFeaturedItem.getId() == null) {
            em.persist(dataverseFeaturedItem);
        } else {
            dataverseFeaturedItem = em.merge(dataverseFeaturedItem);
        }
        em.flush();
        return dataverseFeaturedItem;
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
