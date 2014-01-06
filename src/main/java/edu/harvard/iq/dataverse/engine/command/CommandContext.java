package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;

/**
 * An interface for accessing Dataverse's resources, user info etc. Used by the
 * {@link Command} implementations to perform their intended actions.
 * 
 * @author michael
 */
public interface CommandContext {
	
	/**
	 * @return the service bean for datasets.
	 */
	public DatasetServiceBean datasets();
	
	public DataverseServiceBean dataverses();
}
