package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;

@RequiredPermissions({})
abstract class AbstractGetPublishedFileMetadataCommand extends AbstractCommand<FileMetadata> {
    protected final DataFile dataFile;
    protected final boolean includeDeaccessioned;

    public AbstractGetPublishedFileMetadataCommand(DataverseRequest request, DataFile dataFile, boolean includeDeaccessioned) {
        super(request, dataFile);
        this.dataFile = dataFile;
        this.includeDeaccessioned = includeDeaccessioned;
    }

    protected boolean isDatasetVersionAccessible(DatasetVersion datasetVersion, Dataset ownerDataset, CommandContext ctxt) {
        return datasetVersion.isReleased() || isDatasetVersionDeaccessionedAndAccessible(datasetVersion, ownerDataset, ctxt);
    }

    private boolean isDatasetVersionDeaccessionedAndAccessible(DatasetVersion datasetVersion, Dataset ownerDataset, CommandContext ctxt) {
        return includeDeaccessioned && datasetVersion.isDeaccessioned() && ctxt.permissions().requestOn(getRequest(), ownerDataset).has(Permission.EditDataset);
    }
}
