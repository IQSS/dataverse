package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * A command to move a {@link Dataverse} between two {@link Dataverse}s.
 * @author michael
 */

//@todo We will need to revist the permissions for move, once we add this 
//(will probably need different move commands for unplublished which checks add,
//versus published which checks publish 

@RequiredPermissionsMap({
	@RequiredPermissions( dataverseName = "moved",       value = {Permission.ManageDataversePermissions, Permission.EditDataverse} ),
	@RequiredPermissions( dataverseName = "source",      value = Permission.DeleteDataverse ),
	@RequiredPermissions( dataverseName = "destination", value = Permission.AddDataverse )
})
public class MoveDataverseCommand extends AbstractVoidCommand {
	
	final Dataverse moved;
	final Dataverse destination;

	public MoveDataverseCommand( DataverseRequest aRequest, Dataverse moved, Dataverse destination ) {
		super(aRequest, dv("moved", moved),
					 dv("source",moved.getOwner()),
					 dv("destination",destination) );
		this.moved = moved;
		this.destination = destination;
	}
	
	@Override
	public void executeImpl(CommandContext ctxt) throws CommandException {
        
        // NOTE placeholder, more logic due
        
		// validate the move makes sense
		if ( destination.getOwners().contains(moved) ) {
			throw new IllegalCommandException("Can't move a dataverse to its descendant", this);
		}
		
		// OK, move
		moved.setOwner(destination);
		ctxt.dataverses().save(moved);
		
	}
	
}
