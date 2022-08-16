package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseMetadataBlockFacet;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

import java.util.List;

/**
 *
 * @author adaybujeda
 */
@RequiredPermissions( Permission.EditDataverse )
public class UpdateMetadataBlockFacetsCommand extends AbstractCommand<Dataverse> {

    private final Dataverse editedDv;
    private final List<DataverseMetadataBlockFacet> metadataBlockFacets;

    public UpdateMetadataBlockFacetsCommand(DataverseRequest aRequest, Dataverse editedDv, List<DataverseMetadataBlockFacet> metadataBlockFacets) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.metadataBlockFacets = metadataBlockFacets;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        if (!editedDv.isMetadataBlockFacetRoot()) {
            throw new IllegalCommandException("Cannot update metadata blocks facets when dataverse has metadata block facet root set to false", this);
        }

        editedDv.setMetadataBlockFacets(metadataBlockFacets);
        Dataverse updated = ctxt.dataverses().save(editedDv);
        return updated;
    }

    // Visible for testing
    public Dataverse getEditedDataverse() {
        return this.editedDv;
    }

    // Visible for testing
    public List<DataverseMetadataBlockFacet> getMetadataBlockFacets() {
        return this.metadataBlockFacets;
    }

}
