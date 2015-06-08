package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Deletes a data set.
 *
 * @author michael
 */
@RequiredPermissions(Permission.DeleteDatasetDraft)
public class DeleteDatasetCommand extends AbstractVoidCommand {

    private final Dataset doomed;

    public DeleteDatasetCommand(User aUser, Dataset dataset) {
        super(aUser, dataset);
        this.doomed = dataset;
    }
    
    public DeleteDatasetCommand(Dataset dataset, User aUser) {
        super(aUser, dataset);
        this.doomed = dataset;
    }    

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // TODO: REMOVE THIS COMMAND
        // for now it just calls DeleteDatasetVersion
        ctxt.engine().submit(new DeleteDatasetVersionCommand(getUser(), doomed));
    }

}
