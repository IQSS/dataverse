package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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

    public List<Consent> findConsents() {

        return em.createQuery("SELECT DISTINCT cons FROM Consent cons JOIN FETCH cons.consentDetails",
                              Consent.class)
                .getResultList();
    }

    public Option<Consent> findConsent(String alias) {

        TypedQuery<Consent> query = em.createQuery(
                "SELECT DISTINCT cons FROM Consent cons JOIN FETCH cons.consentDetails" +
                        " WHERE cons.name = :consentName",
                Consent.class)
                .setParameter("consentName", alias);

        return Try.of(() -> Option.of(query.getSingleResult()))
                .getOrElse(Option::none);
    }

    public Consent mergeConsent(Consent consent){
        return em.merge(consent);
    }

    public void saveAcceptedConsent(AcceptedConsent acceptedConsent){
        em.persist(acceptedConsent);
    }

}
