package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

// inspired by UpdateDataverseMetadataBlocksCommand and DeactivateUserCommand
@RequiredPermissions({})
public class UpdateMetadataBlockDatasetTypeAssociations extends AbstractCommand<MetadataBlock> {

    private DataverseRequest dataverseRequest;
    private MetadataBlock metadataBlock;
    private List<DatasetType> datasetTypes;

    public UpdateMetadataBlockDatasetTypeAssociations(DataverseRequest dataverseRequest, MetadataBlock metadataBlock, List<DatasetType> datasetTypes) {
        super(dataverseRequest, (DvObject) null);
        this.dataverseRequest = dataverseRequest;
        this.metadataBlock = metadataBlock;
        this.datasetTypes = datasetTypes;
    }

    @Override
    public MetadataBlock execute(CommandContext ctxt) throws CommandException {
        if (!getUser().isSuperuser()) {
            throw new PermissionException("Command can only be called by superusers.", this, null, null);
        }
        metadataBlock.setDatasetTypes(datasetTypes);
        MetadataBlock savedMetadataBlock = ctxt.em().merge(metadataBlock);
//        ctxt.em().flush();
        for (DatasetType datasetType : savedMetadataBlock.getDatasetTypes()) {
            System.out.println("type: " + datasetType.getName());
            List<MetadataBlock> blocks = datasetType.getMetadataBlocks();
            blocks.add(metadataBlock);
            // We filter the list through a set, so that all blocks are distinct.
            datasetType.setMetadataBlocks(new LinkedList<>(new HashSet<>(blocks)));
            ctxt.em().merge(datasetType);
        }
        // TODO save the block one more time?
        return savedMetadataBlock;
    }

}
