package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;

/**
 * Exception thrown when a {@link Command} is executed with invalid or malformed arguments.
 * <p>
 * This exception typically indicates that the input parameters provided to the command
 * do not meet the required criteria (e.g., missing fields, invalid formats, or other
 * constraints).
 * </p>
 * <p>
 * Example scenarios:
 * <ul>
 *   <li>A required argument is null or missing.</li>
 *   <li>An argument is in an invalid format (e.g., a malformed email address).</li>
 *   <li>Arguments violate business rules or constraints.</li>
 * </ul>
 */
public class InvalidCommandArgumentsException extends CommandException {

    public InvalidCommandArgumentsException(String message, Command aCommand) {
        super(message, aCommand);
    }
}
