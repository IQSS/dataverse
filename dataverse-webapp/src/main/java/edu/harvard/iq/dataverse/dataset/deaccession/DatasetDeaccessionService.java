package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.IndexServiceBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class DatasetDeaccessionService {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private IndexServiceBean indexService;

    // -------------------- LOGIC --------------------
    
    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.PublishDataset}))
    public List<DatasetVersion> deaccessVersions(@PermissionNeeded Dataset dataset, List<DatasetVersion> versions,
                                                 String deaccessionReason , String deaccessionForwardURLFor) {
        return versions.stream()
                .map(v -> deaccessDatasetVersion(dataset, v, deaccessionReason, deaccessionForwardURLFor))
                .collect(Collectors.toList());
    }

    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.PublishDataset}))
    public List<DatasetVersion> deaccessReleasedVersions(@PermissionNeeded Dataset dataset, List<DatasetVersion> versions,
                                                         String deaccessionReason ,String deaccessionForwardURLFor) {
        return versions.stream()
                .filter(DatasetVersion::isReleased)
                .map(v -> deaccessDatasetVersion(dataset, v, deaccessionReason, deaccessionForwardURLFor))
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private DatasetVersion deaccessDatasetVersion(Dataset containingDataset, DatasetVersion deaccessionVersion,
                                                 String deaccessionReason, String deaccessionForwardURLFor)  {
        if (containingDataset == null || !containingDataset.equals(deaccessionVersion.getDataset())) {
            throw new RuntimeException("Provided dataset is not equal to the dataset of version being deaccessed.");
        }

        deaccessionVersion.setVersionNote(deaccessionReason);
        deaccessionVersion.setArchiveNote(deaccessionForwardURLFor);
        deaccessionVersion.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        DatasetVersion merged = em.merge(deaccessionVersion);

        Dataset dataset = merged.getDataset();
        indexService.indexDataset(dataset, true);
        em.merge(dataset);
        return merged;
    }
}
