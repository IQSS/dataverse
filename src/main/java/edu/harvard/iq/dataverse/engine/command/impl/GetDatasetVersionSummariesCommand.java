package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datasetversionsummaries.DatasetVersionSummary;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * A command that retrieves a paginated list of {@link DatasetVersionSummary} for the versions of a given {@link Dataset}.
 **/
@RequiredPermissions({})
public class GetDatasetVersionSummariesCommand extends AbstractCommand<List<DatasetVersionSummary>> {

    private final Dataset dataset;
    private final Integer limit;
    private final Integer offset;

    public GetDatasetVersionSummariesCommand(DataverseRequest request, Dataset dataset, Integer limit, Integer offset) {
        super(request, dataset);
        this.dataset = dataset;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public List<DatasetVersionSummary> execute(CommandContext ctxt) throws CommandException {
        validatePaginationParameter(limit, "limit");
        validatePaginationParameter(offset, "offset");

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

    /**
     * Validates that a given pagination parameter is not negative.
     *
     * @param value The parameter's value.
     * @param name  The parameter's name, used for the error message.
     * @throws InvalidCommandArgumentsException if the value is negative.
     */
    private void validatePaginationParameter(Integer value, String name) throws InvalidCommandArgumentsException {
        if (value != null && value < 0) {
            throw new InvalidCommandArgumentsException(
                    BundleUtil.getStringFromBundle("getFileVersionDifferencesCommand.errors.negativePaginationParam", List.of(name)),
                    this
            );
        }
    }
}
