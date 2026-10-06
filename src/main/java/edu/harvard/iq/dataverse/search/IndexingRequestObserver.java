package edu.harvard.iq.dataverse.search;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Carries out {@link IndexingRequest}s after the transaction they were fired in has
 * committed. When that transaction rolls back there is nothing to index and the
 * request is dropped.
 */
@Dependent
public class IndexingRequestObserver {

    private final IndexServiceBean indexService;
    private final IndexAsync indexAsync;

    @Inject
    public IndexingRequestObserver(IndexServiceBean indexService, IndexAsync indexAsync) {
        this.indexService = indexService;
        this.indexAsync = indexAsync;
    }

    public void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) IndexingRequest request) {
        switch (request) {
            case IndexingRequest.IndexDataset(var datasetId, var cleanUp) -> indexService.indexDatasetInBackground(datasetId, cleanUp);
            case IndexingRequest.IndexDatasets(var datasetIds, var cleanUp) -> indexService.indexDatasetListInBackground(datasetIds, cleanUp);
            case IndexingRequest.RecordIndexTime(var datasetId) -> indexService.updateLastIndexedTime(datasetId);
            case IndexingRequest.IndexPermissions(var dvObjectIds) -> indexAsync.indexPermissionsInBackground(dvObjectIds);
        }
    }
}
