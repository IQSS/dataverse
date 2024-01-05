package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * An exception raised when a command cannot be executed, due to the
 * issuing user being rate limited.
 *
 * @author
 */
public class RateLimitCommandException extends CommandException {

    public RateLimitCommandException(String message, Command aCommand) {
        super(message, aCommand);
    }
}
