/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Assign a in a dataverse to a user.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class AssignRoleCommand extends AbstractCommand<RoleAssignment> {

    private final DataverseRole role;
    private final RoleAssignee grantee;
    private final DvObject defPoint;
    private final String privateUrlToken;

    /**
     * @param anAssignee The user being granted the role
     * @param aRole the role being granted to the user
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
    public RoleAssignment execute(CommandContext ctxt) throws CommandException {
        if (grantee instanceof AuthenticatedUser) {
            AuthenticatedUser user = (AuthenticatedUser) grantee;
            if (user.isDeactivated()) {
                throw new IllegalCommandException("User " + user.getUserIdentifier() + " is deactivated and cannot be given a role.", this);
            }
        }
        // TODO make sure the role is defined on the dataverse.
        RoleAssignment roleAssignment = new RoleAssignment(role, grantee, defPoint, privateUrlToken);
        return ctxt.roles().save(roleAssignment);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset
        return Collections.singletonMap("",
                defPoint instanceof Dataverse ? Collections.singleton(Permission.ManageDataversePermissions)
                : Collections.singleton(Permission.ManageDatasetPermissions));
    }

    @Override
    public String describe() {
        return grantee + " has been given " + role + " on " + defPoint.accept(DvObject.NameIdPrinter);
    }

}
