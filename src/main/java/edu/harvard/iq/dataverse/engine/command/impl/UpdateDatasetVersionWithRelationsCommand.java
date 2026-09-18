package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.TermsOfUseAndAccessValidator;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.List;

/**
 * Atomically updates a draft version and, when supplied, replaces its relations.
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionWithRelationsCommand extends AbstractDatasetCommand<DatasetVersion> {

    private final DatasetVersion incomingVersion;
    private final List<DatasetRelationDTO> relationDTOs;

    public UpdateDatasetVersionWithRelationsCommand(Dataset dataset, DatasetVersion incomingVersion,
            List<DatasetRelationDTO> relationDTOs, DataverseRequest request) {
        super(request, dataset);
        this.incomingVersion = incomingVersion;
        this.relationDTOs = relationDTOs;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        Dataset dataset = getDataset();
        DatasetVersion managedVersion;

        if (dataset.getLatestVersion().isDraft()) {
            DatasetVersion editVersion = dataset.getOrCreateEditVersion();
            editVersion.setDatasetFields(incomingVersion.getDatasetFields());
            editVersion.setTermsOfUseAndAccess(incomingVersion.getTermsOfUseAndAccess());
            editVersion.getTermsOfUseAndAccess().setDatasetVersion(editVersion);

            if (!TermsOfUseAndAccessValidator.isTOUAValid(editVersion.getTermsOfUseAndAccess(), null)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.toua.invalid"), this);
            }

            Dataset managedDataset = ctxt.engine().submit(new UpdateDatasetVersionCommand(dataset, getRequest()));
            managedVersion = managedDataset.getOrCreateEditVersion();
        } else {
            if (relationDTOs == null && dataset.getLatestVersion().getRelations() != null) {
                incomingVersion.setRelations(dataset.getLatestVersion().getRelations().stream()
                        .map(relation -> relation.copy(incomingVersion))
                        .toList());
            }

            if (!TermsOfUseAndAccessValidator.isTOUAValid(incomingVersion.getTermsOfUseAndAccess(), null)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.toua.invalid"), this);
            }

            managedVersion = ctxt.engine().submit(new CreateDatasetVersionCommand(getRequest(), dataset, incomingVersion));
        }

        if (relationDTOs != null) {
            ctxt.engine().submit(new ReplaceDatasetRelationsCommand(managedVersion, relationDTOs, getRequest()));
        }

        return managedVersion;
    }
}
