package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class DataverseRoleRepository extends JpaRepository<Long, DataverseRole> {

    // -------------------- CONSTRUCTORS --------------------

    public DataverseRoleRepository() {
        super(DataverseRole.class);
    }

    // -------------------- LOGIC --------------------

    public List<DataverseRole> findByOwnerId(Long ownerId) {
        return em.createQuery("SELECT r FROM DataverseRole r WHERE r.owner.id=:ownerId ORDER BY r.name", DataverseRole.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }
    
    public List<DataverseRole> findWithoutOwner() {
        return em.createQuery("SELECT r FROM DataverseRole r WHERE r.owner is null ORDER BY r.name", DataverseRole.class)
                .getResultList();
    }

    public Optional<DataverseRole> findByAlias(String alias) {
        return getSingleResult(
                em.createQuery("SELECT r FROM DataverseRole r WHERE r.alias=:alias", DataverseRole.class)
                    .setParameter("alias", alias));
    }
}
