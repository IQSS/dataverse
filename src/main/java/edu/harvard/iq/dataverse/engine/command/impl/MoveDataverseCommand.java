package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import static edu.harvard.iq.dataverse.IdServiceBean.logger;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.List;
import java.util.logging.Level;

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
        final Boolean force;

	public MoveDataverseCommand( DataverseRequest aRequest, Dataverse moved, Dataverse destination, Boolean force ) {
		super(aRequest, dv("moved", moved),
					 dv("source",moved.getOwner()),
					 dv("destination",destination) );
		this.moved = moved;
		this.destination = destination;
                this.force = force;
	}
	
	@Override
	public void executeImpl(CommandContext ctxt) throws CommandException {
        
        
		// validate the move makes sense
		if ( destination.getOwners().contains(moved) ) {
			throw new IllegalCommandException("Can't move a Dataverse to its descendant", this);
		}
                if (moved.getOwner().equals(destination)) {
                    throw new IllegalCommandException("Dataverse already in this Dataverse ", this);
                }
                if (moved.equals(destination)) {
                    throw new IllegalCommandException("Cannot move a Dataverse into itself", this);
                }
                // if dataverse is published make sure that its destination is published
                if (moved.isReleased() && !destination.isReleased()){
                    throw new IllegalCommandException("Published Dataverse may not be moved to unpublished Dataverse. You may publish " + destination.getDisplayName() + " and re-try the move.", this);
                }
		
		// OK, move
		moved.setOwner(destination);
		ctxt.dataverses().save(moved);
                try {
                    indexDataverses(ctxt, moved);
                } catch (CommandException e) {
                    logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage()); //, e);
                    throw new CommandException("Dataverse could not be moved. Indexing failed: (" + e.getMessage() + ")", this);
                }
	}
        
        public void indexDataverses(CommandContext ctxt, Dataverse dataverse) throws CommandException{
            // index the Dataverse of current recursion
            try {
                ctxt.index().indexDataverse(dataverse);
            } catch (Exception e) {
                throw new CommandException("Dataverse could not be moved. Indexing failed on " + dataverse.getName() + ": (" + e.getMessage() + ")", this);
            }
            // get list of Dataverse children
            List<Dataverse> dataverseChildren = ctxt.dataverses().findByOwnerId(dataverse.getId());
            
            // get list of Dataset children
            List<Dataset> datasetChildren = ctxt.datasets().findByOwnerId(dataverse.getId());

            // index the Dataset children
            for (Dataset child : datasetChildren) {
                try {
                    boolean doNormalSolrDocCleanUp = true;
                    ctxt.index().indexDataset(child, doNormalSolrDocCleanUp);
                } catch (Exception e){
                    throw new CommandException("Dataset could not be moved. Indexing failed on " + child.getId() +": (" + e.getMessage() + ")", this);
                }
            }
            
            // recursively index the Dataverse children
            for (Dataverse child : dataverseChildren) {
                indexDataverses(ctxt, child);
            }
        }
	
}
