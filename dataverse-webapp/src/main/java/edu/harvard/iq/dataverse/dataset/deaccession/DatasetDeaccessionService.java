package edu.harvard.iq.dataverse.dataset.deaccession;

import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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
    public DatasetVersion deaccessVersion(long versionId, String deaccessionReason, String deaccessionForwardURLFor) {
        DatasetVersion versionToDeaccess = datasetVersionService.find(versionId);
        versionToDeaccess.setVersionNote(deaccessionReason);
        versionToDeaccess.setArchiveNote(deaccessionForwardURLFor);

        return commandEngine.submit(new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), versionToDeaccess));
    }

    public List<DatasetVersion> deaccessVersions(List<DatasetVersion> versions, String deaccessionReason ,String deaccessionForwardURLFor) {
        List<DatasetVersion> deaccessionedVersions = new ArrayList<>();
        for (DatasetVersion version : versions) {
            deaccessionedVersions.add(deaccessVersion(version.getId(), deaccessionReason, deaccessionForwardURLFor));
        }
        return deaccessionedVersions;
    }

    public List<DatasetVersion> deaccessReleasedVersions(List<DatasetVersion> versions, String deaccessionReason ,String deaccessionForwardURLFor) {
        List<DatasetVersion> deaccessionedVersions = new ArrayList<>();
        for (DatasetVersion version : versions) {
            if(version.isReleased()) {
                deaccessionedVersions.add(deaccessVersion(version.getId(), deaccessionReason, deaccessionForwardURLFor));
            }
        }
        return deaccessionedVersions;
    }
}
