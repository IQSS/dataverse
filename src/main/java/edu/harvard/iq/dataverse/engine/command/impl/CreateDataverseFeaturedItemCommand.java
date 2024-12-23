package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * A command that creates a featured item in a {@link Dataverse}.
 */
//TODO permissions
@RequiredPermissions({})
public class CreateDataverseFeaturedItemCommand extends AbstractCommand<DataverseFeaturedItem> {

    private final Dataverse dataverse;

    public CreateDataverseFeaturedItemCommand(DataverseRequest request, Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public DataverseFeaturedItem execute(CommandContext ctxt) throws CommandException {
        return null;
    }
}
