package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;

@RequiredPermissions(Permission.ViewUnpublishedDataset)
public class GetDraftVersionIfExists extends AbstractCommand<DatasetVersion> {

    private Dataset dataset;

    public GetDraftVersionIfExists(DataverseRequest aRequest, Dataset anAffectedDataset) {
        super(aRequest, anAffectedDataset);
        dataset = anAffectedDataset;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) {
        DatasetVersion latestVersion = dataset.getLatestVersion();
        return latestVersion.isDraft() ? latestVersion : null;
    }
}
