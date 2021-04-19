package edu.harvard.iq.dataverse.persistence.dataverse;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

import java.util.List;

@Stateless
public class DataverseFeaturedDataverseRepository extends JpaRepository<Long, DataverseFeaturedDataverse> {

    // -------------------- CONSTRUCTORS --------------------

    public DataverseFeaturedDataverseRepository() {
        super(DataverseFeaturedDataverse.class);
    }

    // -------------------- LOGIC --------------------

    public List<DataverseFeaturedDataverse> findByDataverseId(Long dataverseId) {
        String query = "select object(o) from DataverseFeaturedDataverse as o where o.dataverse.id = :dataverseId order by o.displayOrder";
        return em.createQuery(query, DataverseFeaturedDataverse.class)
                .setParameter("dataverseId", dataverseId)
                .getResultList();
    }

}
