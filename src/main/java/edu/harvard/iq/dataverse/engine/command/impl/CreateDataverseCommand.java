package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.UndoableEdit )
public class CreateDataverseCommand extends AbstractCommand<Dataverse> {
	
	private final Dataverse created;

	public CreateDataverseCommand(Dataverse created, DataverseUser aUser) {
		super(aUser, created.getOwner());
		this.created = created;
	}
	
	
	@Override
	public Dataverse execute(CommandContext ctxt) throws CommandException {
		Dataverse managedDv = ctxt.dataverses().save(created);
		ctxt.permissions().setPermissionOn(getUser(), managedDv, EnumSet.allOf(Permission.class));
		String indexingResult = ctxt.indexing().indexDataverse(managedDv);
		Logger.getLogger(CreateDataverseCommand.class.getName()).log(Level.INFO, "during dataset save, indexing result was: {0}", indexingResult);
		return managedDv;
	}
	
}
