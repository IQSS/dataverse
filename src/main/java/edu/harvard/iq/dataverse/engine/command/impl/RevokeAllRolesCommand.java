/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;

/**
 *
 * Revokes all roles for a assignee.
 * (Note that in addition to deleting the explicit role assignments, 
 * it also strips the assignee of membership in any groups!)
 * @author Leonid Andreev
 */
// the permission annotation is open, since this is a superuser-only command - 
// and that's enforced in the command body:
@RequiredPermissions({})
public class RevokeAllRolesCommand extends AbstractVoidCommand {

    private final RoleAssignee assignee;

    public RevokeAllRolesCommand(RoleAssignee assignee, DataverseRequest aRequest) {
        super(aRequest, (Dataverse)null);
        this.assignee = assignee;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Revoke Superuser status command can only be called by superusers.",
                    this, null, null);
        }
        
        try {
            ctxt.roles().revokeAll(assignee, getRequest());
            
            ctxt.explicitGroups().revokeAllGroupsForAssignee(assignee);
            
        } catch (Exception ex) {
            throw new CommandException("Failed to revoke role assignments and/or group membership", this);
        }
    }

}
