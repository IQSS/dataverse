package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 * Deletes a data set.
 * @author michael
 */
@RequiredPermissions( Permission.DestructiveEdit )
public class DeleteDatasetCommand extends DestroyDatasetCommand {
	
	private final Dataset doomed;

	public DeleteDatasetCommand(Dataset doomed, DataverseUser aUser) {
		super(doomed, aUser);
		this.doomed = doomed;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		if ( doomed.isReleased() ) {
			throw new IllegalCommandException("Cannot delete a released dataset", this);
		}
		super.executeImpl(ctxt);
	}
	
}
