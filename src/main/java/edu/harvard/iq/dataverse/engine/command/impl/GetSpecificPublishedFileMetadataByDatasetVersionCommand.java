package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

public class GetSpecificPublishedFileMetadataByDatasetVersionCommand extends AbstractGetPublishedFileMetadataCommand {
    private final long majorVersion;
    private final long minorVersion;

    public GetSpecificPublishedFileMetadataByDatasetVersionCommand(DataverseRequest request, DataFile dataFile, long majorVersion, long minorVersion, boolean includeDeaccessioned) {
        super(request, dataFile, includeDeaccessioned);
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
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
        return isDatasetVersionAccessible(datasetVersion, dataFile.getOwner(), ctxt)
                && datasetVersion.getVersionNumber().equals(majorVersion)
                && datasetVersion.getMinorVersionNumber().equals(minorVersion);
    }
}
