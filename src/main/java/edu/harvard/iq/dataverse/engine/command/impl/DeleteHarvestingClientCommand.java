package edu.harvard.iq.dataverse.engine.command.impl;

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

    public DeleteHarvestingClientCommand(DataverseRequest aRequest, Dataverse motherDataverse) {
        super(aRequest, motherDataverse);
        this.motherDataverse = motherDataverse;
    }

    @Override
    public void executeImpl(CommandContext ctxt) throws CommandException {
        HarvestingClient harvestingClient = motherDataverse.getHarvestingClientConfig();
        if (harvestingClient == null) {
            throw new IllegalCommandException("No harvesting client is configured for dataverse "+motherDataverse.getAlias(), this);
        }
        motherDataverse.setHarvestingClientConfig(null);
        ctxt.em().remove(harvestingClient);
        ctxt.em().merge(motherDataverse);
    }
    
}
