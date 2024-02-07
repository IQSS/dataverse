package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.List;

@RequiredPermissions({})
public class GetSpecificPublishedFileMetadataByDatasetVersionCommand extends AbstractCommand<FileMetadata> {
    private final long majorVersion;
    private final long minorVersion;
    private final DataFile dataFile;

    public GetSpecificPublishedFileMetadataByDatasetVersionCommand(DataverseRequest aRequest, DataFile dataFile, long majorVersionNum, long minorVersionNum) {
        super(aRequest, dataFile);
        this.dataFile = dataFile;
        majorVersion = majorVersionNum;
        minorVersion = minorVersionNum;
    }

    @Override
    public FileMetadata execute(CommandContext ctxt) throws CommandException {
        List<FileMetadata> fileMetadatas = dataFile.getFileMetadatas();

        for (FileMetadata fileMetadata : fileMetadatas) {
            DatasetVersion datasetVersion = fileMetadata.getDatasetVersion();

            if (datasetVersion.isPublished() &&
                    datasetVersion.getVersionNumber().equals(majorVersion) &&
                    datasetVersion.getMinorVersionNumber().equals(minorVersion)) {
                return fileMetadata;
            }
        }

        return null;
    }
}
