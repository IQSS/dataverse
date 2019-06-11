package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Base interface for the Dataverse Engine - the entity responsible for 
 * executing {@link Command}s.
 * 
 * @author michael
 */
public interface DataverseEngine {
	
	/**
	 * Submits a command for immediate execution.
	 * @param <R> The command result's type.
	 * @param aCommand The command to execute
	 * @return The result of the command execution.
	 * @throws CommandException 
	 */
	public <R> R submit( Command<R> aCommand ) throws CommandException;
}
