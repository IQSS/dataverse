package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.List;

/**
 * An abstract command for operations that support pagination.
 * It handles the common logic for 'limit' and 'offset' parameters,
 * including validation.
 *
 * @param <T> The return type of the command's result.
 */
public abstract class AbstractPaginatedCommand<T> extends AbstractCommand<T> {

    protected final Integer limit;
    protected final Integer offset;

    public AbstractPaginatedCommand(DataverseRequest request, DvObject dvObject, Integer limit, Integer offset) {
        super(request, dvObject);
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public T execute(CommandContext ctxt) throws CommandException {
        validatePaginationParameter(limit, "limit");
        validatePaginationParameter(offset, "offset");
        return executeCommand(ctxt);
    }

    /**
     * The core logic of the command, executed after pagination parameters have been validated.
     * Subclasses must implement this method.
     *
     * @param ctxt The command context.
     * @return The result of the command.
     * @throws CommandException if an error occurs during command execution.
     */
    public abstract T executeCommand(CommandContext ctxt) throws CommandException;

    /**
     * Validates that a given pagination parameter is not negative.
     *
     * @param value The parameter's value.
     * @param name  The parameter's name, used for the error message.
     * @throws InvalidCommandArgumentsException if the value is negative.
     */
    private void validatePaginationParameter(Integer value, String name) throws InvalidCommandArgumentsException {
        if (value != null && value < 0) {
            // Both original commands used this specific bundle key. A more generic one could be introduced later.
            throw new InvalidCommandArgumentsException(
                    BundleUtil.getStringFromBundle("getFileVersionDifferencesCommand.errors.negativePaginationParam", List.of(name)),
                    this
            );
        }
    }
}
