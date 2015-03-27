package edu.harvard.iq.dataverse.search.savedsearch;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class SavedSearchServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<SavedSearch> findAll() {
        TypedQuery<SavedSearch> typedQuery = em.createQuery("SELECT OBJECT(o) FROM SavedSearch AS o", SavedSearch.class);
        return typedQuery.getResultList();
    }

    public SavedSearch save(SavedSearch savedSearch) {
        if (savedSearch.getId() == null) {
            em.persist(savedSearch);
            return savedSearch;
        } else {
            return em.merge(savedSearch);
        }
    }

}
