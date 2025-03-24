package edu.harvard.iq.dataverse.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.inject.Named;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 * 
 * This singleton is dedicated to initializing the Http2SolrClient, used by
 * the application to talk to the search engine, and serving it to all the
 * other classes that need it.
 * This ensures that we are using one client only - as recommended by the 
 * documentation. 
 */
@Named
@Singleton
public class SolrClientService extends AbstractSolrClientService {
    private static final Logger logger = Logger.getLogger(SolrClientService.class.getCanonicalName());
    
    private SolrClient solrClient;
    
    @PostConstruct
    public void init() {
        solrClient = new Http2SolrClient.Builder(getSolrUrl()).build();
    }
    
    @PreDestroy
    public void close() {
        close(solrClient);
    }

    public SolrClient getSolrClient() {
        // Should never happen - but? 
        if (solrClient == null) {
            init(); 
        }
        return solrClient;
    }

    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }
}
