package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

import java.util.Collections;

/**
 *
 * @author adaybujeda
 */
@RequiredPermissions( Permission.EditDataverse )
public class DeleteMetadataBlockFacetsCommand extends AbstractCommand<Dataverse> {

    private final Dataverse editedDv;

    public DeleteMetadataBlockFacetsCommand(DataverseRequest aRequest, Dataverse editedDv) {
        super(aRequest, editedDv);
        this.editedDv = editedDv;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        editedDv.setMetadataBlockFacetRoot(false);
        editedDv.setMetadataBlockFacets(Collections.emptyList());
        Dataverse updated = ctxt.dataverses().save(editedDv);
        return updated;
    }

    // Visible for testing
    public Dataverse getEditedDataverse() {
        return this.editedDv;
    }
}
