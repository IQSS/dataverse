package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract base class for commands that perform write operations on {@link Dataverse}s.
 */
abstract class AbstractWriteDataverseCommand extends AbstractCommand<Dataverse> {

    protected Dataverse dataverse;
    private final List<DataverseFieldTypeInputLevel> inputLevels;
    private final List<DatasetFieldType> facets;
    protected final List<MetadataBlock> metadataBlocks;

    public AbstractWriteDataverseCommand(Dataverse dataverse,
                                         Dataverse affectedDataverse,
                                         DataverseRequest request,
                                         List<DatasetFieldType> facets,
                                         List<DataverseFieldTypeInputLevel> inputLevels,
                                         List<MetadataBlock> metadataBlocks) {
        super(request, affectedDataverse);
        this.dataverse = dataverse;
        if (facets != null) {
            this.facets = new ArrayList<>(facets);
        } else {
            this.facets = null;
        }
        if (inputLevels != null) {
            this.inputLevels = new ArrayList<>(inputLevels);
        } else {
            this.inputLevels = null;
        }
        if (metadataBlocks != null) {
            this.metadataBlocks = new ArrayList<>(metadataBlocks);
        } else {
            this.metadataBlocks = null;
        }
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        dataverse = innerExecute(ctxt);

        processMetadataBlocks();
        processFacets(ctxt);
        processInputLevels(ctxt);

        return ctxt.dataverses().save(dataverse);
    }

    /*
      metadataBlocks = null - ignore
      metadataBlocks is empty - delete and inherit from parent
      metadataBlocks is not empty - set with new updated values
    */
    private void processMetadataBlocks() {
        if (metadataBlocks != null) {
            if (metadataBlocks.isEmpty()) {
                dataverse.setMetadataBlockRoot(false);
                dataverse.clearMetadataBlocks();
            } else {
                dataverse.setMetadataBlockRoot(true);
                dataverse.setMetadataBlocks(metadataBlocks);
            }
        }
    }

    /*
      facets = null - ignore
      facets is empty - delete and inherit from parent
      facets is not empty - set with new updated values
    */
    private void processFacets(CommandContext ctxt) {
        if (facets != null) {
            if (facets.isEmpty()) {
                ctxt.facets().deleteFacetsFor(dataverse);
                dataverse.setFacetRoot(false);
            } else {
                ctxt.facets().deleteFacetsFor(dataverse);
                dataverse.setDataverseFacets(new ArrayList<>());
                dataverse.setFacetRoot(true);
                for (int i = 0; i < facets.size(); i++) {
                    ctxt.facets().create(i, facets.get(i), dataverse);
                }
            }
        }
    }

    /*
      inputLevels = null - ignore
      inputLevels is empty - delete
      inputLevels is not empty - set with new updated values
    */
    private void processInputLevels(CommandContext ctxt) {
        if (inputLevels != null) {
            if (inputLevels.isEmpty()) {
                ctxt.fieldTypeInputLevels().deleteDataverseFieldTypeInputLevelFor(dataverse);
            } else {
                dataverse.addInputLevelsMetadataBlocksIfNotPresent(inputLevels);
                ctxt.fieldTypeInputLevels().deleteDataverseFieldTypeInputLevelFor(dataverse);
                inputLevels.forEach(inputLevel -> {
                    inputLevel.setDataverse(dataverse);
                    ctxt.fieldTypeInputLevels().create(inputLevel);
                });
            }
        }
    }

    abstract protected Dataverse innerExecute(CommandContext ctxt) throws IllegalCommandException;
}
