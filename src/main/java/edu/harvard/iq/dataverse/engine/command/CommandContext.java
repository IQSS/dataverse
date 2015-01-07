package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseFacetServiceBean;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.SearchServiceBean;
import edu.harvard.iq.dataverse.TemplateServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import javax.ejb.EJB;
import javax.persistence.EntityManager;

/**
 * An interface for accessing Dataverse's resources, user info etc. Used by the
 * {@link Command} implementations to perform their intended actions.
 * 
 * @author michael
 */
public interface CommandContext {
	
	public EntityManager em();
	
	public DataverseEngine engine();
	
	public DvObjectServiceBean dvObjects();
	
	public DatasetServiceBean datasets();
	
	public DataverseServiceBean dataverses();
	
	public DataverseRoleServiceBean roles();
	
	public BuiltinUserServiceBean users();
	
	public IndexServiceBean index();

        public SolrIndexServiceBean solrIndex();
	
	public SearchServiceBean search();
	
	public PermissionServiceBean permissions();
	
	public DataverseFacetServiceBean facets(); 
        
        public FeaturedDataverseServiceBean featuredDataverses();       
        
        public DataFileServiceBean files(); 
        
        public TemplateServiceBean templates();
        
        public DataverseFieldTypeInputLevelServiceBean fieldTypeInputLevels();
                   
        public DOIEZIdServiceBean doiEZId();
        
        public GuestbookServiceBean guestbooks();
        
        public GuestbookResponseServiceBean responses();
        
        public SettingsServiceBean settings();       
	
}
