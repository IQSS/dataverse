package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * A command to move a {@link Dataverse} between two {@link Dataverse}s.
 * @author michael
 */
@RequiredPermissionsMap({
	@RequiredPermissions( dataverseName = "moved",       value = Permission.DataverseEdit ),
	@RequiredPermissions( dataverseName = "source",      value = Permission.DataverseDelete ),
	@RequiredPermissions( dataverseName = "destination", value = Permission.DataverseCreate )
})
public class MoveDataverseCommand extends AbstractVoidCommand{
	
	final Dataverse moved;
	final Dataverse destination;

	public MoveDataverseCommand( DataverseUser aUser, Dataverse moved, Dataverse destination ) {
		super(aUser, p("moved", moved),
					 p("source",moved.getOwner()),
					 p("destination",destination) );
		this.moved = moved;
		this.destination = destination;
	}
	
	@Override
	public void executeImpl(CommandContext ctxt) throws CommandException {
		moved.setOwner(destination);
		ctxt.dataverses().save(moved);
	}
	
}
