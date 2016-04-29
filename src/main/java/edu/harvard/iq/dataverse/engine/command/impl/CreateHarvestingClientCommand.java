package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author Leonid Andreev
 */
@RequiredPermissions( Permission.EditDataverse )
public class CreateHarvestingClientCommand extends AbstractCommand<HarvestingClient> {
    
    private final Dataverse dv;
    private final HarvestingClient harvestingClient; 

    public CreateHarvestingClientCommand(DataverseRequest aRequest, HarvestingClient harvestingClient) {
        super(aRequest, harvestingClient.getDataverse());
        this.harvestingClient = harvestingClient; 
        dv = harvestingClient.getDataverse();
    }

    @Override
    public HarvestingClient execute(CommandContext ctxt) throws CommandException {
        // TODO: check if the harvesting client config is legit; 
        // and that it is indeed new and unique? 
        // (may not be necessary - as the uniqueness should be enforced by 
        // the persistence layer... -- L.A. 4.4)
        return ctxt.em().merge(this.harvestingClient);
    }
    
}