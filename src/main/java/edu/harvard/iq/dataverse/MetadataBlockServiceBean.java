package edu.harvard.iq.dataverse;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.List;

/**
 * @author michael
 */
@Stateless
@Named
public class MetadataBlockServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public MetadataBlock save(MetadataBlock mdb) {
        return em.merge(mdb);
    }

    public List<MetadataBlock> listMetadataBlocks() {
        return listMetadataBlocks(false);
    }

    public List<MetadataBlock> listMetadataBlocks(boolean onlyDisplayedOnCreate) {
        if (onlyDisplayedOnCreate) {
            return listMetadataBlocksDisplayedOnCreate(null);
        }
        return em.createNamedQuery("MetadataBlock.listAll", MetadataBlock.class).getResultList();
    }

    public MetadataBlock findById(Long id) {
        return em.find(MetadataBlock.class, id);
    }

    public MetadataBlock findByName(String name) {
        try {
            return em.createNamedQuery("MetadataBlock.findByName", MetadataBlock.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public List<MetadataBlock> listMetadataBlocksDisplayedOnCreate(Dataverse ownerDataverse) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<MetadataBlock> criteriaQuery = criteriaBuilder.createQuery(MetadataBlock.class);
        Root<MetadataBlock> metadataBlockRoot = criteriaQuery.from(MetadataBlock.class);
        Join<MetadataBlock, DatasetFieldType> datasetFieldTypeJoin = metadataBlockRoot.join("datasetFieldTypes");
        
        if (ownerDataverse != null) {
            Root<Dataverse> dataverseRoot = criteriaQuery.from(Dataverse.class);
            Join<Dataverse, DataverseFieldTypeInputLevel> datasetFieldTypeInputLevelJoin = 
                dataverseRoot.join("dataverseFieldTypeInputLevels", JoinType.LEFT);

            // Subquery to check if the input level exists
            Subquery<Long> inputLevelSubquery = criteriaQuery.subquery(Long.class);
            Root<DataverseFieldTypeInputLevel> subqueryRoot = inputLevelSubquery.from(DataverseFieldTypeInputLevel.class);
            inputLevelSubquery.select(criteriaBuilder.literal(1L))
                .where(
                    criteriaBuilder.equal(subqueryRoot.get("dataverse"), dataverseRoot),
                    criteriaBuilder.equal(subqueryRoot.get("datasetFieldType"), datasetFieldTypeJoin)
                );

            // Predicate for displayOnCreate in the input level
            Predicate displayOnCreateInputLevelPredicate = criteriaBuilder.and(
                datasetFieldTypeInputLevelJoin.get("datasetFieldType").in(metadataBlockRoot.get("datasetFieldTypes")),
                criteriaBuilder.isNotNull(datasetFieldTypeInputLevelJoin.get("displayOnCreate")),
                criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("displayOnCreate")));

            // Predicate for required fields
            Predicate requiredPredicate = criteriaBuilder.and(
                datasetFieldTypeInputLevelJoin.get("datasetFieldType").in(metadataBlockRoot.get("datasetFieldTypes")),
                criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("required")));

            // Predicate for default displayOnCreate (when there is no input level or when input level exists but doesn't have displayOnCreate set)
            Predicate defaultDisplayOnCreatePredicate = criteriaBuilder.or(
                // Case 1: No input level and required is true
                criteriaBuilder.and(
                    criteriaBuilder.not(criteriaBuilder.exists(inputLevelSubquery)),
                    criteriaBuilder.isTrue(datasetFieldTypeJoin.get("required"))
                ),
                // Case 2: (No input level OR input level exists but displayOnCreate is null) AND displayOnCreate is true in datasetFieldType
                criteriaBuilder.and(
                    criteriaBuilder.or(
                        criteriaBuilder.not(criteriaBuilder.exists(inputLevelSubquery)),
                        criteriaBuilder.isNull(datasetFieldTypeInputLevelJoin.get("displayOnCreate"))
                    ),
                    criteriaBuilder.isTrue(datasetFieldTypeJoin.get("displayOnCreate"))
                )
            );

            Predicate unionPredicate = criteriaBuilder.or(
                displayOnCreateInputLevelPredicate,
                requiredPredicate,
                defaultDisplayOnCreatePredicate
            );

            criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(dataverseRoot.get("id"), ownerDataverse.getId()),
                metadataBlockRoot.in(dataverseRoot.get("metadataBlocks")),
                unionPredicate
            ));
        } else {
            // When ownerDataverse is null, we need to include fields that are either displayOnCreate=true OR required=true
            Predicate displayOnCreatePredicate = criteriaBuilder.isTrue(datasetFieldTypeJoin.get("displayOnCreate"));
            Predicate requiredPredicate = criteriaBuilder.isTrue(datasetFieldTypeJoin.get("required"));
            
            // We also need to ensure that fields from linked metadata blocks are included
            Predicate linkedFieldsPredicate = criteriaBuilder.and(
                criteriaBuilder.isNotNull(datasetFieldTypeJoin.get("id")),
                criteriaBuilder.or(displayOnCreatePredicate, requiredPredicate)
            );
            
            criteriaQuery.where(linkedFieldsPredicate);
        }

        criteriaQuery.select(metadataBlockRoot).distinct(true)
                .orderBy(criteriaBuilder.desc(metadataBlockRoot.get("id")));
        return em.createQuery(criteriaQuery).getResultList();
    }
}
