package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelation;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationIndexing;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ejb.EJBException;

import java.util.List;

/**
 * Creates a new relation for a given dataset.
 *
 * @author Vera Clemens (ZB MED)
 */
@RequiredPermissions(Permission.EditDatasetRelations)
public class CreateDatasetRelationCommand extends AbstractCommand<DatasetRelation> {
    private final DatasetVersion version;

    private final DatasetRelationDTO relationDTO;

    public CreateDatasetRelationCommand(DatasetVersion version, DatasetRelationDTO relation, DataverseRequest aRequest) {
        super(aRequest, version.getDataset());
        this.version = version;
        this.relationDTO = relation;
    }

    @Override
    public DatasetRelation execute(CommandContext ctxt) throws CommandException {
        try {
            DatasetRelation relation = ctxt.datasetRelations().fromDTO(relationDTO, version);

            if (relation == null) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.invalid"), this);
            }
            if (ctxt.datasetRelations().isDuplicate(relation)) {
                throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.duplicate"), this);
            }

            DatasetRelation addedRelation = ctxt.datasetRelations().addDatasetRelation(relation);
            DatasetRelationIndexing.schedule(ctxt, version.getDataset(), List.of(addedRelation));
            return addedRelation;
        } catch (EJBException ex) {
            throw new CommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.create"), ex, this);
        }
    }

}
