package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;
import java.util.Map;

public class InvalidFieldsCommandException extends CommandException {

    private final Map<String, String> fieldErrors;

    /**
     * Constructs a new InvalidFieldsCommandException with the specified detail message,
     * command, and a map of field errors.
     *
     * @param message The detail message.
     * @param aCommand The command where the exception was encountered.
     * @param fieldErrors A map containing the fields as keys and the reasons for their errors as values.
     */
    public InvalidFieldsCommandException(String message, Command aCommand, Map<String, String> fieldErrors) {
        super(message, aCommand);
        this.fieldErrors = fieldErrors;
    }

    /**
     * Gets the map of fields and their corresponding error messages.
     *
     * @return The map of field errors.
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Returns a string representation of this exception, including the
     * message and details of the invalid fields and their errors.
     *
     * @return A string representation of this exception.
     */
    @Override
    public String toString() {
        return super.toString() + ", fieldErrors=" + fieldErrors;
    }
}
