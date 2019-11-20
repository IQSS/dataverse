package edu.harvard.iq.dataverse.permission;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.Serializable;

@Stateless
public class ManagePermissionsService implements Serializable {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManagePermissionsService() {
    }

    @Inject
    public ManagePermissionsService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- LOGIC --------------------
    public RoleAssignment assignRole(DataverseRole role, RoleAssignee roleAssignee, DvObject object) {
        return commandEngine.submit(new AssignRoleCommand(roleAssignee, role, object, dvRequestService.getDataverseRequest(), null));
    }

    public void removeRoleAssignment(RoleAssignment roleAssignment) {
        commandEngine.submit(new RevokeRoleCommand(roleAssignment, dvRequestService.getDataverseRequest()));
    }

    public DataverseRole saveOrUpdateRole(DataverseRole role) {
        return commandEngine.submit(new CreateRoleCommand(role, dvRequestService.getDataverseRequest(), (Dataverse) role.getOwner()));
    }

    public Dataverse setDataverseDefaultContributorRole(DataverseRole defaultRole, Dataverse affectedDataverse) {
        return commandEngine.submit(new UpdateDataverseDefaultContributorRoleCommand(defaultRole, dvRequestService.getDataverseRequest(), affectedDataverse));
    }
}
