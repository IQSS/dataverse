package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class DeleteExplicitGroupCommand extends AbstractVoidCommand {
    
    private final ExplicitGroup explicitGroup;
    
    public DeleteExplicitGroupCommand(User aUser, ExplicitGroup anExplicitGroup) {
        super(aUser, anExplicitGroup.getOwner());
        explicitGroup = anExplicitGroup;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // Remove this group from all explicit groups it belongs to.
        ctxt.em().createNativeQuery(
                "DELETE FROM explicitgroup_explicitgroup "
              + "WHERE containedexplicitgroups_id=" + explicitGroup.getId()
        ).executeUpdate();
        
        ctxt.explicitGroups().removeGroup( explicitGroup );
        
    }
    
}
