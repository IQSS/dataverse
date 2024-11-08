package edu.harvard.iq.dataverse.persistence.dataverse;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;

import java.util.List;

@Singleton
public class DataverseRepository extends JpaRepository<Long, Dataverse> {

    // -------------------- CONSTRUCTORS --------------------

    public DataverseRepository() {
        super(Dataverse.class);
    }
    
    public DataverseRepository(final EntityManager em) {
        
        super(Dataverse.class);
        super.em = em;
    }

    // -------------------- LOGIC --------------------

    public List<Dataverse> findPublishedByOwnerId(Long ownerId) {
        String query = "select d from Dataverse d where d.owner.id =:ownerId and d.publicationDate is not null order by d.name";
        return em.createQuery(query, Dataverse.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }
}
