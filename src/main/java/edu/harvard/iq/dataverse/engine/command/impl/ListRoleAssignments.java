package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author michael
 */
@RequiredPermissions( Permission.ManageDataversePermissions )
public class ListRoleAssignments extends AbstractCommand<List<RoleAssignment>> {
	
	private final DvObject definitionPoint;
	public ListRoleAssignments(DataverseRequest aRequest, DvObject aDefinitionPoint) {
		super(aRequest, aDefinitionPoint);
		definitionPoint = aDefinitionPoint;
	}

	@Override
	public List<RoleAssignment> execute(CommandContext ctxt) throws CommandException {
            if(definitionPoint.isInstanceofDataset()){
                List <RoleAssignment> retVal = new ArrayList();
                retVal.addAll(ctxt.permissions().assignmentsOn(definitionPoint));
                retVal.addAll(ctxt.permissions().assignmentsOn(definitionPoint.getOwner()));
                return retVal;
            }
		return ctxt.permissions().assignmentsOn(definitionPoint);
	}
	
}
