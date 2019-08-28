/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

/**
 *
 * @author landreev
 * 
 * This singleton is dedicated to initializing the HttpSolrClient used by the 
 * application to talk to the search engine, and serving it to all the other
 * classes that need it. 
 * This ensures that we are using one client only - as recommended by the 
 * documentation. 
 */
@Named
@Singleton
public class SolrClientService {
    private static final Logger logger = Logger.getLogger(SolrClientService.class.getCanonicalName());
    
    @EJB
    SystemConfig systemConfig;
    
    private SolrClient solrClient;
    
    @PostConstruct
    public void init() {
        String urlString = "http://" + systemConfig.getSolrHostColonPort() + "/solr/collection1";
        solrClient = new HttpSolrClient.Builder(urlString).build();
        
    }
    
    @PreDestroy
    public void close() {
        if (solrClient != null) {
            try {
                solrClient.close();
            } catch (IOException e) {
                logger.warning("Solr closing error: " + e);
            }

            solrClient = null;
        }
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
    
    public void reInitialize() {
        close(); 
        init();
    }
}
