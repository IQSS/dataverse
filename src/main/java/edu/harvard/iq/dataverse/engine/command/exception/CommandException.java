package edu.harvard.iq.dataverse.engine.command.exception;

/**
 * Base class for exceptions thrown by commands being submitted to the system.
 * @author michael
 */
public class CommandException extends Exception {

	public CommandException(String message) {
		super(message);
	}

	public CommandException(String message, Throwable cause) {
		super(message, cause);
	}

}
