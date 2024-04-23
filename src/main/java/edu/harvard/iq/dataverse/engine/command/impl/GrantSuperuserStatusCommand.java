/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;

/**
 *
 * @author Leonid Andreev
 */
// the permission annotation is open, since this is a superuser-only command - 
// and that's enforced in the command body:
@RequiredPermissions({})
public class GrantSuperuserStatusCommand extends AbstractVoidCommand  {

    private final AuthenticatedUser targetUser;
    
    public GrantSuperuserStatusCommand (AuthenticatedUser targetUser, DataverseRequest aRequest) {
        super(aRequest, (Dataset)null);
        this.targetUser = targetUser;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Revoke Superuser status command can only be called by superusers.",
                    this, null, null);
        }

        if (targetUser.isDeactivated()) {
            throw new CommandException("User " + targetUser.getIdentifier() + " has been deactivated and cannot become a superuser.", this);
        }

        try {
            targetUser.setSuperuser(true);
            ctxt.em().merge(targetUser);
            ctxt.em().flush();
        } catch (Exception e) {
            throw new CommandException("Failed to grant the superuser status to user "+targetUser.getIdentifier(), this);
        }
    }
}
