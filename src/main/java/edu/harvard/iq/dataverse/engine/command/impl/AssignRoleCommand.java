/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
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
    //Kept for convenience -could get this as the only DVObject AbstractCommand<>.getAffectedDvObjects() instead of having a local defPoint
    private final DvObject defPoint;
    private final String privateUrlToken;
    private boolean anonymizedAccess;

    /**
     * @param anAssignee The user being granted the role
     * @param aRole the role being granted to the user
     * @param assignmentPoint the dataverse on which the role is granted.
     * @param aRequest
     * @param privateUrlToken An optional token used by the Private Url feature.
     */
    public AssignRoleCommand(RoleAssignee anAssignee, DataverseRole aRole, DvObject assignmentPoint, DataverseRequest aRequest, String privateUrlToken) {
        // for data file check permission on owning dataset
        super(aRequest, assignmentPoint);
        role = aRole;
        grantee = anAssignee;
        defPoint = assignmentPoint;
        this.privateUrlToken = privateUrlToken;
        this.anonymizedAccess=false;
    }

    public AssignRoleCommand(PrivateUrlUser privateUrlUser, DataverseRole memberRole, Dataset dataset, DataverseRequest request, String privateUrlToken, boolean anonymizedAccess) {
        this(privateUrlUser, memberRole, dataset, request, privateUrlToken);
        this.anonymizedAccess= anonymizedAccess;
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
        RoleAssignment roleAssignment = new RoleAssignment(role, grantee, defPoint, privateUrlToken, anonymizedAccess);
        return ctxt.roles().save(roleAssignment);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset
        return Collections.singletonMap("",
                defPoint instanceof Dataverse ? Collections.singleton(Permission.ManageDataversePermissions)
                : defPoint instanceof Dataset ? Collections.singleton(Permission.ManageDatasetPermissions) : Collections.singleton(Permission.ManageFilePermissions));
    }

    @Override
    public String describe() {
        return grantee + " has been given " + role + " on " + defPoint.accept(DvObject.NameIdPrinter);
    }

}
