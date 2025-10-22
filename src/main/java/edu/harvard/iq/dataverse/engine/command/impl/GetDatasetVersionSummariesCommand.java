package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datasetversionsummaries.DatasetVersionSummary;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * A command that retrieves a paginated list of {@link DatasetVersionSummary} for the versions of a given {@link Dataset}.
 **/
@RequiredPermissions({})
public class GetDatasetVersionSummariesCommand extends AbstractPaginatedCommand<List<DatasetVersionSummary>> {

    private final Dataset dataset;

    public GetDatasetVersionSummariesCommand(DataverseRequest request, Dataset dataset, Integer limit, Integer offset) {
        super(request, dataset, limit, offset);
        this.dataset = dataset;
    }

    /**
     * Executes the command to retrieve a paginated list of dataset version summaries.
     * <p>
     * This method first checks if the user has permission to view unpublished
     * versions of the dataset. It then fetches the appropriate {@link DatasetVersion}s,
     * respecting pagination parameters (limit and offset). Each version is then
     * enriched with its contributor names before being converted into a
     * {@link DatasetVersionSummary}.
     *
     * @param ctxt The command context.
     * @return A list of {@link DatasetVersionSummary} objects.
     */
    @Override
    public List<DatasetVersionSummary> executeCommand(CommandContext ctxt) {
        boolean canViewUnpublished = ctxt.permissions().hasPermissionsFor(
                getRequest().getUser(),
                dataset,
                EnumSet.of(Permission.ViewUnpublishedDataset)
        );

        List<DatasetVersion> versions = ctxt.datasetVersion().findVersions(
                dataset.getId(),
                offset,
                limit,
                canViewUnpublished,
                true
        );

        for (DatasetVersion version : versions) {
            version.setContributorNames(ctxt.datasetVersion().getContributorsNames(version));
        }

        return versions.stream()
                .map(DatasetVersionSummary::from)
                .flatMap(Optional::stream)
                .toList();
    }
}
