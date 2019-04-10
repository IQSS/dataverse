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

/**
 *
 * @author Matthew
 */
@RequiredPermissions( Permission.ViewUnpublishedDataset )
public class GetDraftFileMetadataIfAvailableCommand extends AbstractCommand<FileMetadata>{
    private final DataFile df;

    public GetDraftFileMetadataIfAvailableCommand(DataverseRequest aRequest, DataFile dataFile) {
        super(aRequest, dataFile);
        df = dataFile;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        FileMetadata fm = df.getLatestFileMetadata();
        if(fm.getDatasetVersion().getVersionState().equals(DatasetVersion.VersionState.DRAFT)) {
            return df.getLatestFileMetadata();
        } 
        return null;
    }
    
}
