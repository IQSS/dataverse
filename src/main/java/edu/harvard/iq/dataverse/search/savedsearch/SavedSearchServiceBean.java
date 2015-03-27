package edu.harvard.iq.dataverse.search.savedsearch;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class SavedSearchServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public SavedSearch find(long id) {
        TypedQuery<SavedSearch> typedQuery = em.createQuery("SELECT OBJECT(o) FROM SavedSearch AS o WHERE o.id = :id", SavedSearch.class);
        typedQuery.setParameter("id", id);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<SavedSearch> findAll() {
        TypedQuery<SavedSearch> typedQuery = em.createQuery("SELECT OBJECT(o) FROM SavedSearch AS o", SavedSearch.class);
        return typedQuery.getResultList();
    }

    public SavedSearch add(SavedSearch toPersist) {
        SavedSearch persisted = null;
        try {
            persisted = em.merge(toPersist);
        } catch (Exception ex) {
            System.out.println("exeption: " + ex);
        }
        return persisted;
    }

    public boolean delete(long id) {
        SavedSearch doomed = find(id);
        boolean wasDeleted = false;
        if (doomed != null) {
            System.out.println("deleting saved search id " + doomed.getId());
            em.remove(doomed);
            em.flush();
            wasDeleted = true;
        } else {
            System.out.println("problem deleting saved search id " + id);
        }
        return wasDeleted;
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
