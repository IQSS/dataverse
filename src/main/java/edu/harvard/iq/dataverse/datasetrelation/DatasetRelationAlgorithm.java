package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import java.util.List;

/**
 * Interface for algorithms that calculate or retrieve relations for a dataset.
 * This allows swapping the implementation (e.g. from simple direct relation-based to complex graph-based clustering).
 *
 * @author Vera Clemens (ZB MED)
 */
public interface DatasetRelationAlgorithm {
    
    /**
     * Retrieves relations for a given dataset.
     * 
     * @param dataset The dataset for which to find relations.
     * @param version Optional dataset version for version-specific filtering.
     * @param relationTypeNames Optional filter by relation type names.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     * @param limit Maximum number of results.
     * @param offset Offset for pagination.
     * @return A list of DatasetRelation objects.
     */
    List<DatasetRelation> getRelations(Dataset dataset, DatasetVersion version, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset);

    /**
     * Retrieves relation facet counts using the same normalized relation set as listing.
     *
     * @param groupBy The field to group by ("relationType" or "datasetType").
     * @param relationTypeNames Optional filter by relation type names.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     */
    List<Object[]> getRelationFacetCounts(Dataset dataset, DatasetVersion version, String groupBy,
            List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources);

    /**
     * Retrieves the total number of relations returned for a dataset.
     * 
     * @param dataset The dataset.
     * @param version Optional dataset version for version-specific filtering.
     * @param relationTypeNames Optional filter by relation type names.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     * @return Total number of relations returned by {@link #getRelations} with the same filters.
     */
    Long getTotalDatasetRelationCountFor(Dataset dataset, DatasetVersion version, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources);
}
