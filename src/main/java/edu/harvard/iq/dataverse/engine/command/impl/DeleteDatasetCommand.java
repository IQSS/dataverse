package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deletes a data set.
 * @author michael
 */
@RequiredPermissions( Permission.DestructiveEdit )
public class DeleteDatasetCommand extends AbstractVoidCommand {
	
	private final Dataset doomed;

	public DeleteDatasetCommand(Dataset doomed, DataverseUser aUser) {
		super(aUser, doomed.getOwner());
		this.doomed = doomed;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		if ( doomed.isReleased() ) {
			throw new IllegalCommandException("Cannot delete a released dataset", this);
		}
		
		final Dataset managedDoomed = ctxt.em().merge(doomed);
		
		// files
		for ( DataFile df : managedDoomed.getFiles() ) {
			ctxt.engine().submit( new DeleteDataFileCommand(df, getUser(), managedDoomed.getOwner()) );
		}
		
		// versions
		for ( DatasetVersion ver : managedDoomed.getVersions() ) {
			Logger.getLogger(DeleteDatasetCommand.class.getName()).log(Level.INFO, "deleting " + ver );
			DatasetVersion managed = ctxt.em().merge(ver);
			Logger.getLogger(DeleteDatasetCommand.class.getName()).log(Level.INFO, " - Managed: " + managed );
			ctxt.em().remove( managed );
		}
		
		// dataset
		ctxt.em().remove(managedDoomed);
	}
	
}
