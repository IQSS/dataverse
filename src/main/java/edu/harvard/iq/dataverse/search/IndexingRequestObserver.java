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
            case IndexingRequest.IndexDataset r -> indexService.indexDatasetInBackground(r.dataset(), r.doNormalSolrDocCleanUp());
            case IndexingRequest.IndexDatasetById r -> indexService.indexDatasetInBackground(r.datasetId(), r.doNormalSolrDocCleanUp());
            case IndexingRequest.IndexDatasets r -> indexService.indexDatasetListInBackground(r.datasets(), r.doNormalSolrDocCleanUp());
            case IndexingRequest.RecordIndexTime r -> indexService.updateLastIndexedTime(r.datasetId());
            case IndexingRequest.IndexRole r -> indexAsync.indexRoleInBackground(r.roleAssignment());
            case IndexingRequest.IndexRoles r -> indexAsync.indexRolesInBackground(r.dvObjects());
        }
    }
}
