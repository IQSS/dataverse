package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Leonid Andreev
 */
// One can view the configuration of a Harvesting Client if and only if 
// they have the permission to view the dataverse that owns the harvesting
// client. And for a Dataverse, we cannot define the permission with a 
// @RequiredPermission annotation - because the decision has to be made dynamically: 
// Everybody can view a published Dataverse; otherwise, an explicit 
// ViewUnpublishedDataverse is needed. 
// This is defined in the getRequiredPermissions() method, below. 
public class GetHarvestingClientCommand extends AbstractCommand<HarvestingClient>{
    private final Dataverse ownerDataverse;

    public GetHarvestingClientCommand(DataverseRequest aRequest, HarvestingClient harvestingClient) {
        super(aRequest, harvestingClient.getDataverse());
        this.ownerDataverse = harvestingClient.getDataverse();
    }

    @Override
    public HarvestingClient execute(CommandContext ctxt) throws CommandException {
        if (ownerDataverse == null) {
            throw new IllegalCommandException("GetHarvestingClientCommand called on a null dataverse object", this);
        }
        if (ownerDataverse.getHarvestingClientConfig() == null) {
            throw new IllegalCommandException("No harvesting client is configured for dataverse "+ownerDataverse.getAlias(), this);
        }
        return ownerDataverse.getHarvestingClientConfig();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                ownerDataverse.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }    
}