package edu.harvard.iq.dataverse;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.Comparator;
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
    public List<MetadataBlock> findSystemMetadataBlocks(){
        return em.createNamedQuery("MetadataBlock.listSystem", MetadataBlock.class).getResultList();
    }

    public List<MetadataBlock> findMetadataBlocksByDataverseId(Long dataverse_id) {
        try {
            return em.createNamedQuery("MetadataBlock.findByDataverseId", MetadataBlock.class)
                    .setParameter("dataverse_id", dataverse_id)
                    .getResultList();
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public List<MetadataBlock> listMetadataBlocksDisplayedOnCreate(Dataverse ownerDataverse) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<MetadataBlock> criteriaQuery = criteriaBuilder.createQuery(MetadataBlock.class);
        Root<Dataverse> dataverseRoot = criteriaQuery.from(Dataverse.class);

        // Join metadataBlocks from Dataverse
        Join<Dataverse, MetadataBlock> metadataBlockJoin = dataverseRoot.join("metadataBlocks");

        // Join datasetFieldTypes from MetadataBlock
        Join<MetadataBlock, DatasetFieldType> datasetFieldTypeJoin = metadataBlockJoin.join("datasetFieldTypes");

        Predicate displayOnCreatePredicate = criteriaBuilder.isTrue(datasetFieldTypeJoin.get("displayOnCreate"));
        Predicate requiredPredicate = criteriaBuilder.isTrue(datasetFieldTypeJoin.get("required"));

        if (ownerDataverse != null) {
            // Ensure we filter for the specific Dataverse
            Predicate dataversePredicate = criteriaBuilder.equal(dataverseRoot.get("id"), ownerDataverse.getId());

            // Join DataverseFieldTypeInputLevel (LEFT JOIN)
            Join<Dataverse, DataverseFieldTypeInputLevel> datasetFieldTypeInputLevelJoin =
                    dataverseRoot.join("dataverseFieldTypeInputLevels", JoinType.LEFT);

            // Check if input level explicitly defines displayOnCreate
            Predicate inputLevelDisplayPredicate = criteriaBuilder.and(
                    criteriaBuilder.equal(datasetFieldTypeInputLevelJoin.get("datasetFieldType"), datasetFieldTypeJoin),
                    criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("displayOnCreate"))
            );

            // Check if input level explicitly defines required
            Predicate inputLevelRequiredPredicate = criteriaBuilder.and(
                    criteriaBuilder.equal(datasetFieldTypeInputLevelJoin.get("datasetFieldType"), datasetFieldTypeJoin),
                    criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("required"))
            );

            Predicate finalDisplayPredicate = criteriaBuilder.or(inputLevelDisplayPredicate, displayOnCreatePredicate);
            Predicate finalRequiredPredicate = criteriaBuilder.or(inputLevelRequiredPredicate, requiredPredicate);

            criteriaQuery.where(
                    dataversePredicate,
                    criteriaBuilder.or(finalDisplayPredicate, finalRequiredPredicate)
            );
        } else {
            // When ownerDataverse is null, we need to include fields that are either displayOnCreate=true OR required=true
            // We also need to ensure that fields from linked metadata blocks are included
            Predicate linkedFieldsPredicate = criteriaBuilder.and(
                    criteriaBuilder.isNotNull(datasetFieldTypeJoin.get("id")),
                    criteriaBuilder.or(displayOnCreatePredicate, requiredPredicate)
            );

            criteriaQuery.where(linkedFieldsPredicate);
        }

        criteriaQuery.select(metadataBlockJoin).distinct(true);

        List<MetadataBlock> result = em.createQuery(criteriaQuery).getResultList();

        // Order by id
        result.sort(Comparator.comparing(MetadataBlock::getId));

        return result;
    }
}
