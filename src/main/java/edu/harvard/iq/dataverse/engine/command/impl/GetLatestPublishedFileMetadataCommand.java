package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

public class GetLatestPublishedFileMetadataCommand extends AbstractGetPublishedFileMetadataCommand {

    public GetLatestPublishedFileMetadataCommand(DataverseRequest request, DataFile dataFile, boolean includeDeaccessioned) {
        super(request, dataFile, includeDeaccessioned);
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        return dataFile.getFileMetadatas().stream().filter(fileMetadata -> {
            DatasetVersion.VersionState versionState = fileMetadata.getDatasetVersion().getVersionState();
            return (!versionState.equals(DatasetVersion.VersionState.DRAFT)
                    && !(versionState.equals(DatasetVersion.VersionState.DEACCESSIONED) && !includeDeaccessioned)
                    && isDatasetVersionAccessible(fileMetadata.getDatasetVersion(), dataFile.getOwner(), ctxt));
        }).reduce(null, DataFile::getTheNewerFileMetadata);
    }
}
