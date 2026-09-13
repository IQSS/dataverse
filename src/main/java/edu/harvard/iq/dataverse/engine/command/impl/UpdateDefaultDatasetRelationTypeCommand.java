package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationType;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 * A superuser-only command for changing the default dataset relation type.
 */
@RequiredPermissions({})
public class UpdateDefaultDatasetRelationTypeCommand extends AbstractVoidCommand {

    private final DatasetRelationType relationType;

    public UpdateDefaultDatasetRelationTypeCommand(DataverseRequest request, DatasetRelationType relationType) {
        super(request, (DvObject) null);
        this.relationType = relationType;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws IllegalCommandException {
        if (!getUser().isSuperuser()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.default.superuserOnly"), this);
        }
        ctxt.datasetRelationTypes().setDefault(relationType);
    }
}
