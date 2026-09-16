package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import java.util.Collection;
import java.util.List;

/**
 * Indexing work requested from inside a transaction.
 *
 * Fired as a CDI event by {@link IndexServiceBean} and {@link IndexAsync}, and carried
 * out by {@link IndexingRequestObserver} once the requesting transaction has committed,
 * or immediately when there is no transaction. Without this, the background index job
 * could read the database before the changes it should index were visible to it: a
 * dataset created in the same transaction got a permission document without its
 * creator and never had its index time recorded.
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
