package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

public class GetLatestAccessibleFileMetadataCommand extends AbstractGetPublishedFileMetadataCommand {

    public GetLatestAccessibleFileMetadataCommand(DataverseRequest request, DataFile dataFile, boolean includeDeaccessioned) {
        super(request, dataFile, includeDeaccessioned);
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        FileMetadata fileMetadata = null;

        if (ctxt.permissions().requestOn(getRequest(), dataFile.getOwner()).has(Permission.ViewUnpublishedDataset)) {
            fileMetadata = dataFile.getDraftFileMetadata();
        }

        if (fileMetadata == null) {
            fileMetadata = getLatestPublishedFileMetadata(ctxt);
        }

        return fileMetadata;
    }
}
