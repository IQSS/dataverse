package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * Create a new role in a dataverse.
 *
 * @author michael
 */
@RequiredPermissions(Permission.ManageDataversePermissions)
public class CreateRoleCommand extends AbstractCommand<DataverseRole> {

    private final DataverseRole created;
    private final Dataverse dv;

    public CreateRoleCommand(DataverseRole aRole, DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        created = aRole;
        dv = anAffectedDataverse;
    }

    @Override
    public DataverseRole execute(CommandContext ctxt) throws CommandException {
        User user = getUser();
        //todo: temporary for 4.0 - only superusers can create and edit roles
        if ((!(user instanceof AuthenticatedUser) || !user.isSuperuser())) {
            throw new IllegalCommandException("Roles can only be created or edited by superusers.",this);
        }

        dv.addRole(created);
        return ctxt.roles().save(created);
    }
    
}
