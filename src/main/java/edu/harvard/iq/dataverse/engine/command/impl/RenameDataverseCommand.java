package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.UndoableEdit )
public class RenameDataverseCommand extends AbstractCommand<Dataverse>{
	
	private final String newName;
	private final Dataverse renamed;
	
	public RenameDataverseCommand( User aUser, Dataverse aDataverse, String aNewName ) {
		super( aUser, aDataverse );
		newName = aNewName;
		renamed = aDataverse;
	}
	
	@Override
	public Dataverse execute(CommandContext ctxt) throws CommandException {
		if ( newName.trim().isEmpty() ) {
			throw new IllegalCommandException("Dataverse name cannot be empty", this);
		}
		
		renamed.setName(newName);
		return ctxt.dataverses().save(renamed);
	}
	
}
