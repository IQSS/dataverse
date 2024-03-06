package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.ViewUnpublishedDataset )
public class GetDraftDatasetVersionCommand extends AbstractCommand<DatasetVersion>{
    private final Dataset ds;

    public GetDraftDatasetVersionCommand(DataverseRequest aRequest, Dataset anAffectedDataset) {
        super(aRequest, anAffectedDataset);
        ds = anAffectedDataset;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        return ds.getOrCreateEditVersion();
    }
    
}
