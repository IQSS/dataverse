package edu.harvard.iq.dataverse.search;

import java.util.Collection;
import java.util.List;

/**
 * Indexing work that must wait for the requesting transaction to commit.
 *
 * How it works:
 * <ol>
 * <li>a bean fires one of the records below from inside its transaction; nothing runs yet,</li>
 * <li>CDI holds the event, because its only observer is marked {@code AFTER_SUCCESS}, and
 *     delivers it once the transaction has committed (immediately when there is no
 *     transaction, never when it rolls back),</li>
 * <li>{@link IndexingRequestObserver} calls the matching {@code @Asynchronous} method, which
 *     does the work in the background as before.</li>
 * </ol>
 * Without this, the background job could read the database before the changes it should
 * index were visible to it: a dataset created in the same transaction got a permission
 * document without its creator and never had its index time recorded.
 *
 * The records carry ids, not entities. The background job loads what it indexes in a
 * persistence context of its own: an entity shared with the requesting thread is read by
 * both threads at the same time, and EclipseLink's unit of work is not thread-safe.
 */
public sealed interface IndexingRequest {

    /** Index a dataset in the background. */
    record IndexDataset(Long datasetId, boolean doNormalSolrDocCleanUp) implements IndexingRequest {}

    /** Index datasets in the background, one after the other. */
    record IndexDatasets(List<Long> datasetIds, boolean doNormalSolrDocCleanUp) implements IndexingRequest {}

    /** Record that a dataset has just been indexed. */
    record RecordIndexTime(Long datasetId) implements IndexingRequest {}

    /** Reindex the permissions of these collections, datasets or files and of their children. */
    record IndexPermissions(Collection<Long> dvObjectIds) implements IndexingRequest {}
}
