package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
