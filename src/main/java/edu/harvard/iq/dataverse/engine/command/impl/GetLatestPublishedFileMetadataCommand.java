package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
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
        try {
            FileMetadata fileMetadata = dataFile.getLatestPublishedFileMetadata(includeDeaccessioned);
            if (isDatasetVersionAccessible(fileMetadata.getDatasetVersion(), dataFile.getOwner(), ctxt)) {
                return fileMetadata;
            }
            return null;
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
