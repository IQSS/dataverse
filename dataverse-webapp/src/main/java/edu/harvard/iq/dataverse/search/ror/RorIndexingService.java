package edu.harvard.iq.dataverse.search.ror;

import edu.harvard.iq.dataverse.search.RorSolrClient;
import io.vavr.control.Try;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service dedicate for indexing ROR data.
 */
@Stateless
public class RorIndexingService {

    private static final Logger logger = Logger.getLogger(RorIndexingService.class.getCanonicalName());

    @Inject
    @RorSolrClient
    private SolrClient solrServer;

    public UpdateResponse indexRorRecord(RorDto rorData) {

        Try.of(() -> solrServer.addBean(rorData))
           .onFailure(throwable -> logger.log(Level.WARNING, "Unable to add ror record with ror id: " + rorData.getRorId()));

        return Try.of(() -> solrServer.commit())
                  .getOrElseThrow(throwable -> new IllegalStateException("Unable to commit ror data to solr.", throwable));

    }
}
