package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * Deletes a {@link Dataverse} - but only if it is empty.
 * @author michael
 */
@RequiredPermissionsMap({
	@RequiredPermissions( dataverseName = "doomed", value = Permission.DestructiveEdit )
})
public class DeleteDataverseCommand extends AbstractVoidCommand {
	
	private final Dataverse doomed;
	
	public DeleteDataverseCommand( User u, Dataverse aDoomedDataverse ) {
		super(u, dv("doomed", aDoomedDataverse),dv("owner",aDoomedDataverse.getOwner()));
		doomed = aDoomedDataverse;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		// Make sure we don't delete root
		if ( doomed.getOwner() == null ) {
			throw new IllegalCommandException("Cannot delete the root dataverse", this);
		}
		
		// make sure the dataverse is emptyw
		if ( ctxt.dvObjects().hasData(doomed) ) {
			throw new IllegalCommandException("Cannot delete non-empty dataverses", this);
		}
		
		// if we got here, we can delete
		
		// Metadata blocks
		for (MetadataBlock block : doomed.getMetadataBlocks(true) ) {
			ctxt.em().remove(block);
		}
		// ASSIGNMENTS
		for ( RoleAssignment ra : ctxt.roles().directRoleAssignments(doomed) ) {
			ctxt.em().remove(ra);
		}
		// ROLES
		for ( DataverseRole ra : ctxt.roles().findByOwnerId(doomed.getId()) ) {
			ctxt.em().remove(ra);
		}
		// FACETS
		for ( DataverseFacet facet : doomed.getDataverseFacets(true) ) {
			ctxt.em().remove(facet);
		}       
		
        // DATAVERSE
                Dataverse doomedAndMerged = ctxt.em().merge(doomed);
		ctxt.em().remove(doomedAndMerged);
                // Remove from index        
                        ctxt.index().delete(doomed);
	}	
}
