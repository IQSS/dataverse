package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.ArrayList;
import java.util.List;

@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseInputLevelsCommand extends AbstractCommand<Dataverse> {
    private final Dataverse dataverse;
    private final List<DataverseFieldTypeInputLevel> inputLevelList;

    public UpdateDataverseInputLevelsCommand(Dataverse dataverse, DataverseRequest request, List<DataverseFieldTypeInputLevel> inputLevelList) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.inputLevelList = new ArrayList<>(inputLevelList);
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        if (inputLevelList == null || inputLevelList.isEmpty()) {
            throw new CommandException("Error while updating dataverse input levels: Input level list cannot be null or empty", this);
        }
        addInputLevelMetadataBlocks();
        dataverse.setMetadataBlockRoot(true);
        return ctxt.engine().submit(new UpdateDataverseCommand(dataverse, null, null, getRequest(), inputLevelList));
    }

    private void addInputLevelMetadataBlocks() {
        List<MetadataBlock> dataverseMetadataBlocks = dataverse.getMetadataBlocks();
        for (DataverseFieldTypeInputLevel inputLevel : inputLevelList) {
            MetadataBlock inputLevelMetadataBlock = inputLevel.getDatasetFieldType().getMetadataBlock();
            if (!dataverseHasMetadataBlock(dataverseMetadataBlocks, inputLevelMetadataBlock)) {
                dataverseMetadataBlocks.add(inputLevelMetadataBlock);
            }
        }
        dataverse.setMetadataBlocks(dataverseMetadataBlocks);
    }

    private boolean dataverseHasMetadataBlock(List<MetadataBlock> dataverseMetadataBlocks, MetadataBlock metadataBlock) {
        return dataverseMetadataBlocks.stream().anyMatch(block -> block.getId().equals(metadataBlock.getId()));
    }
}
