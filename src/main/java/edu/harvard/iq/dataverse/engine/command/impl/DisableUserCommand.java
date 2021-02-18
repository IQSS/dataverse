package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.sql.Timestamp;
import java.util.Date;

// Superuser-only enforced below.
@RequiredPermissions({})
public class DisableUserCommand extends AbstractCommand<AuthenticatedUser> {

    private DataverseRequest request;
    private AuthenticatedUser userToDisable;

    public DisableUserCommand(DataverseRequest request, AuthenticatedUser userToDisable) {
        super(request, (DvObject) null);
        this.request = request;
        this.userToDisable = userToDisable;
    }

    @Override
    public AuthenticatedUser execute(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Disable user command can only be called by superusers.", this, null, null);
        }
        if (userToDisable == null) {
            throw new CommandException("Cannot disable user. User not found.", this);
        }
        ctxt.engine().submit(new RevokeAllRolesCommand(userToDisable, request));
        ctxt.authentication().removeAuthentictedUserItems(userToDisable);
        ctxt.notifications().findByUser(userToDisable.getId()).forEach(ctxt.notifications()::delete);
        userToDisable.setDisabled(true);
        userToDisable.setDisabledTime(new Timestamp(new Date().getTime()));
        AuthenticatedUser disabledUser = ctxt.authentication().save(userToDisable);
        return disabledUser;
    }

}
