/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Assign a in a dataverse to a user.
 * @author michael
 */
@RequiredPermissions( Permission.GrantPermissions )
public class AssignRoleCommand extends AbstractCommand<RoleAssignment> {
	
	private final DataverseRole role;
	private final DataverseUser grantedUser;
	private final DvObject defPoint;
	
	/**
	 * @param aUser The user being granted the role
	 * @param aRole the role being granted to the user
	 * @param anAffectedDataverse the dataverse on which the role is granted.
	 * @param issuingUser the user issuing the command.
	 */
	public AssignRoleCommand(DataverseUser aUser, DataverseRole aRole, Dataverse anAffectedDataverse, DataverseUser issuingUser) {
		super(issuingUser, anAffectedDataverse);
		role = aRole;
		grantedUser = aUser;
		defPoint = anAffectedDataverse;
	}

	@Override
	public RoleAssignment execute(CommandContext ctxt) throws CommandException {
		// TODO make sure the role is defined on the dataverse.
		RoleAssignment roleAssignment = new RoleAssignment(role, grantedUser,defPoint);
		return ctxt.roles().save(roleAssignment);
	}
	
}
