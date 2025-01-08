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
    private final boolean resetRelationsOnNullValues;

    public AbstractWriteDataverseCommand(Dataverse dataverse,
                                         Dataverse affectedDataverse,
                                         DataverseRequest request,
                                         List<DatasetFieldType> facets,
                                         List<DataverseFieldTypeInputLevel> inputLevels,
                                         List<MetadataBlock> metadataBlocks,
                                         boolean resetRelationsOnNullValues) {
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
        this.resetRelationsOnNullValues = resetRelationsOnNullValues;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        dataverse = innerExecute(ctxt);

        processMetadataBlocks();
        processFacets(ctxt);
        processInputLevels(ctxt);

        return ctxt.dataverses().save(dataverse);
    }

    private void processMetadataBlocks() {
        if (metadataBlocks != null && !metadataBlocks.isEmpty()) {
            dataverse.setMetadataBlockRoot(true);
            dataverse.setMetadataBlocks(metadataBlocks);
        } else if (resetRelationsOnNullValues) {
            dataverse.setMetadataBlockRoot(false);
            dataverse.clearMetadataBlocks();
        }
    }

    private void processFacets(CommandContext ctxt) {
        if (facets != null) {
            ctxt.facets().deleteFacetsFor(dataverse);
            dataverse.setDataverseFacets(new ArrayList<>());
           
            if (!facets.isEmpty()) {
                dataverse.setFacetRoot(true);
            }

            for (int i = 0; i < facets.size(); i++) {
                ctxt.facets().create(i, facets.get(i), dataverse);
            }
        } else if (resetRelationsOnNullValues) {
            ctxt.facets().deleteFacetsFor(dataverse);
            dataverse.setFacetRoot(false);
        }
    }

    private void processInputLevels(CommandContext ctxt) {
        if (inputLevels != null) {
            if (!inputLevels.isEmpty()) {
                dataverse.addInputLevelsMetadataBlocksIfNotPresent(inputLevels);
            }
            ctxt.fieldTypeInputLevels().deleteFacetsFor(dataverse);
            inputLevels.forEach(inputLevel -> {
                inputLevel.setDataverse(dataverse);
                ctxt.fieldTypeInputLevels().create(inputLevel);
            });
        } else if (resetRelationsOnNullValues) {
            ctxt.fieldTypeInputLevels().deleteFacetsFor(dataverse);
        }
    }

    abstract protected Dataverse innerExecute(CommandContext ctxt) throws IllegalCommandException;
}
