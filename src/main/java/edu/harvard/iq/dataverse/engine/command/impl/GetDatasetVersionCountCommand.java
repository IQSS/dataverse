package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.EnumSet;

/**
 * Retrieves the version count for a single dataset.
 * Depending on user permissions, unpublished versions will be counted or not.
 */
@RequiredPermissions({})
public class GetDatasetVersionCountCommand extends AbstractCommand<Long> {

    private final Dataset dataset;

    public GetDatasetVersionCountCommand(DataverseRequest request, Dataset dataset) {
        super(request, dataset);
        this.dataset = dataset;
    }

    @Override
    public Long execute(CommandContext ctxt) throws CommandException {
        return ctxt.datasetVersion().getDatasetVersionCount(dataset.getId(), canViewUnpublishedVersions(ctxt, dataset));
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
}
