package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class DatasetDeaccessionService {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private DatasetVersionServiceBean datasetVersionService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DatasetDeaccessionService() {
    }

    @Inject
    public DatasetDeaccessionService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                                     DatasetVersionServiceBean datasetVersionService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.datasetVersionService = datasetVersionService;
    }

    // -------------------- LOGIC --------------------
    public DatasetVersion deaccessVersion(DatasetVersion version, String deaccessionReason, String deaccessionForwardURLFor) {
        version.setVersionNote(deaccessionReason);
        version.setArchiveNote(deaccessionForwardURLFor);

        return commandEngine.submit(new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), version));
    }

    public List<DatasetVersion> deaccessVersions(List<DatasetVersion> versions, String deaccessionReason ,String deaccessionForwardURLFor) {
        return versions
                .stream()
                .map(version -> deaccessVersion(version, deaccessionReason, deaccessionForwardURLFor)).collect(Collectors.toList());
    }

    public List<DatasetVersion> deaccessReleasedVersions(List<DatasetVersion> versions, String deaccessionReason ,String deaccessionForwardURLFor) {
        return versions
                .stream()
                .filter(DatasetVersion::isReleased)
                .map(version -> deaccessVersion(version, deaccessionReason, deaccessionForwardURLFor)).collect(Collectors.toList());
    }
}
