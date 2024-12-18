package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.List;

@RequiredPermissions({})
public class UpdateDatasetTypeLinksToMetadataBlocksCommand extends AbstractVoidCommand {

    final DatasetType datasetType;
    List<MetadataBlock> metadataBlocks;

    public UpdateDatasetTypeLinksToMetadataBlocksCommand(DataverseRequest dataverseRequest, DatasetType datasetType, List<MetadataBlock> metadataBlocks) {
        super(dataverseRequest, (DvObject) null);
        this.datasetType = datasetType;
        this.metadataBlocks = metadataBlocks;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException("Update dataset type links to metadata block command can only be called by superusers.",
                    this, null, null);
        }
        datasetType.setMetadataBlocks(metadataBlocks);
        ctxt.em().merge(datasetType);
    }

}
