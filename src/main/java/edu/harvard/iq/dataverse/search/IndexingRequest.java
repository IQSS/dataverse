package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
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
 */
public sealed interface IndexingRequest {

    /** Index a dataset in the background. */
    record IndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) implements IndexingRequest {}

    /** Index a dataset in the background, looking it up by id first. */
    record IndexDatasetById(Long datasetId, boolean doNormalSolrDocCleanUp) implements IndexingRequest {}

    /** Index datasets in the background, one after the other. */
    record IndexDatasets(List<Dataset> datasets, boolean doNormalSolrDocCleanUp) implements IndexingRequest {}

    /** Record that a dataset has just been indexed. */
    record RecordIndexTime(Long datasetId) implements IndexingRequest {}

    /** Reindex the permissions of the definition point of a role assignment. */
    record IndexRole(RoleAssignment roleAssignment) implements IndexingRequest {}

    /** Reindex the permissions of several definition points. */
    record IndexRoles(Collection<DvObject> dvObjects) implements IndexingRequest {}
}
