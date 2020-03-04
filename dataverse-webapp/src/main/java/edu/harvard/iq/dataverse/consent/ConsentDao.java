package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.consent.Consent;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ConsentDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<Consent> findNotHiddenConsents() {

        return em.createQuery("SELECT DISTINCT cons FROM Consent cons JOIN cons.consentDetails details" +
                                      " WHERE cons.hidden = false",
                              Consent.class)
                .getResultList();
    }

    public void saveAcceptedConsent(AcceptedConsent acceptedConsent){
        em.persist(acceptedConsent);
    }

}
