/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Assign a in a dataverse to a user.
 * @author michael
 */
@RequiredPermissions( Permission.AssignRole )
public class AssignRoleCommand extends AbstractCommand<RoleAssignment> {
	
	private final DataverseRole role;
	private final RoleAssignee grantee;
	private final DvObject defPoint;
	
	/**
	 * @param anAssignee The user being granted the role
	 * @param aRole the role being granted to the user
	 * @param assignmentPoint the dataverse on which the role is granted.
	 * @param issuingUser the user issuing the command.
	 */
	public AssignRoleCommand(RoleAssignee anAssignee, DataverseRole aRole, DvObject assignmentPoint, User issuingUser) {
		super(issuingUser, assignmentPoint);
		role = aRole;
		grantee = anAssignee;
		defPoint = assignmentPoint;
	}

	@Override
	public RoleAssignment execute(CommandContext ctxt) throws CommandException {
		// TODO make sure the role is defined on the dataverse.
		RoleAssignment roleAssignment = new RoleAssignment(role, grantee,defPoint);
		return ctxt.roles().save(roleAssignment);
	}
	
}
