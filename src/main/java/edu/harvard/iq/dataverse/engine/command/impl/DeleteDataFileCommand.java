package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Deletes a data file, both DB entity and filesystem object.
 * @author michael
 */
public class DeleteDataFileCommand extends AbstractVoidCommand {
	
	private final DataFile doomed;

	public DeleteDataFileCommand(DataFile doomed, DataverseUser aUser, Dataverse anAffectedDataverse) {
		super(aUser, anAffectedDataverse);
		this.doomed = doomed;
	}
	
	@Override
	protected void executeImpl(CommandContext ctxt) throws CommandException {
		if ( doomed.isReleased() ) {
			throw new IllegalCommandException("Cannot delete a released file", this);
		}
		
		// Delete the file from the file system
		Path filePath = Paths.get(doomed.getFileSystemName());
		if ( Files.exists(filePath) ) {
			try {
				Files.delete(filePath);
			} catch (IOException ex) {
				throw new CommandExecutionException("Error deleting physical file '" + doomed.getFileSystemName() + "' while deleting DataFile " + doomed.getName(), ex, this );
			}
		}
		
		// Delete the file from the DB.
		ctxt.em().remove(doomed);
		
		// TODO Remove ingest data etc.
		
	}
	
}
