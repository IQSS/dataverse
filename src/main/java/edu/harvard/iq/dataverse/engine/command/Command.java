package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 * Base interface for all commands running on Dataverse.
 * @author michael
 * @param <R> The type of result this command returns.
 */
public interface Command<R> {

	/**
	 * Override this method to execute the actual command.
	 * @param ctxt the context on which the command work. All dependency injections, if any, should be done using this.
	 * @return A result. May be {@code null}
	 * @throws CommandException If anything goes wrong.
	 */
	public R execute( CommandContext ctxt ) throws CommandException;
	
	
	/**
	 * Retrieves the dataverse this command works on. Used by the {@link DataverseEngine} 
	 * to validate that the user
	 * has the permissions required to execute {@code this} commnad.
	 * @return The dataverse on which the command will work
	 */
	public Dataverse getAffectedDataverse();
}
