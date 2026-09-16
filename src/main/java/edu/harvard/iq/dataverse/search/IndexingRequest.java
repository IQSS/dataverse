package edu.harvard.iq.dataverse.search;

import java.util.Objects;

/**
 * Indexing work requested from inside a transaction.
 *
 * Fired as a CDI event by {@link IndexServiceBean} and {@link IndexAsync}, and run by
 * {@link IndexingRequestObserver} once the requesting transaction has committed, or
 * immediately when there is no transaction. Without this, the background index job
 * could read the database before the changes it should index were visible to it: a
 * dataset created in the same transaction got a permission document without its
 * creator and never had its index time recorded.
 */
public final class IndexingRequest {

    private final Runnable action;

    public IndexingRequest(Runnable action) {
        this.action = Objects.requireNonNull(action);
    }

    public void run() {
        action.run();
    }
}
