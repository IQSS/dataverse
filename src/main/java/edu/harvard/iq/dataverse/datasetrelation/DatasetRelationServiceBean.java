package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * Service bean for managing dataset relations.
 * It uses an injected DatasetRelationAlgorithm to compute and filter dataset relations.
 *
 * @author Vera Clemens (ZB MED)
 */
@Stateless
@Named
public class DatasetRelationServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetRelationServiceBean.class.getCanonicalName());

    @PersistenceContext
    private EntityManager em;

    @EJB
    private DatasetRelationTypeServiceBean relationTypeService;

    @EJB
    private DatasetServiceBean datasetService;

    @Inject
    private DatasetRelationAlgorithm algorithm;

    public void deleteAllDatasetRelationsFor(DatasetVersion v) {
        em.createNamedQuery("DatasetRelation.removeRelationsByDatasetVersionId")
                .setParameter("versionId", v.getId())
                .executeUpdate();
    }

    public void deleteAllDatasetRelationsInvolving(Dataset dataset) {
        em.createQuery("DELETE FROM InternalDatasetRelation rel WHERE rel.relatedDataset = :dataset")
                .setParameter("dataset", dataset)
                .executeUpdate();
        em.createQuery("DELETE FROM DatasetRelation rel WHERE rel.dataset = :dataset")
                .setParameter("dataset", dataset)
                .executeUpdate();
    }

    public DatasetRelation getDatasetRelationById(Long id) {
        try {
            return em.createNamedQuery("DatasetRelation.getRelationById", DatasetRelation.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<DatasetRelation> getDatasetRelationsDefinedAt(DatasetVersion version) {
        return em.createNamedQuery("DatasetRelation.getRelationsDefinedAtDatasetVersionId", DatasetRelation.class)
                .setParameter("versionId", version.getId())
                .getResultList();
    }

    public void deleteDatasetRelationById(Long id) {
        em.createNamedQuery("DatasetRelation.deleteRelationById")
          .setParameter("id", id)
          .executeUpdate();
    }

    public List<DatasetRelation> getDatasetRelationsFor(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset) {
        return algorithm.getRelations(d, v, relationTypeNames, datasetTypeNames, relationSources, limit, offset);
    }

    public List<Object[]> getDatasetRelationFacetCountsFor(Dataset d, DatasetVersion v, String groupBy,
            List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources) {
        return algorithm.getRelationFacetCounts(d, v, groupBy, relationTypeNames, datasetTypeNames, relationSources);
    }

    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v) {
        return algorithm.getTotalDatasetRelationCountFor(d, v, null, null, null);
    }

    public Long getTotalDatasetRelationCountFor(Dataset d, DatasetVersion v, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources) {
        return algorithm.getTotalDatasetRelationCountFor(d, v, relationTypeNames, datasetTypeNames, relationSources);
    }

    public DatasetRelation addDatasetRelation(DatasetRelation relation) {
        em.persist(relation);
        em.flush();
        return relation;
    }

    /**
     * A relation is a duplicate when it has the same endpoints, definition
     * point, and relation type as an existing relation.
     */
    public boolean isDuplicate(DatasetRelation relation) {
        if (relation instanceof InternalDatasetRelation internalRelation) {
            var query = em.createQuery("SELECT COUNT(rel) FROM InternalDatasetRelation rel "
                    + "WHERE rel.dataset = :dataset AND rel.relatedDataset = :relatedDataset "
                    + "AND rel.definitionPoint = :definitionPoint AND rel.relationType.id = :relationTypeId", Long.class)
                    .setParameter("dataset", relation.getDataset())
                    .setParameter("relatedDataset", internalRelation.getRelatedDataset())
                    .setParameter("definitionPoint", relation.getDefinitionPoint())
                    .setParameter("relationTypeId", relation.getRelationType().getId());
            return query.getSingleResult() > 0;
        } else if (relation instanceof ExternalDatasetRelation externalRelation) {
            var query = em.createQuery("SELECT COUNT(rel) FROM ExternalDatasetRelation rel "
                    + "WHERE rel.dataset = :dataset AND rel.externalIdentifier = :externalIdentifier "
                    + "AND rel.definitionPoint = :definitionPoint AND rel.relationType.id = :relationTypeId", Long.class)
                    .setParameter("dataset", relation.getDataset())
                    .setParameter("externalIdentifier", externalRelation.getExternalIdentifier())
                    .setParameter("definitionPoint", relation.getDefinitionPoint())
                    .setParameter("relationTypeId", relation.getRelationType().getId());
            return query.getSingleResult() > 0;
        }
        throw new IllegalArgumentException("Unknown dataset relation type");
    }

    public boolean containsDuplicates(List<DatasetRelation> relations) {
        for (int i = 0; i < relations.size(); i++) {
            for (int j = i + 1; j < relations.size(); j++) {
                if (areDuplicates(relations.get(i), relations.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean areDuplicates(DatasetRelation first, DatasetRelation second) {
        if (!first.getDataset().equals(second.getDataset())
                || !first.getDefinitionPoint().equals(second.getDefinitionPoint())
                || !first.getRelationType().equals(second.getRelationType())) {
            return false;
        }
        if (first instanceof InternalDatasetRelation firstInternal
                && second instanceof InternalDatasetRelation secondInternal) {
            return firstInternal.getRelatedDataset().equals(secondInternal.getRelatedDataset());
        }
        if (first instanceof ExternalDatasetRelation firstExternal
                && second instanceof ExternalDatasetRelation secondExternal) {
            return firstExternal.getExternalIdentifier().equals(secondExternal.getExternalIdentifier());
        }
        return false;
    }

    public List<DatasetRelation> replaceAllDatasetRelationsFor(DatasetVersion v, List<DatasetRelation> newRelations) {
        List<DatasetRelation> existingRelations = getDatasetRelationsDefinedAt(v);

        Set<String> existingKeys = existingRelations.stream()
                .map(DatasetRelation::toKey)
                .collect(Collectors.toSet());
        Set<String> newKeys = newRelations.stream()
                .map(DatasetRelation::toKey)
                .collect(Collectors.toSet());

        List<DatasetRelation> toAdd = newRelations.stream()
                .filter(r -> !existingKeys.contains(r.toKey()))
                .toList();
        List<DatasetRelation> toRemove = existingRelations.stream()
                .filter(r -> !newKeys.contains(r.toKey()))
                .toList();

        if (!toRemove.isEmpty()) {
            List<Long> toRemoveIds = toRemove.stream().map(DatasetRelation::getId).toList();
            em.createQuery("DELETE FROM DatasetRelation dr WHERE dr.id IN :toRemoveIds")
                    .setParameter("toRemoveIds", toRemoveIds)
                    .executeUpdate();
        }
        for (DatasetRelation r : toAdd) {
            em.persist(r);
        }

        em.flush();

        // Re-fetch to ensure IDs are populated
        return getDatasetRelationsDefinedAt(v);
    }

    public DatasetRelation fromDTO(DatasetRelationDTO dto, DatasetVersion version) {
        Dataset d = version.getDataset();
        DatasetRelationType type;
        if (dto.getRelationTypeName() != null) {
            type = relationTypeService.findByName(dto.getRelationTypeName());
            if (type == null) {
                logger.severe("Failed to find dataset relation type with name " + dto.getRelationTypeName());
                return null;
            }
        } else {
            type = relationTypeService.getDefault();
            if (type == null) {
                logger.severe("Failed to find a default dataset relation type");
                return null;
            }
        }

        if (dto.getRelatedDatasetPid() != null) {
            Dataset relatedDataset = datasetService.findByGlobalId(dto.getRelatedDatasetPid());
            if (relatedDataset == null) {
                logger.severe("Failed to find related dataset with PID " + dto.getRelatedDatasetPid());
                return null;
            }
            return new InternalDatasetRelation(d, relatedDataset, type, version);
        } else if (dto.getExternalIdentifier() != null) {
            return new ExternalDatasetRelation(d, dto.getExternalIdentifier(), dto.getIdentifierScheme(), dto.getDatasetType(), type, version);
        } else {
            logger.severe("Relation DTO must have either relatedDatasetPid or externalIdentifier");
            return null;
        }
    }

}
