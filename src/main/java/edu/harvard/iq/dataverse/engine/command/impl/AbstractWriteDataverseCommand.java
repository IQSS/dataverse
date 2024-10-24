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

        if (metadataBlocks != null && !metadataBlocks.isEmpty()) {
            dataverse.setMetadataBlockRoot(true);
            dataverse.setMetadataBlocks(metadataBlocks);
        }

        if (facets != null) {
            ctxt.facets().deleteFacetsFor(dataverse);

            if (!facets.isEmpty()) {
                dataverse.setFacetRoot(true);
            }

            int i = 0;
            for (DatasetFieldType df : facets) {
                ctxt.facets().create(i++, df, dataverse);
            }
        }

        if (inputLevels != null) {
            if (!inputLevels.isEmpty()) {
                dataverse.addInputLevelsMetadataBlocksIfNotPresent(inputLevels);
            }
            ctxt.fieldTypeInputLevels().deleteFacetsFor(dataverse);
            for (DataverseFieldTypeInputLevel inputLevel : inputLevels) {
                inputLevel.setDataverse(dataverse);
                ctxt.fieldTypeInputLevels().create(inputLevel);
            }
        }

        return ctxt.dataverses().save(dataverse);
    }

    abstract protected Dataverse innerExecute(CommandContext ctxt) throws IllegalCommandException;
}
