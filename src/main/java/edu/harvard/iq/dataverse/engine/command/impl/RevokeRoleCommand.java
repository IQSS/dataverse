package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Revokes a role for a user on a dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.GrantPermissions )
public class RevokeRoleCommand extends AbstractVoidCommand {
	
	private final RoleAssignment toBeRevoked;

	public RevokeRoleCommand(RoleAssignment toBeRevoked, DataverseUser aUser) {
		super(aUser, (Dataverse)toBeRevoked.getDefinitionPoint());
		this.toBeRevoked = toBeRevoked;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		ctxt.roles().revoke(toBeRevoked);
	}
	
}
