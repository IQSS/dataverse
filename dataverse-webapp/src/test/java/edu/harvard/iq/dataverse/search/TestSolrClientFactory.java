package edu.harvard.iq.dataverse.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * CDI compliant factory of {@link SolrClient} objects
 * to be used in test environment.
 * <p>
 * Class will start solr server (with dataverse specific
 * configuration) inside docker container.
 * 
 * @author madryk
 * @see /docker/test_solr/Dockerfile in test resources
 */
public class TestSolrClientFactory extends SolrClientFactory {

    private static final Logger LOGGER = Logger.getLogger(TestSolrClientFactory.class.getCanonicalName());

    private static final String DEFAULT_SOLR_TEST_PORT = "8984";

    // -------------------- LOGIC --------------------
    
    @Produces
    @Specializes
    public SolrClient produceSolrClient() throws IOException {
        String urlString = "http://localhost:" + resolveSolrPort() + "/solr/collection1";
        LOGGER.fine("Creating test SolrClient at url: " + urlString);
        
        return new HttpSolrClient.Builder(urlString).build();
    }

    @Produces
    @Specializes
    @RorSolrClient
    public SolrClient produceRorSolrClient() {
        String urlString = "http://localhost:" + resolveSolrPort() + "/solr/rorSuggestions";
        LOGGER.fine("Creating test SolrClient at url: " + urlString);

        return new HttpSolrClient.Builder(urlString).build();
    }
    
    public void disposeSolrClient(@Disposes SolrClient solrClient) throws IOException {
        solrClient.close();
    }

    // -------------------- PRIVATE --------------------

    private static String resolveSolrPort() {
        String port = System.getProperty("test.solr.port");
        if (port == null) {
            return DEFAULT_SOLR_TEST_PORT;
        }
        return port;
    }

}
