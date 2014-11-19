package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.List;

/**
 * 
 * @author michael
 */
//@todo should this command exist for other dvObjects
@RequiredPermissions( Permission.ManageDataversePermissions )
public class ListRoleAssignments extends AbstractCommand<List<RoleAssignment>> {
	
	private final Dataverse definitionPoint;
	public ListRoleAssignments(User aUser, Dataverse aDefinitionPoint) {
		super(aUser, aDefinitionPoint);
		definitionPoint = aDefinitionPoint;
	}

	@Override
	public List<RoleAssignment> execute(CommandContext ctxt) throws CommandException {
		return ctxt.permissions().assignmentsOn(definitionPoint);
	}
	
}
