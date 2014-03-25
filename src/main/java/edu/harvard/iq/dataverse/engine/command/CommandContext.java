package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.SearchServiceBean;
import javax.persistence.EntityManager;

/**
 * An interface for accessing Dataverse's resources, user info etc. Used by the
 * {@link Command} implementations to perform their intended actions.
 * 
 * @author michael
 */
public interface CommandContext {
	
	public EntityManager em();
	
	public DvObjectServiceBean dvObjects();
	
	public DatasetServiceBean datasets();
	
	public DataverseServiceBean dataverses();
	
	public DataverseRoleServiceBean roles();
	
	public DataverseUserServiceBean users();
	
	public IndexServiceBean indexing();
	
	public SearchServiceBean search();
	
	public PermissionServiceBean permissions();
	
	public DataverseFacetServiceBean facets(); 
	
}
