package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
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

    public License getById(Long id) {
        try {
            return em.createNamedQuery("License.findById", License.class)
                    .setParameter("id", id)
                    .getSingleResult();
        }catch (NoResultException noResultException) {
            logger.log(Level.WARNING, "License with ID {0} doesn't exist.", id);
            return null;
        }
    }

    public License getDefault() {
        return em.createNamedQuery("License.findDefault", License.class)
                .getSingleResult();
    }

    public License getByNameOrUri(String nameOrUri) {
        try {
            return em.createNamedQuery("License.findActiveByNameOrUri", License.class)
                    .setParameter("name", nameOrUri)
                    .setParameter("uri", nameOrUri)
                    .getSingleResult();
        } catch (NoResultException noResultException) {
            logger.log(Level.WARNING, "Couldn't find a license for: " + nameOrUri);
            return null;
        }
    }

    public int setDefault(Long id) throws WrappedResponse{
        License candidate = getById(id);
        if (candidate == null) return 0;
        if (candidate.isActive()) {
                em.createNamedQuery("License.clearDefault").executeUpdate();
               return em.createNamedQuery("License.setDefault").setParameter("id", id).executeUpdate();
        } else {
            throw new WrappedResponse(new IllegalArgumentException("Cannot set an inactive license as default"), null);
        }
    }

    public int setActive(Long id, boolean state) throws WrappedResponse {
        License candidate = getById(id);
        if (candidate == null)
            return 0;
        
        if (candidate.isActive() != state) {
            if(candidate.isDefault() && state==false) {
                throw new WrappedResponse(
                        new IllegalArgumentException("Cannot inactivate the default license"), null);
            }
            return em.createNamedQuery("License.setActiveState").setParameter("id", id).setParameter("state", state)
                    .executeUpdate();
        } else {
            throw new WrappedResponse(
                    new IllegalArgumentException("License already " + (state ? "active" : "inactive")), null);
        }
    }
    
    public License save(License license) throws WrappedResponse {
        if (license.getId() != null) {
            throw new WrappedResponse(new IllegalArgumentException("There shouldn't be an ID in the request body"), null);
        }
        try {
            em.persist(license);
            em.flush();
        }
        catch (PersistenceException p) {
            if (p.getMessage().contains("duplicate key")) {
                throw new WrappedResponse(new IllegalStateException("A license with the same URI or name is already present.", p), null);
            }
            else {
                throw p;
            }
        }
        return license;
    }

    public int deleteById(long id) throws WrappedResponse {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
                            .setInfo(Long.toString(id)));
        try {
            return em.createNamedQuery("License.deleteById").setParameter("id", id).executeUpdate();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("violates foreign key constraint")) {
                throw new WrappedResponse(new IllegalStateException("License with id " + id + " is referenced and cannot be deleted.", p), null);
            } else {
                throw p;
            }
        }
    }
}
