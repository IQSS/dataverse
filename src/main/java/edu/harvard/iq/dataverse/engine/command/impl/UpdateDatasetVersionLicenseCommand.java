package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionLicenseCommand extends AbstractVoidCommand {
    private final DatasetVersion datasetVersion;

    public UpdateDatasetVersionLicenseCommand(DataverseRequest aRequest, DatasetVersion datasetVersion) {
        super(aRequest, datasetVersion.getDataset());
        this.datasetVersion = datasetVersion;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

    }
}
