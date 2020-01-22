package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.Permission;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetGuestbookCommand extends AbstractDatasetCommand<Dataset> {

    public UpdateDatasetGuestbookCommand(DataverseRequest dataverseRequest, Dataset dataset) {
        super(dataverseRequest, dataset);
    }

    @Override
    public Dataset execute(CommandContext ctxt) {
        ctxt.permissions().checkEditDatasetLock(getDataset(), getRequest(), this);

        return ctxt.datasets().merge(getDataset());
    }
}
