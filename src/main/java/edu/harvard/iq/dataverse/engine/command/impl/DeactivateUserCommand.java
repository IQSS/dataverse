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
public class DeactivateUserCommand extends AbstractCommand<AuthenticatedUser> {

    private DataverseRequest request;
    private AuthenticatedUser userToDeactivate;

    public DeactivateUserCommand(DataverseRequest request, AuthenticatedUser userToDeactivate) {
        super(request, (DvObject) null);
        this.request = request;
        this.userToDeactivate = userToDeactivate;
    }

    @Override
    public AuthenticatedUser execute(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Deactivate user command can only be called by superusers.", this, null, null);
        }
        if (userToDeactivate == null) {
            throw new CommandException("Cannot deactivate user. User not found.", this);
        }
        ctxt.engine().submit(new RevokeAllRolesCommand(userToDeactivate, request));
        ctxt.authentication().removeAuthentictedUserItems(userToDeactivate);
        ctxt.notifications().findByUser(userToDeactivate.getId()).forEach(ctxt.notifications()::delete);
        userToDeactivate.setDeactivated(true);
        userToDeactivate.setDeactivatedTime(new Timestamp(new Date().getTime()));
        AuthenticatedUser deactivatedUser = ctxt.authentication().save(userToDeactivate);
        return deactivatedUser;
    }

}
