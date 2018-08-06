package edu.harvard.iq.dataverse.locality;

import edu.harvard.iq.dataverse.EntityManagerBean;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

@Stateless
public class StorageSiteServiceBean {
    
    @Inject
    EntityManagerBean emBean;

    private static final Logger logger = Logger.getLogger(StorageSiteServiceBean.class.getCanonicalName());

    public StorageSite find(long id) {
        TypedQuery<StorageSite> typedQuery = emBean.getMasterEM().createQuery("SELECT OBJECT(o) FROM StorageSite AS o WHERE o.id = :id", StorageSite.class);
        typedQuery.setParameter("id", id);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<StorageSite> findAll() {
        // TODO: order by primary first, then whatever
        TypedQuery<StorageSite> typedQuery = emBean.getMasterEM().createQuery("SELECT OBJECT(o) FROM StorageSite AS o ORDER BY o.id", StorageSite.class);
        return typedQuery.getResultList();
    }

    public StorageSite add(StorageSite toPersist) {
        StorageSite persisted = null;
        try {
            persisted = emBean.getMasterEM().merge(toPersist);
        } catch (Exception ex) {
            logger.info("Exception caught during add: " + ex);
        }
        return persisted;
    }

    public boolean delete(long id) {
        StorageSite doomed = find(id);
        boolean wasDeleted = false;
        if (doomed != null) {
            logger.fine("deleting id " + doomed.getId());
            emBean.getMasterEM().remove(doomed);
            emBean.getMasterEM().flush();
            wasDeleted = true;
        } else {
            logger.warning("problem deleting id " + id);
        }
        return wasDeleted;
    }

    public StorageSite save(StorageSite storageSite) {
        return emBean.getMasterEM().merge(storageSite);
    }

}
