package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions({})
public class GetLatestAccessibleFileMetadataCommand extends AbstractCommand<FileMetadata> {
    private final DataFile dataFile;

    public GetLatestAccessibleFileMetadataCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        FileMetadata fileMetadata = ctxt.engine().submit(
                new GetLatestPublishedFileMetadataCommand(getRequest(), dataFile)
        );

        if (fileMetadata == null) {
            fileMetadata = ctxt.engine().submit(
                    new GetDraftFileMetadataIfAvailableCommand(getRequest(), dataFile)
            );
        }

        return fileMetadata;
    }
}
