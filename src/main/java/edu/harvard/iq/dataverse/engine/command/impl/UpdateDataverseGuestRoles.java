package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.HashSet;
import java.util.Set;

/**
 * Update the roles the guest user has on a dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.GrantPermissions )
public class UpdateDataverseGuestRoles extends AbstractVoidCommand {
	private final Set<DataverseRole> newRoleSet;
	private final Dataverse dv;

	public UpdateDataverseGuestRoles(Set<DataverseRole> roleSet, DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
		newRoleSet = roleSet;
		dv = anAffectedDataverse;
	}
	
	
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		// TODO validate that the users won't lock themselves out
		
		DataverseUser guest = ctxt.users().findGuestUser();
		Set<DataverseRole> existingUserRoles = new HashSet<>();
		for ( RoleAssignment ra : ctxt.roles().directRoleAssignments(guest, dv)) {
			existingUserRoles.add( ra.getRole() );
		}
		Set<DataverseRole> toAdd = new HashSet<>( newRoleSet );
		toAdd.removeAll( existingUserRoles );
		
		Set<DataverseRole> toRemove = new HashSet<>(existingUserRoles);
		toRemove.removeAll( newRoleSet );
		
		for ( DataverseRole dr : toAdd ) {
			ctxt.roles().save( new RoleAssignment(dr, guest, dv) );
		}
		
		if ( ! toRemove.isEmpty() ) {
			ctxt.roles().revoke(toRemove, guest, dv);
		}
	}
	
}
