package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * A command to move a {@link Dataverse} between two {@link Dataverse}s.
 * @author michael
 */
@RequiredPermissionsMap({
	@RequiredPermissions( dataverseName = "moved",       value = {Permission.UndoableEdit, Permission.GrantPermissions} ),
	@RequiredPermissions( dataverseName = "source",      value = Permission.UndoableEdit ),
	@RequiredPermissions( dataverseName = "destination", value = Permission.DestructiveEdit )
})
public class MoveDataverseCommand extends AbstractVoidCommand {
	
	final Dataverse moved;
	final Dataverse destination;

	public MoveDataverseCommand( DataverseUser aUser, Dataverse moved, Dataverse destination ) {
		super(aUser, dv("moved", moved),
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
