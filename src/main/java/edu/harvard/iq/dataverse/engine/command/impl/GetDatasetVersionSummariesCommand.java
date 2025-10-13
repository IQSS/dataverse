package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datasetversionsummaries.DatasetVersionSummary;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

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

    @Override
    public List<DatasetVersionSummary> executeCommand(CommandContext ctxt) throws CommandException {
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

        return versions.stream()
                .map(DatasetVersionSummary::from)
                .flatMap(Optional::stream)
                .toList();
    }
}
