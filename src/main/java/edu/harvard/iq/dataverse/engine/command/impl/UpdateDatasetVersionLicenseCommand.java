package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionLicenseCommand extends AbstractVoidCommand {
    private final DatasetVersion datasetVersion;
    private final String licenseName;

    public UpdateDatasetVersionLicenseCommand(DataverseRequest aRequest, DatasetVersion datasetVersion, String licenseName) {
        super(aRequest, datasetVersion.getDataset());
        this.datasetVersion = datasetVersion;
        this.licenseName = licenseName;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

    }
}
