package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of DatasetRelationAlgorithm using JPA and SQL queries.
 *
 * @author Vera Clemens (ZB MED)
 */
@ApplicationScoped
public class SqlDirectDatasetRelationAlgorithm implements DatasetRelationAlgorithm {

    @PersistenceContext
    private EntityManager em;

    private static final String WITH_LATEST_RELEASED_VERSIONS =
            " WITH latest_released_versions AS MATERIALIZED ( " +
            "     SELECT DISTINCT ON (dataset_id) id, dataset_id " +
            "     FROM datasetversion " +
            "     WHERE versionstate = 'RELEASED' " +
            "     ORDER BY dataset_id, id DESC " +
            " ), ";

    private static final String WITH_CANDIDATE_RELATIONS =
            " candidate_relations AS ( " +
            // Relations defined on the requested version.
            "     SELECT dr.id, 0 AS definition_point_priority " +
            "     FROM datasetrelation dr " +
            "     WHERE dr.definitionpoint_id = ? " +
            "     UNION ALL " +
            // Incoming relations defined on the latest released version of another dataset.
            "     SELECT dr.id, 1 AS definition_point_priority " +
            "     FROM datasetrelation dr " +
            "     JOIN latest_released_versions lrv ON dr.definitionpoint_id = lrv.id " +
            "     WHERE lrv.dataset_id != ? " +
            "       AND dr.relateddataset_id = ? " +
            " ) ";

    // Normalize and deduplicate relation types from the requested dataset's perspective.
    private static final String WITH_DEDUPLICATED_RELATIONS =
            " , normalized_candidate_relations AS ( " +
            "     SELECT cr.id, cr.definition_point_priority, dr.relation_source AS normalized_relation_source, " +
            "         CASE " +
            "             WHEN dr.dataset_id = ? THEN dr.relationtype_id " +
            "             ELSE rt.inverse_id " +
            "         END AS normalized_relation_type_id, " +
            "         CASE WHEN dr.relation_source = 'internal' " +
            "             THEN CASE WHEN dr.dataset_id = ? THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END " +
            "             ELSE dr.externalidentifier " +
            "         END AS normalized_related_dataset " +
            "     FROM candidate_relations cr " +
            "     JOIN datasetrelation dr ON cr.id = dr.id " +
            "     LEFT JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id " +
            " ), " +
            " deduplicated_relations AS ( " +
            "     SELECT DISTINCT ON (normalized_relation_source, normalized_relation_type_id, normalized_related_dataset) " +
            "         id, definition_point_priority " +
            "     FROM normalized_candidate_relations " +
            "     ORDER BY normalized_relation_source, normalized_relation_type_id, normalized_related_dataset, definition_point_priority, id " +
            " ) ";

    private static final String GET_TOTAL_RELATION_COUNT_QUERY_BASE =
            " SELECT COUNT(*) " +
            " FROM deduplicated_relations ddr " +
            " JOIN datasetrelation dr ON ddr.id = dr.id ";

    private static final String GET_RELATIONS_QUERY_BASE =
            " SELECT dr.* " +
            " FROM deduplicated_relations ddr " +
            " JOIN datasetrelation dr ON ddr.id = dr.id ";

    private static final String JOIN_RELATION_TYPES = 
            // Get information about dataset relation types
            " JOIN datasetrelationtype rt ON dr.relationtype_id = rt.id " +
            " LEFT JOIN datasetrelationtype inv ON rt.inverse_id = inv.id ";

    private static final String WHERE_RELATION_TYPE_MATCHES = 
            // The relation type must match the given one (inverted if necessary)
            " ( " +
            "    (dr.dataset_id = ? AND rt.name IN (?)) " +
            "    OR " +
            "    (dr.relateddataset_id = ? AND inv.name IN (?)) " +
            " ) ";

    private static final String JOIN_DATASET_TYPES = 
            " LEFT JOIN dataset d_related ON dr.relation_source = 'internal' " +
            "     AND (CASE WHEN dr.dataset_id = ? THEN dr.relateddataset_id ELSE dr.dataset_id END) = d_related.id " +
            " LEFT JOIN datasettype dt ON d_related.datasettype_id = dt.id " +
            "     OR (dr.relation_source = 'external' AND dt.displayname = dr.datasettype " +
            "         AND NOT EXISTS (SELECT 1 FROM datasettype duplicate_dt " +
            "                         WHERE duplicate_dt.displayname = dt.displayname AND duplicate_dt.id <> dt.id)) ";

    private static final String WHERE_DATASET_TYPE_MATCHES = 
            " ((dr.relation_source = 'internal' AND dt.name IN (?)) " +
            " OR (dr.relation_source = 'external' AND (dr.datasettype IN (?) OR dt.name IN (?)))) ";

    private static final String WHERE_RELATION_SOURCE_MATCHES =
            " (dr.relation_source IN (?)) ";

    private static final String ORDER_BY_REQUESTED_DATASET_FIRST =
            " ORDER BY ddr.definition_point_priority ASC, dr.id ASC ";

    @SuppressWarnings("unchecked")
    @Override
    public List<DatasetRelation> getRelations(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset) {
        StringBuilder sql = new StringBuilder();

        sql.append(WITH_LATEST_RELEASED_VERSIONS)
                .append(WITH_CANDIDATE_RELATIONS)
                .append(WITH_DEDUPLICATED_RELATIONS)
                .append(GET_RELATIONS_QUERY_BASE);

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(JOIN_RELATION_TYPES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(JOIN_DATASET_TYPES);
        }

        sql.append(" WHERE 1 = 1 ");

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES.replace("(?)", 
                    "(" + relationTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)", 
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_SOURCE_MATCHES.replace("(?)", 
                    "(" + relationSources.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        sql.append(ORDER_BY_REQUESTED_DATASET_FIRST);

        Query query = em.createNativeQuery(sql.toString(), DatasetRelation.class);
        int i = 1;

        // WITH_CANDIDATE_RELATIONS
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        // WITH_DEDUPLICATED_RELATIONS
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            // WHERE_RELATION_TYPE_MATCHES
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // WHERE_DATASET_TYPE_MATCHES
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            // WHERE_RELATION_SOURCE_MATCHES
            for (String source : relationSources) {
                query.setParameter(i++, source);
            }
        }

        return (List<DatasetRelation>) query.setMaxResults(limit)
                .setFirstResult(offset)
                .getResultList();
    }

    @Override
    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources) {
        StringBuilder sql = new StringBuilder();

        sql.append(WITH_LATEST_RELEASED_VERSIONS)
                .append(WITH_CANDIDATE_RELATIONS)
                .append(WITH_DEDUPLICATED_RELATIONS)
                .append(GET_TOTAL_RELATION_COUNT_QUERY_BASE);

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(JOIN_RELATION_TYPES);
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(JOIN_DATASET_TYPES);
        }

        sql.append(" WHERE 1 = 1 ");

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES.replace("(?)", 
                    "(" + relationTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)", 
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_SOURCE_MATCHES.replace("(?)", 
                    "(" + relationSources.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }

        Query query = em.createNativeQuery(sql.toString());
        int i = 1;

        // WITH_CANDIDATE_RELATIONS
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        // WITH_DEDUPLICATED_RELATIONS
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());

        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // JOIN_DATASET_TYPES
            query.setParameter(i++, d.getId());
        }

        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            // WHERE_RELATION_TYPE_MATCHES
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
        }
        
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            // WHERE_DATASET_TYPE_MATCHES
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }

        if (relationSources != null && !relationSources.isEmpty()) {
            // WHERE_RELATION_SOURCE_MATCHES
            for (String source : relationSources) {
                query.setParameter(i++, source);
            }
        }

        return (Long) query.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object[]> getRelationFacetCounts(Dataset d, DatasetVersion v, String groupBy,
            List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources) {
        boolean groupByDatasetType = "datasetType".equals(groupBy);
        boolean needsRelationTypes = !groupByDatasetType || (relationTypeNames != null && !relationTypeNames.isEmpty());
        boolean needsDatasetTypes = datasetTypeNames != null && !datasetTypeNames.isEmpty();
        String select = groupByDatasetType
                ? " SELECT CASE WHEN dr.relation_source = 'external' AND dt.id IS NULL THEN NULL ELSE dt.name END, "
                        + "CASE WHEN dr.relation_source = 'external' AND dt.id IS NULL THEN dr.datasettype ELSE dt.displayname END, "
                        + "CASE WHEN dr.relation_source = 'external' AND dt.id IS NULL THEN NULL ELSE dt.description END, COUNT(*) "
                : " SELECT CASE WHEN dr.dataset_id = ? THEN rt.name ELSE inv.name END, "
                        + "CASE WHEN dr.dataset_id = ? THEN rt.displayname ELSE inv.displayname END, "
                        + "CASE WHEN dr.dataset_id = ? THEN rt.description ELSE inv.description END, COUNT(*) ";

        StringBuilder sql = new StringBuilder();
        sql.append(WITH_LATEST_RELEASED_VERSIONS)
                .append(WITH_CANDIDATE_RELATIONS)
                .append(WITH_DEDUPLICATED_RELATIONS)
                .append(select)
                .append(" FROM deduplicated_relations ddr JOIN datasetrelation dr ON ddr.id = dr.id ");
        if (needsRelationTypes) {
            sql.append(JOIN_RELATION_TYPES);
        }
        if (groupByDatasetType) {
            sql.append(JOIN_DATASET_TYPES);
        } else if (needsDatasetTypes) {
            sql.append(JOIN_DATASET_TYPES);
        }
        sql.append(" WHERE 1 = 1 ");
        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_TYPE_MATCHES.replace("(?)",
                    "(" + relationTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            sql.append(" AND ").append(WHERE_DATASET_TYPE_MATCHES.replace("(?)",
                    "(" + datasetTypeNames.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        if (relationSources != null && !relationSources.isEmpty()) {
            sql.append(" AND ").append(WHERE_RELATION_SOURCE_MATCHES.replace("(?)",
                    "(" + relationSources.stream().map(n -> "?").collect(Collectors.joining(",")) + ")"));
        }
        if (groupByDatasetType) {
            sql.append(" AND (dr.relation_source = 'internal' OR dr.datasettype IS NOT NULL) ")
                    .append(" GROUP BY 1, 2, 3 ORDER BY COUNT(*) DESC, 1 ASC ");
        } else {
            sql.append(" GROUP BY 1, 2, 3 ORDER BY COUNT(*) DESC, 1 ASC ");
        }

        Query query = em.createNativeQuery(sql.toString());
        int i = 1;
        query.setParameter(i++, v.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        query.setParameter(i++, d.getId());
        if (!groupByDatasetType) {
            query.setParameter(i++, d.getId());
            query.setParameter(i++, d.getId());
            query.setParameter(i++, d.getId());
        }
        if (groupByDatasetType || needsDatasetTypes) {
            query.setParameter(i++, d.getId());
        }
        if (relationTypeNames != null && !relationTypeNames.isEmpty()) {
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
            query.setParameter(i++, d.getId());
            for (String typeName : relationTypeNames) {
                query.setParameter(i++, typeName);
            }
        }
        if (datasetTypeNames != null && !datasetTypeNames.isEmpty()) {
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
            for (String typeName : datasetTypeNames) {
                query.setParameter(i++, typeName);
            }
        }
        if (relationSources != null && !relationSources.isEmpty()) {
            for (String source : relationSources) {
                query.setParameter(i++, source);
            }
        }
        return (List<Object[]>) query.getResultList();
    }
}
