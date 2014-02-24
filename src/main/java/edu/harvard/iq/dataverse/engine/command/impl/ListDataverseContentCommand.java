package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.LinkedList;
import java.util.List;

/**
 * Lists the content of a dataverse - both datasets and dataverses.
 * @author michael
 */
@RequiredPermissions( Permission.Access )
public class ListDataverseContentCommand extends AbstractCommand<List<DvObject>>{
	private final Dataverse dvToList;
	
	public ListDataverseContentCommand(DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
		dvToList = anAffectedDataverse;
	}
	
	@Override
	public List<DvObject> execute(CommandContext ctxt) throws CommandException {
		LinkedList<DvObject> result = new LinkedList<>();
		result.addAll(ctxt.datasets().findByOwnerId(dvToList.getId()));
		result.addAll(ctxt.dataverses().findByOwnerId(dvToList.getId()));
		return result;
	}
	
}
