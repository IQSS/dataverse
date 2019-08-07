package edu.harvard.iq.dataverse.engine.command.impl;

import com.google.common.collect.ImmutableSet;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Revokes a role for a user on a dataverse.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class RevokeRoleCommand extends AbstractVoidCommand {

    private final RoleAssignment toBeRevoked;

    public RevokeRoleCommand(RoleAssignment toBeRevoked, DataverseRequest aRequest) {
        // for data file check permission on owning dataset
        super(aRequest, toBeRevoked.getDefinitionPoint() instanceof DataFile ? toBeRevoked.getDefinitionPoint().getOwner() : toBeRevoked.getDefinitionPoint());
        this.toBeRevoked = toBeRevoked;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.roles().revoke(toBeRevoked);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset

        if (toBeRevoked.getDefinitionPoint() instanceof Dataverse) {
            return Collections.singletonMap("", Collections.singleton(Permission.ManageDataversePermissions));
        }
        if (DataverseRolePermissionHelper.getRolesAllowedToBeAssignedByManageMinorDatasetPermissions().contains(toBeRevoked.getRole().getAlias())) {
            return Collections.singletonMap("",
                                            ImmutableSet.of(Permission.ManageDatasetPermissions, Permission.ManageMinorDatasetPermissions));
        }

        return Collections.singletonMap("", Collections.singleton(Permission.ManageDatasetPermissions));
    }

    @Override
    public boolean isAllPermissionsRequired() {
        return false;
    }
}
