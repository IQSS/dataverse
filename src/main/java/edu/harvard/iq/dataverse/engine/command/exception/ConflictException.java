package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * An exception raised when a command cannot be executed because it clashes with the current state, e.g. when trying to
 * create a resource that already exists.
 */
public class ConflictException extends CommandException {

    public ConflictException(String message, Command aCommand) {
        super(message, aCommand);
    }
}
