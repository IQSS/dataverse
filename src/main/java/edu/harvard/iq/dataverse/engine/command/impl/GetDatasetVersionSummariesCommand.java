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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@RequiredPermissions({})
public class GetDatasetVersionSummariesCommand extends AbstractCommand<List<DatasetVersionSummary>> {

    private final Dataset dataset;

    public GetDatasetVersionSummariesCommand(DataverseRequest request, Dataset dataset) {
        super(request, dataset);
        this.dataset = dataset;
    }

    @Override
    public List<DatasetVersionSummary> execute(CommandContext ctxt) throws CommandException {
        return dataset.getVersions().stream()
                .filter(dv -> versionRequiresSummary(ctxt, dv))
                .map(DatasetVersionSummary::from)
                .flatMap(Optional::stream)
                .toList();
    }

    private boolean versionRequiresSummary(CommandContext ctxt, DatasetVersion dv) {
        return dv.isPublished()
                || dv.isDeaccessioned()
                || ctxt.permissions().hasPermissionsFor(getRequest().getUser(), dv.getDataset(), EnumSet.of(Permission.ViewUnpublishedDataset));
    }
}
