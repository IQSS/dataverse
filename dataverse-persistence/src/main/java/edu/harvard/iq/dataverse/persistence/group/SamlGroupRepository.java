package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class SamlGroupRepository extends JpaRepository<Long, SamlGroup> {

    // -------------------- CONSTRUCTORS --------------------

    public SamlGroupRepository() {
        super(SamlGroup.class);
    }

    // -------------------- LOGIC --------------------

    public List<SamlGroup> findByEntityId(String entityId) {
        TypedQuery<SamlGroup> query = em.createNamedQuery("SamlGroup.findByEntityId", SamlGroup.class)
                .setParameter("entityId", entityId);
        return query.getResultList();
    }
}
