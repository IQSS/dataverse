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
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions({})
public class GetSpecificPublishedFileMetadataByDatasetVersionCommand extends AbstractCommand<FileMetadata> {
    private final long majorVersion;
    private final long minorVersion;
    private final DataFile dataFile;
    private final boolean includeDeaccessioned;

    public GetSpecificPublishedFileMetadataByDatasetVersionCommand(DataverseRequest aRequest, DataFile dataFile, long majorVersion, long minorVersion, boolean includeDeaccessioned) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.includeDeaccessioned = includeDeaccessioned;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        return dataFile.getFileMetadatas().stream()
                .filter(fileMetadata -> isRequestedVersionFileMetadata(fileMetadata, ctxt))
                .findFirst()
                .orElse(null);
    }

    private boolean isRequestedVersionFileMetadata(FileMetadata fileMetadata, CommandContext ctxt) {
        DatasetVersion datasetVersion = fileMetadata.getDatasetVersion();
        Dataset ownerDataset = dataFile.getOwner();
        return (datasetVersion.isReleased() || isDatasetVersionDeaccessionedAndAccessible(datasetVersion, ownerDataset, ctxt))
                && datasetVersion.getVersionNumber().equals(majorVersion)
                && datasetVersion.getMinorVersionNumber().equals(minorVersion);
    }

    private boolean isDatasetVersionDeaccessionedAndAccessible(DatasetVersion datasetVersion, Dataset ownerDataset, CommandContext ctxt) {
        return includeDeaccessioned && datasetVersion.isDeaccessioned() && ctxt.permissions().requestOn(getRequest(), ownerDataset).has(Permission.EditDataset);
    }
}
