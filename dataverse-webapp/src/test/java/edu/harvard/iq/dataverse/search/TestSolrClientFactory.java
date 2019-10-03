package edu.harvard.iq.dataverse.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

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
    private static final org.slf4j.Logger SOLR_CONTAINER_LOGGER = org.slf4j.LoggerFactory.getLogger("SolrContainer");
    
    private static final GenericContainer<?> SOLR_CONTAINER;
    
    static {
        SOLR_CONTAINER = new GenericContainer<>(new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "/docker/test_solr/Dockerfile")
                    .withFileFromClasspath("schema.xml", "/docker/test_solr/schema.xml")
                    .withFileFromClasspath("solrconfig.xml", "/docker/test_solr/solrconfig.xml")
                )
                .withLogConsumer(new Slf4jLogConsumer(SOLR_CONTAINER_LOGGER))
                .withCommand("solr-precreate collection1 /opt/solr/server/solr/configsets/dataverse_config")
                .withExposedPorts(8983);
        
        SOLR_CONTAINER.start();
    }
    
    // -------------------- LOGIC --------------------
    
    @Produces
    @Specializes
    public SolrClient produceSolrClient() throws IOException {
        String urlString = "http://localhost:" + SOLR_CONTAINER.getMappedPort(8983) + "/solr/collection1";
        LOGGER.fine("Creating test SolrClient at url: " + urlString);
        
        return new HttpSolrClient.Builder(urlString).build();
    }
    
    public void disposeSolrClient(@Disposes SolrClient solrClient) throws IOException {
        solrClient.close();
    }
    
}
