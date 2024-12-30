package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.tika.Tika;

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

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataverseFeaturedItem findById(Long id) {
        return em.find(DataverseFeaturedItem.class, id);
    }

    public DataverseFeaturedItem save(DataverseFeaturedItem dataverseFeaturedItem) {
        if (dataverseFeaturedItem.getId() == null) {
            em.persist(dataverseFeaturedItem);
            return dataverseFeaturedItem;
        } else {
            return em.merge(dataverseFeaturedItem);
        }
    }

    public InputStream getImageFileAsInputStream(DataverseFeaturedItem dataverseFeaturedItem) throws IOException {
        Path imagePath = Path.of(JvmSettings.DOCROOT_DIRECTORY.lookup(),
                JvmSettings.FEATURED_ITEMS_IMAGE_UPLOADS_DIRECTORY.lookup(),
                dataverseFeaturedItem.getDataverse().getId().toString(),
                dataverseFeaturedItem.getImageFileName());
        return Files.newInputStream(imagePath);
    }

    public void saveDataverseFeaturedItemImageFile(InputStream inputStream, String imageFileName, Long dataverseId) throws IOException {
        File tempFile = FileUtil.inputStreamToFile(inputStream);
        validateImageFile(tempFile);

        Path imageDir = createDataverseFeaturedItemImageDir(dataverseId);
        File uploadedFile = new File(imageDir.toFile(), imageFileName);

        if (!uploadedFile.exists()) {
            uploadedFile.createNewFile();
        }

        Files.copy(tempFile.toPath(), uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // TODO: Move file-specific logic out of here

    private Path createDataverseFeaturedItemImageDir(Long dataverseId) throws IOException {
        Path imagePath = Path.of(JvmSettings.DOCROOT_DIRECTORY.lookup(), JvmSettings.FEATURED_ITEMS_IMAGE_UPLOADS_DIRECTORY.lookup(), dataverseId.toString());
        Files.createDirectories(imagePath);
        return imagePath;
    }

    private void validateImageFile(File file) throws IOException, IllegalArgumentException {
        if (!isValidDataverseFeaturedItemFileType(file)) {
            throw new IllegalArgumentException(
                    BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.invalidFileType")
            );
        }
        if (!isValidDataverseFeaturedItemFileSize(file)) {
            String maxAllowedSize = getMaxAllowedDataverseFeaturedItemFileSize().toString();
            throw new IllegalArgumentException(
                    BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.fileSizeExceedsLimit", List.of(maxAllowedSize))
            );
        }
    }

    private boolean isValidDataverseFeaturedItemFileType(File file) throws IOException {
        Tika tika = new Tika();
        String mimeType = tika.detect(file);
        return mimeType != null && mimeType.startsWith("image/");
    }

    private boolean isValidDataverseFeaturedItemFileSize(File file) {
        return file.length() <= getMaxAllowedDataverseFeaturedItemFileSize();
    }

    private Integer getMaxAllowedDataverseFeaturedItemFileSize() {
        return JvmSettings.FEATURED_ITEMS_IMAGE_MAXSIZE.lookup(Integer.class);
    }
}
