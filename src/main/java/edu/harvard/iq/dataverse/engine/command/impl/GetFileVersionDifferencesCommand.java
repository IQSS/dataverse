package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A command that retrieves the version history for a single file,
 * highlighting the differences between each version.
 */
@RequiredPermissions({})
public class GetFileVersionDifferencesCommand extends AbstractCommand<List<FileVersionDifference>> {

    private final FileMetadata fileMetadata;
    private final Integer limit;
    private final Integer offset;
    private final FileMetadataVersionsHelper versionsHelper;

    public GetFileVersionDifferencesCommand(DataverseRequest request, FileMetadata fileMetadata, Integer limit, Integer offset, FileMetadataVersionsHelper fileMetadataVersionsHelper) {
        super(request, fileMetadata.getDataFile());
        this.fileMetadata = Objects.requireNonNull(fileMetadata);
        this.versionsHelper = Objects.requireNonNull(fileMetadataVersionsHelper);
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public List<FileVersionDifference> execute(CommandContext ctxt) throws CommandException {
        List<VersionedFileMetadata> fileHistory = findFileHistory(ctxt);

        return fileHistory.stream()
                .map(this::createDifferenceFromHistory)
                .collect(Collectors.toList());
    }

    /**
     * Fetches the file's version history from the database, respecting user permissions.
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
        return ctxt.permissions().requestOn(getRequest(), dataset).has(Permission.ViewUnpublishedDataset);
    }

    /**
     * Transforms a single historical entry into a FileVersionDifference object.
     */
    private FileVersionDifference createDifferenceFromHistory(VersionedFileMetadata versionedFileMetadata) {
        FileMetadata current = versionedFileMetadata.getFileMetadata();
        DatasetVersion version = versionedFileMetadata.getDatasetVersion();

        FileMetadata previous = versionsHelper.getPreviousFileMetadata(this.fileMetadata, version);

        return (current != null)
                ? new FileVersionDifference(current, previous, false)
                : createDifferenceForNonexistentFile(version, previous);
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
