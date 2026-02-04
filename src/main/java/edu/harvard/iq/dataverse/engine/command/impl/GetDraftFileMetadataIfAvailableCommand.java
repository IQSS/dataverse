package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * @author Matthew
 */
@RequiredPermissions(Permission.ViewUnpublishedDataset)
public class GetDraftFileMetadataIfAvailableCommand extends AbstractCommand<FileMetadata> {
    private final DataFile dataFile;

    public GetDraftFileMetadataIfAvailableCommand(DataverseRequest request, DataFile dataFile) {
        super(request, dataFile);
        this.dataFile = dataFile;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        return dataFile.getDraftFileMetadata();
    }
}
