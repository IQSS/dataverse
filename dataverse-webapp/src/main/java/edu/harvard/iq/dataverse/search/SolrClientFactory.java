package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import javax.ejb.EJB;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * CDI compliant factory of {@link SolrClient} objects. 
 * 
 * @author madryk
 */
public class SolrClientFactory {
    
    private static final Logger LOGGER = Logger.getLogger(SolrClientFactory.class.getCanonicalName());
    
    @EJB
    private SettingsServiceBean settingsService;
    
    
    // -------------------- LOGIC --------------------
    
    @Produces
    public SolrClient produceSolrClient() throws IOException {
        String urlString = "http://" + settingsService.getValueForKey(Key.SolrHostColonPort) + "/solr/collection1";
        LOGGER.fine("Creating SolrClient at url: " + urlString);
        
        return new HttpSolrClient.Builder(urlString).build();
    }
    
    public void disposeSolrClient(@Disposes SolrClient solrClient) throws IOException {
        solrClient.close();
    }
}
