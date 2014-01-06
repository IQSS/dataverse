package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;

/**
 * Convenience class for implementing the {@link Command} interface.
 * @author michael
 * @param <R> The result type of the command.
 */
public abstract class AbstractCommand<R> implements Command<R> {
	
	private final Dataverse affectedDataverse;
	private final DataverseUser user;

	public AbstractCommand(DataverseUser aUser, Dataverse anAffectedDataverse) {
		user = aUser;
		affectedDataverse = anAffectedDataverse;
	}
	
	@Override
	public Dataverse getAffectedDataverse() {
		return affectedDataverse;
	}
	
	@Override
	public DataverseUser getUser() {
		return user;
	}
	
}
