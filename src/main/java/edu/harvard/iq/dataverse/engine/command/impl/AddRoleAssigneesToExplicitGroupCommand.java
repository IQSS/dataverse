package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class AddRoleAssigneesToExplicitGroupCommand extends AbstractCommand<ExplicitGroup> {
    
    private final Set<String> roleAssigneeIdentifiers;
    private final ExplicitGroup explicitGroup;
     
    public AddRoleAssigneesToExplicitGroupCommand(DataverseRequest aRequest, ExplicitGroup anExplicitGroup, Set<String> someRoleAssigneeIdentifiers) {
        super(aRequest, anExplicitGroup.getOwner());
        roleAssigneeIdentifiers = someRoleAssigneeIdentifiers;
        explicitGroup = anExplicitGroup;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt) throws CommandException {
        
        List<String> nonexistentRAs = new LinkedList<>();
        for ( String rai : roleAssigneeIdentifiers ) {
            RoleAssignee ra = null;
            try {
                ra = ctxt.roleAssignees().getRoleAssignee(rai);
            } catch ( EJBException iae ) {
                if ( iae.getCausedByException() instanceof IllegalArgumentException ) {
                    throw new IllegalCommandException("Bad role assignee name:" + rai, this);
                }
            }
            if ( ra == null ) {
                nonexistentRAs.add( rai );
            } else {
                if (ra instanceof AuthenticatedUser) {
                    AuthenticatedUser user = (AuthenticatedUser) ra;
                    if (user.isDeactivated()) {
                        throw new IllegalCommandException("User " + user.getUserIdentifier() + " is deactivated and cannot be added to a group.", this);
                    }
                }
                try {
                    explicitGroup.add(ra);
                } catch (GroupException ex) {
                    Logger.getLogger(AddRoleAssigneesToExplicitGroupCommand.class.getName())
                            .log(Level.WARNING, "Error adding role assignee " + rai + " to group"
                                                + " " + explicitGroup.getIdentifier(), ex);
                    throw new IllegalCommandException("Error adding " + rai 
                            + " to group " + explicitGroup.getIdentifier() + ": " 
                            + ex.getMessage(), this );
                }
            }
        }
        
        if ( nonexistentRAs.isEmpty() ) {
            return ctxt.explicitGroups().persist(explicitGroup);
        } else {
            StringBuilder sb = new StringBuilder();
            for ( String s : nonexistentRAs ) {
                sb.append(s).append(", ");
            }
            sb.setLength( sb.length()-2);
            throw new IllegalCommandException("The following role assignees were not found: " + sb.toString(), this );
        }
    }
    
}
