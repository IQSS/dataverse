package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A command that retrieves the version history for a single file,
 * highlighting the differences between each version.
 */
@RequiredPermissions({})
public class GetFileVersionDifferencesCommand extends AbstractPaginatedCommand<List<FileVersionDifference>> {

    private final FileMetadata fileMetadata;

    public GetFileVersionDifferencesCommand(DataverseRequest request, FileMetadata fileMetadata, Integer limit, Integer offset) {
        super(request, fileMetadata.getDataFile(), limit, offset);
        this.fileMetadata = Objects.requireNonNull(fileMetadata);
    }

    @Override
    public List<FileVersionDifference> executeCommand(CommandContext ctxt) {
        List<VersionedFileMetadata> fileHistory = findFileHistory(ctxt);

        return fileHistory.stream()
                .map(versionedFileMetadata -> createDifferenceFromHistory(ctxt, versionedFileMetadata))
                .collect(Collectors.toList());
    }

    /**
     * Fetches the file's version history, respecting user permissions.
     */
    private List<VersionedFileMetadata> findFileHistory(CommandContext ctxt) {
        Dataset dataset = fileMetadata.getDatasetVersion().getDataset();
        boolean canViewUnpublished = canViewUnpublishedVersions(ctxt, dataset);

        return ctxt.files().findFileMetadataHistory(dataset.getId(), fileMetadata.getDataFile(), canViewUnpublished, limit, offset);
    }

    /**
     * Determines if the user has permission to view non-released versions of the dataset.
     */
    private boolean canViewUnpublishedVersions(CommandContext ctxt, Dataset dataset) {
        return ctxt.permissions().hasPermissionsFor(
                getRequest().getUser(),
                dataset,
                EnumSet.of(Permission.ViewUnpublishedDataset)
        );
    }

    /**
     * Transforms a single historical entry into a FileVersionDifference object.
     * As part of this process, it also enriches the file metadata with contributor names from that version.
     */
    private FileVersionDifference createDifferenceFromHistory(CommandContext ctxt, VersionedFileMetadata versionedFileMetadata) {
        FileMetadata fileMetadata = versionedFileMetadata.getFileMetadata();
        FileMetadata previous = ctxt.files().getPreviousFileMetadata(fileMetadata);

        if (fileMetadata != null) {
            fileMetadata.setContributorNames(ctxt.datasetVersion().getContributorsNames(fileMetadata.getDatasetVersion()));
            return new FileVersionDifference(fileMetadata, previous, false);
        }

        return createDifferenceForNonexistentFile(versionedFileMetadata.getDatasetVersion(), previous);
    }

    /**
     * Creates a special FileVersionDifference for versions where the file does not exist.
     */
    private FileVersionDifference createDifferenceForNonexistentFile(DatasetVersion version, FileMetadata previous) {
        FileMetadata placeholder = new FileMetadata();
        placeholder.setDatasetVersion(version);
        // Explicitly null DataFile indicates the file does not exist.
        placeholder.setDataFile(null);

        FileVersionDifference difference = new FileVersionDifference(placeholder, previous, true);
        placeholder.setFileVersionDifference(difference);

        return difference;
    }
}
