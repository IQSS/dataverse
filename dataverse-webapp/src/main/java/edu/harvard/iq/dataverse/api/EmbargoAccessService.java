package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Logger;

@Stateless
public class EmbargoAccessService extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(EmbargoAccessService.class.getCanonicalName());

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
