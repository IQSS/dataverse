package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class SamlIdentityProviderRepository extends JpaRepository<Long, SamlIdentityProvider> {

    // -------------------- CONSTRUCTORS --------------------

    public SamlIdentityProviderRepository() {
        super(SamlIdentityProvider.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<SamlIdentityProvider> findByEntityId(String entityId) {
        List<SamlIdentityProvider> entityIdList =
                em.createNamedQuery("SamlIdentityProvider.findByEntityId", SamlIdentityProvider.class)
                        .setParameter("entityId", entityId)
                        .getResultList();
        return entityIdList.isEmpty()
                ? Optional.empty()
                : Optional.of(entityIdList.get(0));
    }
}
