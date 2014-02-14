package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.List;

/**
 * 
 * @author michael
 */
@RequiredPermissions( Permission.GrantPermissions )
public class ListRoleAssignments extends AbstractCommand<List<RoleAssignment>> {
	
	private final Dataverse definitionPoint;
	public ListRoleAssignments(DataverseUser aUser, Dataverse aDefinitionPoint) {
		super(aUser, aDefinitionPoint);
		definitionPoint = aDefinitionPoint;
	}

	@Override
	public List<RoleAssignment> execute(CommandContext ctxt) throws CommandException {
		return ctxt.permissions().assignmentsOn(definitionPoint);
	}
	
}
