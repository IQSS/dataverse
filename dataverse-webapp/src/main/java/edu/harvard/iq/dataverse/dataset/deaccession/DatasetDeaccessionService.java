package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.annotations.processors.permissions.extractors.DatasetFromVersion;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBeanResolver;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class DatasetDeaccessionService {

    private static final Logger log = LoggerFactory.getLogger(DatasetDeaccessionService.class);

    @PersistenceContext
    private EntityManager em;

    @Inject
    private IndexServiceBean indexService;

    @Inject
    private EjbDataverseEngine commandEngine;

    @Inject
    private DataverseRequestServiceBean dvRequestService;

    @Inject
    private GlobalIdServiceBeanResolver globalIdServiceBeanResolver;

    // -------------------- LOGIC --------------------

    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.PublishDataset}))
    public List<DatasetVersion> deaccessVersions(
            @PermissionNeeded(extractor = DatasetFromVersion.class) List<DatasetVersion> versions,
            String deaccessionReason , String deaccessionForwardURLFor) {
        return versions.stream()
                .map(v -> deaccessDatasetVersion(v, deaccessionReason, deaccessionForwardURLFor))
                .collect(Collectors.toList());
    }

    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.PublishDataset}))
    public List<DatasetVersion> deaccessReleasedVersions(
            @PermissionNeeded(extractor = DatasetFromVersion.class) List<DatasetVersion> versions,
            String deaccessionReason, String deaccessionForwardURLFor) {
        return versions.stream()
                .filter(DatasetVersion::isReleased)
                .map(v -> deaccessDatasetVersion(v, deaccessionReason, deaccessionForwardURLFor))
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private DatasetVersion deaccessDatasetVersion(DatasetVersion version, String deaccessionReason, String deaccessionForwardURLFor) {
        version.setVersionNote(deaccessionReason);
        version.setArchiveNote(deaccessionForwardURLFor);
        version.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        version.setLastUpdateTime(new Date());
        DatasetVersion merged = em.merge(version);

        Dataset dataset = merged.getDataset();
        indexService.indexDataset(dataset, true);
        em.merge(dataset);

        dataset.getFiles().stream()
                .filter(df -> df.getFileMetadatas().stream()
                        .allMatch(fm -> fm.getDatasetVersion().isDeaccessioned()))
                .forEach(this::unregisterGlobalId);

        if (dataset.isDeaccessioned()) {
            unregisterGlobalId(dataset);
        }

        return merged;
    }

    private void unregisterGlobalId(DvObject object) {
        if (object.isIdentifierRegistered()) {
            Option.of(globalIdServiceBeanResolver.resolve(object.getProtocol())).toTry()
                    .andThenTry(service -> service.deleteIdentifier(object))
                    .onFailure(e -> log.warn("Failed to unregister identifier {}", object.getGlobalId(), e));
        }
    }
}
