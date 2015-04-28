package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.ArrayList;

/**
 * Deletes a {@link Dataverse} - but only if it is empty.
 *
 * @author michael
 */
@RequiredPermissionsMap({
    @RequiredPermissions(dataverseName = "doomed", value = Permission.DeleteDataverse)
})
public class DeleteDataverseCommand extends AbstractVoidCommand {

    private final Dataverse doomed;

    public DeleteDataverseCommand(User u, Dataverse aDoomedDataverse) {
        super(u, dv("doomed", aDoomedDataverse), dv("owner", aDoomedDataverse.getOwner()));
        doomed = aDoomedDataverse;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        // Make sure we don't delete root
        if (doomed.getOwner() == null) {
            throw new IllegalCommandException("Cannot delete the root dataverse", this);
        }

        // make sure the dataverse is emptyw
        if (ctxt.dvObjects().hasData(doomed)) {
            throw new IllegalCommandException("Cannot delete non-empty dataverses", this);
        }

	// if we got here, we can delete
	// Metadata blocks - cant delete metadatablocks
         /* Don't seem to need to do this SEK 10/23/14
         for (MetadataBlock block : doomed.getMetadataBlocks(true) ) {
         MetadataBlock merged =  ctxt.em().merge(block);
         ctxt.em().remove(merged);
         } */
        
        //TODO Look at roles upon delete when permissions complete
        //SEK 10/23/14
        // ASSIGNMENTS
        for (RoleAssignment ra : ctxt.roles().directRoleAssignments(doomed)) {
            ctxt.em().remove(ra);
        }
        // ROLES
        for (DataverseRole ra : ctxt.roles().findByOwnerId(doomed.getId())) {
            ctxt.em().remove(ra);
        }

        // FACETS handled with cascade on dataverse

        // Input Level
        for (DataverseFieldTypeInputLevel inputLevel : doomed.getDataverseFieldTypeInputLevels()) {
            DataverseFieldTypeInputLevel merged = ctxt.em().merge(inputLevel);
            ctxt.em().remove(merged);
        }
        doomed.setDataverseFieldTypeInputLevels(new ArrayList());
        // DATAVERSE
        Dataverse doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);
        // Remove from index        
        ctxt.index().delete(doomed);
    }
}
