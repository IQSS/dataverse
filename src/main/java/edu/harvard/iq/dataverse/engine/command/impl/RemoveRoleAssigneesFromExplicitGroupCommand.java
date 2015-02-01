package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Set;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class RemoveRoleAssigneesFromExplicitGroupCommand extends AbstractCommand<ExplicitGroup>{
    
    private final Set<String> roleAssigneeIdentifiers;
    private final ExplicitGroup explicitGroup;
    
    public RemoveRoleAssigneesFromExplicitGroupCommand(User aUser, ExplicitGroup anExplicitGroup, Set<String> someRoleAssigneeIdentifiers ) {
        super(aUser, anExplicitGroup.getOwner());
        roleAssigneeIdentifiers = someRoleAssigneeIdentifiers;
        explicitGroup = anExplicitGroup;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt) throws CommandException {
        for ( String rai : roleAssigneeIdentifiers ) {
            explicitGroup.removeByRoleAssgineeIdentifier(rai);
        }
        return ctxt.explicitGroups().persist(explicitGroup);
    }
    
}
