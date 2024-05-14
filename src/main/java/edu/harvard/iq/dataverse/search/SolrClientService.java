/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.Timer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

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

    @FunctionalInterface
    public static interface HeavyOperation<T, R> {
        R apply(T t) throws SolrServerException, IOException;
    }

    private static final Logger logger = Logger.getLogger(SolrClientService.class.getCanonicalName());
    
    // semaphore for heavy operations
    private static final Semaphore SOLR_HEAVY_OPERATIONS_SEMAPHORE = new Semaphore(JvmSettings.MAX_HEAVY_SOLR_OPERATIONS.lookupOptional(Integer.class).orElse(1), true);

    @Inject
    @Metric(name = "solr_heavy_operation_permit_wait_time", absolute = true, unit = MetricUnits.NANOSECONDS,
            description = "Displays how long does it take to receive a permit to do a heavy operation on Solr")
    Timer solrPermitWaitTimer;

    @Inject
    @Metric(name = "heavy_solr_operation_time", absolute = true, unit = MetricUnits.NANOSECONDS,
            description = "Displays how long does it take to perform a heavy Solr operation")
    Timer heavyOpTimer;
    
    @EJB
    SystemConfig systemConfig;
    
    private SolrClient solrClient;
    
    @PostConstruct
    public void init() {
        // Get from MPCONFIG. Might be configured by a sysadmin or simply return the default shipped with
        // resources/META-INF/microprofile-config.properties.
        String protocol = JvmSettings.SOLR_PROT.lookup();
        String path = JvmSettings.SOLR_PATH.lookup();
        
        String urlString = protocol + "://" + systemConfig.getSolrHostColonPort() + path;
        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, JvmSettings.MAX_SOLR_CONNECTIONS.lookupOptional(String.class).orElse("10000"));
        solrClient = new HttpSolrClient.Builder(urlString).withInvariantParams(params).build();
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

    public SolrResponse doHeavyOperation(HeavyOperation<SolrClient, SolrResponseBase> operation) throws SolrServerException, IOException {
        try {
            acquirePermitFromSemaphore();
            try (var timeContext = heavyOpTimer.time()) {
                return operation.apply(getSolrClient());
            }
        } catch (InterruptedException e) {
            logger.severe("Acquiring permit for heavy solr operation failed: interrupted. Granting one anyway.");
            return operation.apply(getSolrClient());
        } finally {
            SOLR_HEAVY_OPERATIONS_SEMAPHORE.release();
        }
    }

    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }
    
    public void reInitialize() {
        close(); 
        init();
    }
    
    /**
     * Try to acquire a permit from the semaphore avoiding too many parallel heavy operations, potentially overwhelming Solr.
     * This method will time the duration waiting for the permit, allowing heavy operations performance to be measured.
     * @throws InterruptedException
     */
    private void acquirePermitFromSemaphore() throws InterruptedException {
        try (var timeContext = solrPermitWaitTimer.time()) {
            SOLR_HEAVY_OPERATIONS_SEMAPHORE.acquire();
        }
    }
}
