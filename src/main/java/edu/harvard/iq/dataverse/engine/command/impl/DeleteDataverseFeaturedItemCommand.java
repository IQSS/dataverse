package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Deletes a particular featured item {@link DataverseFeaturedItem} of a {@link Dataverse}.
 */
@RequiredPermissions({Permission.EditDataverse})
public class DeleteDataverseFeaturedItemCommand extends AbstractVoidCommand {

    private final DataverseFeaturedItem doomed;

    public DeleteDataverseFeaturedItemCommand(DataverseRequest request, DataverseFeaturedItem doomed) {
        super(request, doomed.getDataverse());
        this.doomed = doomed;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        ctxt.dataverseFeaturedItems().delete(doomed.getId());
    }
}
