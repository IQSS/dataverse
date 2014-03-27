package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * Thrown when a command fails to execute.
 * @author michael
 */
public class CommandExecutionException extends CommandException {

	public CommandExecutionException(String message, Throwable cause, Command aCommand) {
		super(message, cause, aCommand);
	}
	
	public CommandExecutionException(String message, Command aCommand) {
		super(message, aCommand);
	}
	
}
