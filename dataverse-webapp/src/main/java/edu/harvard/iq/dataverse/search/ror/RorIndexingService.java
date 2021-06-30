package edu.harvard.iq.dataverse.search.ror;

import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.ror.RorConverter;
import edu.harvard.iq.dataverse.search.RorSolrClient;
import io.vavr.control.Try;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
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

    @Inject
    private RorConverter rorConverter;

    @Asynchronous
    public Future<UpdateResponse> indexRorRecordAsync(RorData rorData) {
        return CompletableFuture.supplyAsync(() -> indexRorRecord(rorConverter.toSolrDto(rorData)));
    }

    public UpdateResponse indexRorRecord(RorDto rorData) {

        Try.of(() -> solrServer.addBean(rorData))
           .onFailure(throwable -> logger.log(Level.WARNING, "Unable to add ror record with ror id: " + rorData.getRorId(), throwable));

        return Try.of(() -> solrServer.commit())
                  .getOrElseThrow(throwable -> new IllegalStateException("Unable to commit ror data to solr.", throwable));

    }
}
