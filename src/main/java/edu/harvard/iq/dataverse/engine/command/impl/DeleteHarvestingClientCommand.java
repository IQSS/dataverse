package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 *
 * @author Leonid Andreev
 */
@RequiredPermissions( Permission.EditDataverse )
public class DeleteHarvestingClientCommand extends AbstractVoidCommand {
    
    private final Dataverse motherDataverse;
    private final HarvestingClient harvestingClient; 

    public DeleteHarvestingClientCommand(DataverseRequest aRequest, HarvestingClient harvestingClient) {
        super(aRequest, harvestingClient.getDataverse());
        this.motherDataverse = harvestingClient.getDataverse();
        this.harvestingClient = harvestingClient;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        
        if (harvestingClient == null) {
            throw new IllegalCommandException("DeleteHarvestingClientCommand: attempted to execute with null harvesting client; dataverse: "+motherDataverse.getAlias(), this);
        }
        
        HarvestingClient merged = ctxt.em().merge(harvestingClient);

        // Purge all the SOLR documents associated with this client from the 
        // index server: 
        // ctxt.index().deleteHarvestedDocuments(merged);
        
        // All the datasets harvested by this client will be cleanly deleted 
        // through the defined cascade. Cascaded delete does not work for harvested 
        // files, however. So they need to be removed explicitly; before we 
        // proceed removing the client itself. 
       
        for (DataFile harvestedFile : ctxt.files().findHarvestedFilesByClient(merged)) {
            DataFile mergedFile = ctxt.em().merge(harvestedFile);
            ctxt.em().remove(mergedFile);
            harvestedFile = null; 
        }
        
        ctxt.em().remove(merged);
    }
    
}
