package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class for implementing the {@link Command} interface.
 * @author michael
 * @param <R> The result type of the command.
 */
public abstract class AbstractCommand<R> implements Command<R> {
	
	private final Map<String,Dataverse> affectedDataverses;
	private final DataverseUser user;
	
	static protected class DvNamePair {
		final String name;
		final Dataverse dv;

		public DvNamePair(String name, Dataverse dv) {
			this.name = name;
			this.dv = dv;
		}
	}
	
	/**
	 * Convenience method to name affected dataverses.
	 * @param s the name
	 * @param d the dataverse
	 * @return the named pair
	 */
	protected static DvNamePair p( String s, Dataverse d ) {
		return new DvNamePair(s,d);
	}
	
	public AbstractCommand(DataverseUser aUser, Dataverse anAffectedDataverse) {
		this( aUser, p("",anAffectedDataverse));
	}
	
	public AbstractCommand(DataverseUser aUser, DvNamePair dvp, DvNamePair... more ) {
		user = aUser;
		affectedDataverses = new HashMap<>();
		affectedDataverses.put( dvp.name, dvp.dv );
		for ( DvNamePair p : more ) {
			affectedDataverses.put( p.name, p.dv );
		}
	}
	
	public AbstractCommand( DataverseUser aUser, Map<String, Dataverse> someAffectedDataversae ) {
		user = aUser;
		affectedDataverses = someAffectedDataversae;
	}
	
	@Override
	public Map<String,Dataverse> getAffectedDataverses() {
		return affectedDataverses;
	}
	
	@Override
	public DataverseUser getUser() {
		return user;
	}
	
}
