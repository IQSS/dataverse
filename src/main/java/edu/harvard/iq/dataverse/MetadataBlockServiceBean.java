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
        Predicate displayOnCreatePredicate = criteriaBuilder.isTrue(datasetFieldTypeJoin.get("displayOnCreate"));

        if (ownerDataverse != null) {
            Root<Dataverse> dataverseRoot = criteriaQuery.from(Dataverse.class);
            criteriaQuery.where(criteriaBuilder.and(
                    criteriaBuilder.equal(dataverseRoot.get("id"), ownerDataverse.getId()),
                    metadataBlockRoot.in(dataverseRoot.get("metadataBlocks")),
                    displayOnCreatePredicate
            ));
        } else {
            criteriaQuery.where(displayOnCreatePredicate);
        }

        criteriaQuery.select(metadataBlockRoot).distinct(true);
        TypedQuery<MetadataBlock> typedQuery = em.createQuery(criteriaQuery);
        return typedQuery.getResultList();
    }
}
