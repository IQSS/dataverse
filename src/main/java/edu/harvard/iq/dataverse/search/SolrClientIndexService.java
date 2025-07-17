package edu.harvard.iq.dataverse.search;

import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.inject.Named;

/**
 * Solr client to provide insert/update/delete operations.
 * Don't use this service with queries to Solr, use {@link SolrClientService} instead.
 */
@Named
@Singleton
public class SolrClientIndexService extends AbstractSolrClientService {

    private static final Logger logger = Logger.getLogger(SolrClientIndexService.class.getCanonicalName());

    private SolrClient solrClient;

    @PostConstruct
    public void init() {
        solrClient = new ConcurrentUpdateHttp2SolrClient.Builder(
            getSolrUrl(), new Http2SolrClient.Builder().build()).build();
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
