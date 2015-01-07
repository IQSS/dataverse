package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;

/**
 * Your goto bean for everything {@link DvObject}, that's not tied to any
 * concrete subclass.
 *
 * @author michael
 */
@Stateless
@Named
public class DvObjectServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    /**
     * @param dvoc The object we check
     * @return {@code true} iff the passed object is the owner of any
     * {@link DvObject}.
     */
    public boolean hasData(DvObjectContainer dvoc) {
        return em.createNamedQuery("DvObject.ownedObjectsById", Long.class)
                .setParameter("id", dvoc.getId())
                .getSingleResult() > 0;
    }

    public DvObject findDvObject(Long id) {
        try {
            return em.createNamedQuery("DvObject.findById", DvObject.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<DvObject> findAll() {
        return em.createNamedQuery("DvObject.findAll", DvObject.class).getResultList();
    }

    /**
     * @todo Rename this to updateContentIndexTime (or something) to
     * differentiate it from permissions. Content Solr docs vs. permission Solr
     * docs.
     */
    public DvObject updateIndexTime(DvObject dvObject) {
        /**
         * @todo to avoid a possible OptimisticLockException, should we merge
         * dvObject before we try to setIndexTime? See
         * https://github.com/IQSS/dataverse/commit/6ad0ebb272c8cb46368cb76784b55dbf33eea947
         */
        dvObject.setIndexTime(new Timestamp(new Date().getTime()));
        DvObject savedDvObject = em.merge(dvObject);
        return savedDvObject;
    }

    /**
     * @todo DRY! We could probably merge this with the older updateIndexTime
     * method (which should be renamed)
     */
    public DvObject updatePermissionIndexTime(DvObject dvObject) {
        /**
         * @todo to avoid a possible OptimisticLockException, should we merge
         * dvObject before we try to set this timestamp? See
         * https://github.com/IQSS/dataverse/commit/6ad0ebb272c8cb46368cb76784b55dbf33eea947
         */
        dvObject.setPermissionIndexTime(new Timestamp(new Date().getTime()));
        DvObject savedDvObject = em.merge(dvObject);
        return savedDvObject;
    }

}
