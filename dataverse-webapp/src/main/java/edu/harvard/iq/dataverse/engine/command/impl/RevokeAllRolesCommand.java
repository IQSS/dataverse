/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.io.Serializable;

/**
 * Revokes all roles for a assignee.
 * (Note that in addition to deleting the explicit role assignments,
 * it also strips the assignee of membership in any groups!)
 *
 * @author Leonid Andreev
 */
// the permission annotation is open, since this is a superuser-only command - 
// and that's enforced in the command body:
@RequiredPermissions({})
public class RevokeAllRolesCommand extends AbstractCommand<AuthenticatedUser> implements Serializable {

    private final AuthenticatedUser assignee;

    public RevokeAllRolesCommand(AuthenticatedUser assignee, DataverseRequest aRequest) {
        super(aRequest, (Dataverse) null);
        this.assignee = assignee;
    }

    @Override
    public AuthenticatedUser execute(CommandContext ctxt) {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Revoke Superuser status command can only be called by superusers.",
                    this, null, null);
        }

        try {
            ctxt.roles().revokeAll(assignee);

            ctxt.explicitGroups().revokeAllGroupsForAssignee(assignee);
            return assignee;

        } catch (Exception ex) {
            throw new CommandException("Failed to revoke role assignments and/or group membership", this);
        }
    }
}
