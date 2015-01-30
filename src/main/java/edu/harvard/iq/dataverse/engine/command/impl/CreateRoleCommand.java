package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;

/**
 * Create a new role in a dataverse.
 *
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class CreateRoleCommand extends AbstractCommand<DataverseRole> {

    private final DataverseRole created;
    private final Dataverse dv;
    private final User user;

    public CreateRoleCommand(DataverseRole aRole, User aUser, Dataverse anAffectedDataverse) {
        super(aUser, anAffectedDataverse);
        created = aRole;
        dv = anAffectedDataverse;
        user = aUser;
    }

    @Override
    public DataverseRole execute(CommandContext ctxt) throws CommandException {

        //todo: temporary for 4.0 - only superusers can create and edit roles
        if ((!(user instanceof AuthenticatedUser) || !((AuthenticatedUser) user).isSuperuser())) {
            throw new CommandException("Roles can only be created or edited by superusers.",this);
        }

        dv.addRole(created);
        return ctxt.roles().save(created);
    }

}
