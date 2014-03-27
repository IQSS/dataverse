package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * Base class for exceptions thrown by commands being submitted to the system.
 * @author michael
 */
public class CommandException extends Exception {
	
	private final Command failedCommand;
	
	
	public CommandException(String message, Command aCommand) {
		super(message);
		failedCommand = aCommand;
	}

	public CommandException(String message, Throwable cause, Command aCommand) {
		super(message, cause);
		failedCommand = aCommand;
	}

	public Command getFailedCommand() {
		return failedCommand;
	}
	
}
