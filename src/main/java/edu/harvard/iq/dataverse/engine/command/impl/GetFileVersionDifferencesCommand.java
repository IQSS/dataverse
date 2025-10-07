package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datasetversionsummaries.DatasetVersionSummary;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.ArrayList;
import java.util.List;

/**
 * A command that retrieves a paginated list of {@link DatasetVersionSummary} for the versions of a given {@link Dataset}.
 **/
@RequiredPermissions({})
public class GetFileVersionDifferencesCommand extends AbstractCommand<List<FileVersionDifference>> {
    private final FileMetadata fileMetadata;
    private final Integer limit;
    private final Integer offset;
    private final FileMetadataVersionsHelper fileMetadataVersionsHelper;

    public GetFileVersionDifferencesCommand(DataverseRequest request, FileMetadata fileMetadata, Integer limit, Integer offset, FileMetadataVersionsHelper fileMetadataVersionsHelper) {
        super(request, fileMetadata.getDataFile());
        this.fileMetadata = fileMetadata;
        this.limit = limit;
        this.offset = offset;
        this.fileMetadataVersionsHelper = fileMetadataVersionsHelper;
    }

    @Override
    // TODO
    public List<FileVersionDifference> execute(CommandContext ctxt) throws CommandException {
        Dataset dataset = fileMetadata.getDatasetVersion().getDataset();
        boolean hasPermission = ctxt.permissions().requestOn(getRequest(), dataset).has(Permission.ViewUnpublishedDataset);
        List<VersionedFileMetadata> versionedFileMetadataList = ctxt.files().findFileMetadataHistory(dataset.getId(), fileMetadata.getDataFile(), hasPermission);
        List<FileVersionDifference> differences = new ArrayList<>();
        for (VersionedFileMetadata versionedFileMetadata : versionedFileMetadataList) {
            DatasetVersion datasetVersion = versionedFileMetadata.getDatasetVersion();
            if (versionedFileMetadata.getFileMetadata() == null) {
                FileMetadata dummy = new FileMetadata();
                dummy.setDatasetVersion(datasetVersion);
                dummy.setDataFile(null);
                FileVersionDifference fvd = new FileVersionDifference(dummy, fileMetadataVersionsHelper.getPreviousFileMetadata(fileMetadata, datasetVersion), true);
                dummy.setFileVersionDifference(fvd);
                differences.add(fvd);
            } else {
                differences.add(new FileVersionDifference(versionedFileMetadata.getFileMetadata(), fileMetadataVersionsHelper.getPreviousFileMetadata(fileMetadata, datasetVersion), true));
            }
        }
        return differences;
    }
}
