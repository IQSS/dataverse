package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;

import java.util.Optional;

@Singleton
public class MailDomainGroupRepository extends JpaRepository<Long, MailDomainGroup> {

    // -------------------- CONSTRUCTORS --------------------

    public MailDomainGroupRepository() {
        super(MailDomainGroup.class);
    }
    
    public MailDomainGroupRepository(final EntityManager em) {
        
        super(MailDomainGroup.class);
        super.em = em;
    }

    // -------------------- LOGIC --------------------

    public Optional<MailDomainGroup> findByAlias(String alias) {
        return em.createQuery("SELECT m FROM MailDomainGroup m WHERE m.persistedGroupAlias = :alias", MailDomainGroup.class)
                .setParameter("alias", alias)
                .getResultList().stream()
                .findFirst();
    }
}
