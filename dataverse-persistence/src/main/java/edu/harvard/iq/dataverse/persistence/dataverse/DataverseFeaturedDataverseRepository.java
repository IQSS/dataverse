package edu.harvard.iq.dataverse.persistence.dataverse;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class DataverseFeaturedDataverseRepository extends JpaRepository<Long, DataverseFeaturedDataverse> {

    // -------------------- CONSTRUCTORS --------------------

    public DataverseFeaturedDataverseRepository() {
        super(DataverseFeaturedDataverse.class);
    }
    
    public DataverseFeaturedDataverseRepository(final EntityManager em) {
        
        super(DataverseFeaturedDataverse.class);
        super.em = em;
    }

    // -------------------- LOGIC --------------------

    public List<Dataverse> findByDataverseIdOrderByDisplayOrder(Long dataverseId) {
        return findByDataverseId(dataverseId, "order by d.displayOrder");
    }

    public List<Dataverse> findByDataverseIdOrderByNameAsc(Long dataverseId) {
        return findByDataverseId(dataverseId, "order by d.featuredDataverse.name asc");
    }

    public List<Dataverse> findByDataverseIdOrderByNameDesc(Long dataverseId) {
        return findByDataverseId(dataverseId, "order by d.featuredDataverse.name desc");
    }

    public List<Dataverse> findByDataversesBySorting(List<Dataverse.FeaturedDataversesSorting> sorting) {
        return em.createQuery("select distinct d.dataverse from DataverseFeaturedDataverse d " +
                        "where d.dataverse.featuredDataversesSorting in :sorting", Dataverse.class)
                .setParameter("sorting", sorting)
                .getResultList();
    }

    // -------------------- PRIVATE --------------------

    private List<Dataverse> findByDataverseId(Long dataverseId, String orderByClause) {
        String query = "select d.featuredDataverse from DataverseFeaturedDataverse d where d.dataverse.id = :dataverseId " + orderByClause;
        return em.createQuery(query, Dataverse.class)
                .setParameter("dataverseId", dataverseId)
                .getResultList();
    }
}
