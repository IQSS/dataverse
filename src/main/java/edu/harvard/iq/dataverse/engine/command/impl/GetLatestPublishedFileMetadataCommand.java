package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions({})
public class GetLatestPublishedFileMetadataCommand extends AbstractCommand<FileMetadata> {
    private final DataFile dataFile;

    public GetLatestPublishedFileMetadataCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        try {
            return dataFile.getLatestPublishedFileMetadata();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
