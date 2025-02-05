package edu.harvard.iq.dataverse.search;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrClient;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;

/**
 * Generics methods for Solr clients implementations
 * 
 * @author jeromeroucou
 */
public abstract class AbstractSolrClientService {
    private static final Logger logger = Logger.getLogger(AbstractSolrClientService.class.getCanonicalName());

    @EJB
    SystemConfig systemConfig;

    public abstract void init();
    public abstract void close();
    public abstract SolrClient getSolrClient();
    public abstract void setSolrClient(SolrClient solrClient);

    public void close(SolrClient solrClient) {
        if (solrClient != null) {
            try {
                solrClient.close();
            } catch (IOException e) {
                logger.warning("Solr closing error: " + e);
            }
            solrClient = null;
        }
    }

    public void reInitialize() {
        close();
        init();
    }

    public String getSolrUrl() {
        // Get from MPCONFIG. Might be configured by a sysadmin or simply return the
        // default shipped with resources/META-INF/microprofile-config.properties.
        final String protocol = JvmSettings.SOLR_PROT.lookup();
        final String path = JvmSettings.SOLR_PATH.lookup();
        return protocol + "://" + this.systemConfig.getSolrHostColonPort() + path;
    }
}
