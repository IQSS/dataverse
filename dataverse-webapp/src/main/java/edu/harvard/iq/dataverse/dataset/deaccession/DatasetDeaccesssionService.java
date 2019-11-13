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
public class DatasetDeaccesssionService {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private DatasetVersionServiceBean datasetVersionService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DatasetDeaccesssionService() {
    }

    @Inject
    public DatasetDeaccesssionService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                                      DatasetVersionServiceBean datasetVersionService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.datasetVersionService = datasetVersionService;
    }

    // -------------------- LOGIC --------------------
    public DatasetVersion deaccessVersion(DatasetVersion version) {
        return commandEngine.submit(new DeaccessionDatasetVersionCommand(dvRequestService.getDataverseRequest(), version));
    }

    public List<DatasetVersion> deaccessVersions(List<DatasetVersion> versions, String deaccessionReason ,String deaccessionForwardURLFor) {
        List<DatasetVersion> deaccessionedVersions = new ArrayList<>();
        for (DatasetVersion version : versions) {
            deaccessionedVersions.add(deaccessSingleVersion(version, deaccessionReason, deaccessionForwardURLFor));
        }
        return deaccessionedVersions;
    }

    public List<DatasetVersion> deaccessReleasedVersions(List<DatasetVersion> versions, String deaccessionReason ,String deaccessionForwardURLFor) {
        List<DatasetVersion> deaccessionedVersions = new ArrayList<>();
        for (DatasetVersion version : versions) {
            if(version.isReleased()) {
                deaccessionedVersions.add(deaccessSingleVersion(version, deaccessionReason, deaccessionForwardURLFor));
            }
        }
        return deaccessionedVersions;
    }

    // -------------------- PRIVATE ---------------------
    private DatasetVersion deaccessSingleVersion(DatasetVersion version, String deaccessionReason,
                                                 String deaccessionForwardURLFor) {
        DatasetVersion versionToDeaccess = datasetVersionService.find(version.getId());
        updateDeaccessionReasonAndURL(versionToDeaccess, deaccessionReason, deaccessionForwardURLFor);
        return deaccessVersion(versionToDeaccess);
    }

    private void updateDeaccessionReasonAndURL(DatasetVersion datasetVersion, String deaccessionReason ,String deaccessionForwardURLFor) {
        datasetVersion.setVersionNote(deaccessionReason);
        datasetVersion.setArchiveNote(deaccessionForwardURLFor);
    }
}
