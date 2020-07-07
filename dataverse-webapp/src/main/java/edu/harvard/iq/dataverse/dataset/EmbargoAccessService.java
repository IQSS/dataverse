package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class EmbargoAccessService {
    private PermissionServiceBean permissionService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public EmbargoAccessService() {
    }

    @Inject
    public EmbargoAccessService(PermissionServiceBean permissionService) {
        this.permissionService = permissionService;
    }

    // -------------------- LOGIC --------------------
    public boolean isRestrictedByEmbargo(Dataset dataset) {
        return dataset.hasActiveEmbargo() && !permissionService.on(dataset).has(Permission.ViewUnpublishedDataset);
    }

}
