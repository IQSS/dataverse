/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import com.google.common.collect.ImmutableSet;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Assign a in a dataverse to a user.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class AssignRoleCommand extends AbstractCommand<RoleAssignment> implements Serializable {

    private final DataverseRole role;
    private final RoleAssignee grantee;
    private final DvObject defPoint;
    private final String privateUrlToken;

    /**
     * @param anAssignee      The user being granted the role
     * @param aRole           the role being granted to the user
     * @param assignmentPoint the dataverse on which the role is granted.
     * @param aRequest
     * @param privateUrlToken An optional token used by the Private Url feature.
     */
    public AssignRoleCommand(RoleAssignee anAssignee, DataverseRole aRole, DvObject assignmentPoint, DataverseRequest aRequest, String privateUrlToken) {
        // for data file check permission on owning dataset
        super(aRequest, assignmentPoint instanceof DataFile ? assignmentPoint.getOwner() : assignmentPoint);
        role = aRole;
        grantee = anAssignee;
        defPoint = assignmentPoint;
        this.privateUrlToken = privateUrlToken;
    }

    @Override
    public RoleAssignment execute(CommandContext ctxt) {
        // TODO make sure the role is defined on the dataverse.
        RoleAssignment roleAssignment = new RoleAssignment(role, grantee, defPoint, privateUrlToken);
        return ctxt.roles().save(roleAssignment);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset

        if (defPoint instanceof Dataverse) {
            return Collections.singletonMap("", Collections.singleton(Permission.ManageDataversePermissions));
        }

        if (DataverseRolePermissionHelper.getRolesAllowedToBeAssignedByManageMinorDatasetPermissions().contains(role.getAlias())) {
            return Collections.singletonMap("",
                                            ImmutableSet.of(Permission.ManageDatasetPermissions, Permission.ManageMinorDatasetPermissions));
        }

        return Collections.singletonMap("", Collections.singleton(Permission.ManageDatasetPermissions));

    }

    @Override
    public String describe() {
        return grantee + " has been given " + role + " on " + defPoint.accept(DvObject.NameIdPrinter);
    }

    @Override
    public boolean isAllPermissionsRequired() {
        return false;
    }
}
