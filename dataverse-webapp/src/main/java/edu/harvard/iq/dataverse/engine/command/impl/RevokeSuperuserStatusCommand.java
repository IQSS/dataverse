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
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import java.io.Serializable;

/**
 * @author Leonid Andreev
 */
// the permission annotation is open, since this is a superuser-only command - 
// and that's enforced in the command body:
@RequiredPermissions({})
public class RevokeSuperuserStatusCommand extends AbstractCommand<AuthenticatedUser> implements Serializable {

    private final AuthenticatedUser targetUser;

    public RevokeSuperuserStatusCommand(AuthenticatedUser targetUser, DataverseRequest aRequest) {
        super(aRequest, (Dataset) null);
        this.targetUser = targetUser;
    }

    @Override
    public AuthenticatedUser execute(CommandContext ctxt) {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Revoke Superuser status command can only be called by superusers.",
                    this, null, null);
        }

        try {
            targetUser.setSuperuser(false);
            ctxt.em().merge(targetUser);
            ctxt.em().flush();

            return targetUser;
        } catch (Exception e) {
            throw new CommandException("Failed to revoke the superuser status for user " + targetUser.getIdentifier(), this);
        }
    }
}
