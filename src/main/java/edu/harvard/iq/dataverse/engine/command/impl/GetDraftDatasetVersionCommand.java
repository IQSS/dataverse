package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.ViewUnpublishedDataset )
public class GetDraftDatasetVersionCommand extends AbstractCommand<DatasetVersion>{
    private final Dataset ds;

    public GetDraftDatasetVersionCommand(User aUser, Dataset anAffectedDataset) {
        super(aUser, anAffectedDataset);
        ds = anAffectedDataset;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        return ds.getEditVersion();
    }
    
}
