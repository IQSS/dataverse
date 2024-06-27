package edu.harvard.iq.dataverse.persistence.dataverse;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class DataverseFeaturedDataverseRepository extends JpaRepository<Long, DataverseFeaturedDataverse> {
    private static final Logger logger = Logger.getLogger(DataverseFeaturedDataverseRepository.class.getCanonicalName());

    // -------------------- CONSTRUCTORS --------------------

    public DataverseFeaturedDataverseRepository() {
        super(DataverseFeaturedDataverse.class);
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
