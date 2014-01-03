package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.Dataverse;

/**
 * Convenience class for implementing the {@link Command} interface.
 * @author michael
 * @param <R> The result type of the command.
 */
public abstract class AbstractCommand<R> implements Command<R> {
	
	private final Dataverse affectedDataverse;

	public AbstractCommand(Dataverse affectedDataverse) {
		this.affectedDataverse = affectedDataverse;
	}
	
	@Override
	public Dataverse getAffectedDataverse() {
		return affectedDataverse;
	}
	
}
