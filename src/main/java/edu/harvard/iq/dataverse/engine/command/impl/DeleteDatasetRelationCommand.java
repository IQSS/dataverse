package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.datasetrelation.DatasetRelation;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationIndexing;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.*;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.Collections;
import java.util.List;

/**
 * Deletes a dataset relation.
 *
 * @author Vera Clemens (ZB MED)
 */
@RequiredPermissions(Permission.EditDatasetRelations)
public class DeleteDatasetRelationCommand extends AbstractVoidCommand {

    private final DatasetRelation relation;

    public DeleteDatasetRelationCommand(DataverseRequest request, DatasetRelation relation) {
        super(request, relation.getDefinitionPoint().getDataset());
        this.relation = relation;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (relation.getDefinitionPoint().isReleased()
                && (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser())) {
            throw new PermissionException(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.editForbidden"),
                    this, Collections.singleton(Permission.EditDatasetRelations), relation.getDefinitionPoint().getDataset());
        }
        ctxt.datasetRelations().deleteDatasetRelationById(relation.getId());
        DatasetRelationIndexing.schedule(ctxt, relation.getDefinitionPoint().getDataset(), List.of(relation));
    }
}
