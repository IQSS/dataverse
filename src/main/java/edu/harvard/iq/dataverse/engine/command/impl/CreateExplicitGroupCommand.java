package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class CreateExplicitGroupCommand extends AbstractCommand<ExplicitGroup>{
    
    private final ExplicitGroup eg;
    private final Dataverse dv;
    
    public CreateExplicitGroupCommand(User aUser, Dataverse aDataverse, ExplicitGroup anExplicitGroup) {
        super(aUser, aDataverse);
        dv = aDataverse;
        eg = anExplicitGroup;
    }

    @Override
    public ExplicitGroup execute(CommandContext ctxt) throws CommandException {
        // make sure alias in owner is unique
        eg.setOwner(dv);
        eg.updateAlias();
        
        ExplicitGroup existing = eg.getGroupProvider().get( eg.getAlias()  );
        if ( existing != null ) {
            throw new IllegalCommandException( "Explicit group with the alias " 
                                                    + eg.getGroupAliasInOwner() + " already exists for dataverse " + dv.getId(),
                                              this);
        }
        
        // persist
        return ctxt.explicitGroups().persist(eg);
    }
    
}
