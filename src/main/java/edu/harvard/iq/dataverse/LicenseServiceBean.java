package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jing Ma
 */
@Stateless
@Named
public class LicenseServiceBean {
    private static final Logger logger = Logger.getLogger(LicenseServiceBean.class.getName());

    @PersistenceContext
    EntityManager em;

    @EJB
    ActionLogServiceBean actionLogSvc;

    public List<License> listAll() {
        return em.createNamedQuery("License.findAll", License.class).getResultList();
    }

    public List<License> listAllActive() {
        return em.createNamedQuery("License.findAllActive", License.class).getResultList();
    }

    public License getById(Long id) throws NoResultException  {
        return em.createNamedQuery("License.findById", License.class)
                    .setParameter("id", id)
                    .getSingleResult();
    }

    public License getDefault() {
        return em.createNamedQuery("License.findDefault", License.class)
                .getSingleResult();
    }

    public License getByNameOrUri(String nameOrUri) throws NoResultException {
        return em.createNamedQuery("License.findActiveByNameOrUri", License.class)
                    .setParameter("name", nameOrUri)
                    .setParameter("uri", nameOrUri)
                    .getSingleResult();
    }

    public void setDefault(Long id) throws NoResultException {
        License candidate = getById(id);
        if (candidate.isActive()) {
                em.createNamedQuery("License.clearDefault").executeUpdate();
                em.createNamedQuery("License.setDefault").setParameter("id", id).executeUpdate();
        } else {
            throw new IllegalArgumentException("Cannot set an inactive license as default");
        }
    }

    public License save(License license) {
        if (license.getId() != null) {
            throw new IllegalArgumentException("There shouldn't be an ID in the request body");
        }
        try {
            em.persist(license);
            em.flush();
        }
        catch (PersistenceException p) {
            if (p.getMessage().contains("duplicate key")) {
                throw new IllegalStateException("A license with the same URI or name is already present.", p);
            }
            else {
                throw p;
            }
        }
        return license;
    }

    public int deleteById(long id) {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
                            .setInfo(Long.toString(id)));
        try {
            return em.createNamedQuery("License.deleteById").setParameter("id", id).executeUpdate();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("violates foreign key constraint")) {
                throw new IllegalStateException("License with id " + id + " is referenced and cannot be deleted.", p);
            } else {
                throw p;
            }
        }
    }
}
