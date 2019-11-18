package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@ViewScoped
@Named("ReorderDataFilesPage")
public class ReorderDataFilesPage implements java.io.Serializable {

    @EJB
    private DatasetServiceBean datasetService;
    @EJB
    private DatasetVersionServiceBean datasetVersionService;
    @EJB
    private PermissionServiceBean permissionService;
    @Inject
    private PermissionsWrapper permissionsWrapper;

    private DatasetVersion datasetVersion = new DatasetVersion();
    private List<FileMetadata> fileMetadatas;
    private List<FileMetadata> fileMetadatasCopy;

    /**
     * Initializes all properties requested by frontend.
     * Like files for dataset with specific id.
     *
     * @return error if something goes wrong or null if success.
     */
    public String init() {

        Optional<DatasetVersion> fetchedDatasetVersion = fetchDatasetVersion(datasetVersion.getId());

        if (!fetchedDatasetVersion.isPresent() || fetchedDatasetVersion.get().getDataset().isHarvested()) {
            return permissionsWrapper.notFound();
        }

        fileMetadatas = fetchedDatasetVersion.get().getFileMetadatasSorted();

        // for some reason the original fileMetadatas is causing null if used anywhere else. For
        fileMetadatasCopy = fileMetadatas;

        if (!permissionService.on(datasetVersion.getDataset()).has(Permission.EditDataset)) {
            return permissionsWrapper.notAuthorized();
        }

        return null;
    }

    /**
     * Reorders files display order if any were reordered, saves the changes to the database
     * and returns to the previous page.
     *
     * @return uri to previous page
     */
    public String saveFileOrder() {

        datasetVersionService.saveFileMetadata(FileMetadataOrder.reorderDisplayOrder(fileMetadatasCopy));

        return returnToPreviousPage();
    }

    /**
     * Method responsible for retrieving dataset from database.
     *
     * @param id
     * @return optional
     */
    private Optional<DatasetVersion> fetchDatasetVersion(Long id) {
        return Optional.ofNullable(id)
                .map(datasetId -> this.datasetVersion = datasetVersionService.find(datasetId));
    }

    /**
     * returns you to the dataset page.
     *
     * @return uri
     */
    public String returnToPreviousPage() {
        if (datasetVersion.isDraft()) {
            return "/dataset.xhtml?persistentId=" +
                    datasetVersion.getDataset().getGlobalId().asString() + "&version=DRAFT&faces-redirect=true";
        }
        return "/dataset.xhtml?persistentId=" +
                datasetVersion.getDataset().getGlobalId().asString()
                + "&faces-redirect=true&version="
                + datasetVersion.getVersionNumber() + "." + datasetVersion.getMinorVersionNumber();
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }
}
