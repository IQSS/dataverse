package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * Thrown when a user has not specified "force" on a potentially dangerous
 * Command.
 */
public class UnforcedCommandException extends IllegalCommandException {

    public UnforcedCommandException(String message, Command aCommand) {
        super(message, aCommand);
    }

}
