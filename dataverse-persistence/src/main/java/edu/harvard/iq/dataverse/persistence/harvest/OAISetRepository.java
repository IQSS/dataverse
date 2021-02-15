package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

import java.util.List;
import java.util.Optional;

@Stateless
public class OAISetRepository extends JpaRepository<Long, OAISet> {

    // -------------------- CONSTRUCTORS --------------------

    public OAISetRepository() {
        super(OAISet.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<OAISet> findBySpecName(String specName) {
        return JpaRepository.getSingleResult(
                em.createQuery("SELECT o FROM OAISet o WHERE o.spec = :specName", OAISet.class)
                    .setParameter("specName", specName));
    }

    public List<OAISet> findAllBySpecNameNot(String specName) {
        return em.createQuery("SELECT o FROM OAISet o WHERE o.spec != :specName ORDER BY o.spec", OAISet.class)
                    .setParameter("specName", specName)
                    .getResultList();
    }
}
