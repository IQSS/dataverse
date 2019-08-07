package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * Deletes a dataset.
 *
 * @author michael
 */
@RequiredPermissions(Permission.DeleteDatasetDraft)
public class DeleteDatasetCommand extends AbstractVoidCommand {

    private final Dataset doomed;

    public DeleteDatasetCommand(DataverseRequest aRequest, Dataset dataset) {
        super(aRequest, dataset);
        this.doomed = dataset;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // TODO: REMOVE THIS COMMAND
        // for now it just calls DeleteDatasetVersion
        ctxt.engine().submit(new DeleteDatasetVersionCommand(getRequest(), doomed));
    }

}
