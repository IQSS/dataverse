package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions({})
public class GetLatestAccessibleFileMetadataCommand extends AbstractCommand<FileMetadata> {
    private final DataFile dataFile;
    private final boolean includeDeaccessioned;

    public GetLatestAccessibleFileMetadataCommand(DataverseRequest request, DataFile dataFile, boolean includeDeaccessioned) {
        super(request, dataFile);
        this.dataFile = dataFile;
        this.includeDeaccessioned = includeDeaccessioned;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        FileMetadata fileMetadata = null;

        if (ctxt.permissions().requestOn(getRequest(), dataFile.getOwner()).has(Permission.ViewUnpublishedDataset)) {
            fileMetadata = ctxt.engine().submit(
                    new GetDraftFileMetadataIfAvailableCommand(getRequest(), dataFile)
            );
        }

        if (fileMetadata == null) {
            fileMetadata = ctxt.engine().submit(
                    new GetLatestPublishedFileMetadataCommand(getRequest(), dataFile, includeDeaccessioned)
            );
        }

        return fileMetadata;
    }
}
