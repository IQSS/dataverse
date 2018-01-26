/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
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
import java.util.logging.Logger;

/**
 * Moves Dataset from one dataverse to another
 * @author skraffmi
 */
@RequiredPermissionsMap({
	@RequiredPermissions( dataverseName = "moved",       value = {Permission.PublishDataset} ),
        @RequiredPermissions( dataverseName = "source",      value = Permission.EditDataset),
	@RequiredPermissions( dataverseName = "destination", value = {Permission.AddDataset} )
})
public class MoveDatasetCommand extends AbstractVoidCommand {

    
        private static final Logger logger = Logger.getLogger(MoveDatasetCommand.class.getCanonicalName());
	final Dataset moved;
	final Dataverse destination;
        

	public MoveDatasetCommand( DataverseRequest aRequest, Dataset moved, Dataverse destination ) {
		super(aRequest, dv("moved", moved),
                        dv("source",moved.getOwner()),
					 dv("destination",destination) );
		this.moved = moved;
		this.destination = destination;
	}
        
        
        
        
        @Override
	public void executeImpl(CommandContext ctxt) throws CommandException {
		// validate the move makes sense
		if ( moved.getOwner().equals(destination) ) {
			throw new IllegalCommandException("Dataset already in this Dataverse ", this);
		}
                
                //Test Metadata Blocks
                // As long as the target DV has all of the blocks contained in the source it's OK
                // target may have more blocks.
                
                List<MetadataBlock> sourceMetadataBlocks = moved.getOwner().getMetadataBlocks();                
                
                List<MetadataBlock> targetMetadataBlocks = destination.getMetadataBlocks();
                boolean containsAll = true;
                for (MetadataBlock mdbSource : sourceMetadataBlocks) {
                    boolean found = false;
                    
                    for (MetadataBlock mdbTarget : targetMetadataBlocks ){
                        
                        if (mdbTarget.equals(mdbSource)){
                            found = true;
                        }
                    }
                    containsAll &= found;
                }
                
                if (!containsAll) {
			throw new IllegalCommandException("Metadata Blocks are incompatible for moving this Dataset ", this);
		}
                
		
		// OK, move
		moved.setOwner(destination);
		ctxt.em().merge(moved);
                
                
                        try {
            /**
             * @todo Do something with the result. Did it succeed or fail?
             */
            boolean doNormalSolrDocCleanUp = true;
            ctxt.index().indexDataset(moved, doNormalSolrDocCleanUp);
        
        } catch ( Exception e ) { // RuntimeException e ) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage()); //, e);
            /**
             * Even though the original intention appears to have been to allow the 
             * dataset to be successfully created, even if an exception is thrown during 
             * the indexing - in reality, a runtime exception there, even caught, 
             * still forces the EJB transaction to be rolled back; hence the 
             * dataset is NOT created... but the command completes and exits as if
             * it has been successful. 
             * So I am going to throw a Command Exception here, to avoid this. 
             * If we DO want to be able to create datasets even if they cannot 
             * be immediately indexed, we'll have to figure out how to do that. 
             * (Note that import is still possible when Solr is down - because indexDataset() 
             * does NOT throw an exception if it is.
             * -- L.A. 4.5
             */
            throw new CommandException("Dataset could not be created. Indexing failed", this);
            
        }
		
	}
        
    
}
