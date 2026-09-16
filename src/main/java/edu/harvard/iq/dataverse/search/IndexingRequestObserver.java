package edu.harvard.iq.dataverse.search;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/**
 * Runs {@link IndexingRequest}s after the transaction they were fired in has committed.
 * When that transaction rolls back there is nothing to index and the request is dropped.
 */
@ApplicationScoped
public class IndexingRequestObserver {

    public void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) IndexingRequest request) {
        request.run();
    }
}
