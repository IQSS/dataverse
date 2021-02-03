package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class EmbargoAccessService {
    private PermissionServiceBean permissionService;
    private DataverseRequestServiceBean dvRequestService;
    

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public EmbargoAccessService() {
    }

    @Inject
    public EmbargoAccessService(PermissionServiceBean permissionService, DataverseRequestServiceBean dvRequestService) {
        this.permissionService = permissionService;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- LOGIC --------------------
    public boolean isRestrictedByEmbargo(Dataset dataset) {
        return dataset.hasActiveEmbargo() && !permissionService.requestOn(dvRequestService.getDataverseRequest(), dataset).has(Permission.ViewUnpublishedDataset);
    }

}
