package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelation;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationIndexing;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationType;
import edu.harvard.iq.dataverse.datasetrelation.ExternalDatasetRelation;
import edu.harvard.iq.dataverse.datasetrelation.InternalDatasetRelation;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.EJBException;

import java.util.List;

/**
 * Replaces all relations defined for a given dataset.
 *
 * @author Vera Clemens (ZB MED)
 */
@RequiredPermissions(Permission.EditDatasetRelations)
public class ReplaceDatasetRelationsCommand extends AbstractCommand<List<DatasetRelation>> {
    private final DatasetVersion version;

    private final List<DatasetRelationDTO> relationDTOs;

    private final boolean replacePublishedVersionDirectly;

    public ReplaceDatasetRelationsCommand(DatasetVersion version, List<DatasetRelationDTO> relations, DataverseRequest aRequest) {
        this(version, relations, aRequest, false);
    }

    public ReplaceDatasetRelationsCommand(DatasetVersion version, List<DatasetRelationDTO> relations, DataverseRequest aRequest,
            boolean replacePublishedVersionDirectly) {
        super(aRequest, version.getDataset());
        this.version = version;
        this.relationDTOs = relations;
        this.replacePublishedVersionDirectly = replacePublishedVersionDirectly;
    }

    @Override
    public List<DatasetRelation> execute(CommandContext ctxt) throws CommandException {
        try {
            DatasetVersion effectiveVersion;

            // Editing the latest published version creates an edit version, unless a superuser selected a published version explicitly.
            if (!version.isDraft() && !replacePublishedVersionDirectly) {
                effectiveVersion = version.getDataset().getOrCreateEditVersion();
                ctxt.engine().submit(new UpdateDatasetVersionCommand(version.getDataset(), getRequest()));
            } else {
                effectiveVersion = version;
            }

            List<DatasetRelation> relations = relationDTOs.stream()
                    .map(dto -> ctxt.datasetRelations().fromDTO(dto, effectiveVersion))
                    .toList();
            if (relations.contains(null)) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.invalid"), this);
            }
            if (ctxt.datasetRelations().containsDuplicates(relations)) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.duplicate"), this);
            }
            List<DatasetRelation> previousRelations = ctxt.datasetRelations().getDatasetRelationsDefinedAt(effectiveVersion);
            List<DatasetRelation> addedRelations = ctxt.datasetRelations().replaceAllDatasetRelationsFor(effectiveVersion, relations);

            DatasetRelationIndexing.scheduleChanges(ctxt, version.getDataset(), previousRelations, addedRelations);
            return addedRelations;
        } catch (EJBException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.replace"), ex, this);
        }
    }

}
