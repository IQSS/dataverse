package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author adaybujeda
 */
@RequiredPermissions( Permission.EditDataverse )
public class UpdateMetadataBlockFacetRootCommand extends AbstractCommand<Dataverse> {

    private final Dataverse editedDv;
    private final boolean metadataBlockFacetRoot;

    public UpdateMetadataBlockFacetRootCommand(DataverseRequest aRequest, Dataverse editedDv, boolean metadataBlockFacetRoot) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.metadataBlockFacetRoot = metadataBlockFacetRoot;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        if(editedDv.isMetadataBlockFacetRoot() != metadataBlockFacetRoot) {
            // Update metadata block facets when root changes value
            // if you set root to be false (i.e. inherit), it should clear the blocks.
            // if you set to true (i.e. use your own), it should make a copy of what is in the parent
            List<DataverseMetadataBlockFacet> newBlockFacets = Collections.emptyList();
            if (metadataBlockFacetRoot) {
                newBlockFacets = editedDv.getMetadataBlockFacets().stream().map(blockFacet -> {
                    DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
                    metadataBlockFacet.setDataverse(editedDv);
                    metadataBlockFacet.setMetadataBlock(blockFacet.getMetadataBlock());
                    return metadataBlockFacet;
                }).collect(Collectors.toList());
            }
            editedDv.setMetadataBlockFacets(newBlockFacets);

            editedDv.setMetadataBlockFacetRoot(metadataBlockFacetRoot);
            return ctxt.dataverses().save(editedDv);
        }

        return editedDv;
    }

    // Visible for testing
    public Dataverse getEditedDataverse() {
        return this.editedDv;
    }

    // Visible for testing
    public boolean getMetadataBlockFacetRoot() {
        return metadataBlockFacetRoot;
    }
}
