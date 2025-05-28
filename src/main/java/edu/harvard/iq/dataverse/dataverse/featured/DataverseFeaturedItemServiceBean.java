package edu.harvard.iq.dataverse.dataverse.featured;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
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

    public List<DataverseFeaturedItem> findAllByDataverseOrdered(User user, Dataverse dataverse, boolean filter) {
        List<DataverseFeaturedItem> items = em
                .createNamedQuery("DataverseFeaturedItem.findByDataverseOrderedByDisplayOrder", DataverseFeaturedItem.class)
                .setParameter("dataverse", dataverse)
                .getResultList();
        List<DataverseFeaturedItem> filteredList = Lists.newArrayList(items);

        if (filter) {
            // filter the list by removing any items with dvObjects that should not be shown
            for (DataverseFeaturedItem item : items) {
                if (item.getDvObject() != null) {
                    DataverseRequest req = new DataverseRequest(user, (HttpServletRequest) null);
                    if ("datafile".equals(item.getType())) {
                        final DataFile datafile = fileService.find(item.getDvObject().getId());
                        if (datafile == null || (datafile.isRestricted() && !userHasPermission(req, datafile, Permission.DownloadFile))) {
                            filteredList.remove(item);
                        }
                    } else if ("dataset".equals(item.getType())) {
                        final Dataset dataset = datasetService.find(item.getDvObject().getId());
                        if (dataset == null || (dataset.isDeaccessioned() && !userHasPermission(req, dataset, Permission.ViewUnpublishedDataset))) {
                            filteredList.remove(item);
                        }
                    }
                }
            }
        }
        return filteredList;
    }
    private boolean userHasPermission(DataverseRequest req, DvObject dvObject, Permission permission) {
        return req.getUser() == null || dvObject == null ? false : permissionService.hasPermissionsFor(req, dvObject, EnumSet.of(permission));
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
