package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Create a new role in a dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.GrantPermissions )
public class CreateRoleCommand extends AbstractCommand<DataverseRole> {
	
	private final DataverseRole created;
	private final Dataverse dv;
	
	public CreateRoleCommand(DataverseRole aRole, DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
		created = aRole;
		dv = anAffectedDataverse;
	}

	@Override
	public DataverseRole execute(CommandContext ctxt) throws CommandException {
		dv.addRole(created);
		return ctxt.roles().save(created);
	}
	
}
