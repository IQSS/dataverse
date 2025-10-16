package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileVersionDifference;
import edu.harvard.iq.dataverse.VersionedFileMetadata;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Retrieves the version history for a single file, highlighting the
 * differences between each version. This command is pagination-aware.
 */
@RequiredPermissions({})
public class GetFileVersionDifferencesCommand extends AbstractPaginatedCommand<List<FileVersionDifference>> {

    private final FileMetadata fileMetadata;

    public GetFileVersionDifferencesCommand(DataverseRequest request, FileMetadata fileMetadata, Integer limit, Integer offset) {
        super(request, fileMetadata.getDataFile(), limit, offset);
        this.fileMetadata = Objects.requireNonNull(fileMetadata);
    }

    /**
     * Executes the command to generate a list of file version differences.
     * <p>
     * This method processes a paginated list of file history.
     * It pairs each version with its successor from the list.
     * For the last item on the page, it performs a single query to find its true predecessor, handling the pagination case.
     *
     * @param ctxt The command context.
     * @return A list of {@link FileVersionDifference} objects.
     */
    @Override
    public List<FileVersionDifference> executeCommand(CommandContext ctxt) {
        List<VersionedFileMetadata> fileHistory = findFileHistory(ctxt);
        if (fileHistory.isEmpty()) {
            return Collections.emptyList();
        }

        List<FileVersionDifference> differences = new ArrayList<>();

        // Process all but the last item, using the next item in the list as the predecessor.
        for (int i = 0; i < fileHistory.size() - 1; i++) {
            VersionedFileMetadata current = fileHistory.get(i);
            FileMetadata previous = fileHistory.get(i + 1).getFileMetadata();
            differences.add(buildDifference(ctxt, current, previous));
        }

        // Handle the last item on the page separately. Its predecessor may not be in the current
        // list, so we must query the database for it.
        VersionedFileMetadata lastItemOnPage = fileHistory.get(fileHistory.size() - 1);
        FileMetadata predecessor = findPredecessorFor(ctxt, lastItemOnPage);
        differences.add(buildDifference(ctxt, lastItemOnPage, predecessor));

        return differences;
    }

    /**
     * Constructs a {@link FileVersionDifference} from a version's metadata and its predecessor.
     *
     * @param ctxt                   The command context.
     * @param currentVersionMetadata The metadata for the current version in history.
     * @param previousFileMetadata   The metadata for the preceding version (can be null).
     * @return A new {@link FileVersionDifference} object.
     */
    private FileVersionDifference buildDifference(CommandContext ctxt, VersionedFileMetadata currentVersionMetadata, FileMetadata previousFileMetadata) {
        FileMetadata currentFileMetadata = currentVersionMetadata.getFileMetadata();

        if (currentFileMetadata != null) {
            return createDifferenceForExistingFile(ctxt, currentFileMetadata, previousFileMetadata);
        } else {
            return createDifferenceForMissingFile(currentVersionMetadata.getDatasetVersion(), previousFileMetadata);
        }
    }

    /**
     * Creates a difference object for a version where the file exists.
     * As a side effect, this method also enriches the metadata with contributor names.
     */
    private FileVersionDifference createDifferenceForExistingFile(CommandContext ctxt, FileMetadata current, FileMetadata previous) {
        current.setContributorNames(ctxt.datasetVersion().getContributorsNames(current.getDatasetVersion()));
        return new FileVersionDifference(current, previous, false);
    }

    /**
     * Creates a placeholder difference object for a version where the file was absent.
     */
    private FileVersionDifference createDifferenceForMissingFile(DatasetVersion version, FileMetadata previous) {
        FileMetadata placeholder = new FileMetadata();
        placeholder.setDatasetVersion(version);
        placeholder.setDataFile(null); // Explicitly null to indicate absence.

        FileVersionDifference difference = new FileVersionDifference(placeholder, previous, true);
        placeholder.setFileVersionDifference(difference);

        return difference;
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
     * Determines if the user can view non-released versions of the dataset.
     */
    private boolean canViewUnpublishedVersions(CommandContext ctxt, Dataset dataset) {
        return ctxt.permissions().hasPermissionsFor(
                getRequest().getUser(),
                dataset,
                EnumSet.of(Permission.ViewUnpublishedDataset)
        );
    }

    /**
     * Finds the chronological predecessor for a given {@link VersionedFileMetadata}.
     */
    private FileMetadata findPredecessorFor(CommandContext ctxt, VersionedFileMetadata versionedMetadata) {
        FileMetadata current = versionedMetadata.getFileMetadata();
        return (current != null) ? ctxt.files().getPreviousFileMetadata(current) : null;
    }
}
