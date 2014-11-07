package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Revokes a role for a user on a dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.AssignRole )
public class RevokeRoleCommand extends AbstractVoidCommand {
	
	private final RoleAssignment toBeRevoked;

	public RevokeRoleCommand(RoleAssignment toBeRevoked, User aUser) {
		super(aUser, toBeRevoked.getDefinitionPoint());
		this.toBeRevoked = toBeRevoked;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		ctxt.roles().revoke(toBeRevoked);
	}
	
}
