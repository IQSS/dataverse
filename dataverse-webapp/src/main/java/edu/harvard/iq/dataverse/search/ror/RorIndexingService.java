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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    public Future<UpdateResponse> indexRorRecordsAsync(Collection<RorData> rorData) {

        List<RorDto> convertedData = rorData.stream()
                                            .map(ror -> rorConverter.toSolrDto(ror))
                                            .collect(Collectors.toList());

        String firstTenFailedRorIds = rorData.stream()
                                     .map(RorData::getRorId)
                                     .limit(10)
                                     .collect(Collectors.joining(","));

        CompletableFuture<UpdateResponse> updateResult = CompletableFuture.supplyAsync(() -> {
            Try.of(() -> solrServer.addBeans(convertedData))
               .onFailure(throwable -> logger.log(Level.WARNING, "Unable to add ror records with ror ids: " + firstTenFailedRorIds +
                       "; With record count of " + convertedData.size(), throwable));

            return Try.of(() -> solrServer.commit())
                      .getOrElseThrow(throwable -> new IllegalStateException("Unable to commit ror data to solr.", throwable));
        });
        return updateResult;
    }

    public UpdateResponse indexRorRecord(RorDto rorData) {

        Try.of(() -> solrServer.addBean(rorData))
           .onFailure(throwable -> logger.log(Level.WARNING, "Unable to add ror record with ror id: " + rorData.getRorId(), throwable));

        return Try.of(() -> solrServer.commit())
                  .getOrElseThrow(throwable -> new IllegalStateException("Unable to commit ror data to solr.", throwable));

    }
}
