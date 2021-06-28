package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.ConflictException;
import edu.harvard.iq.dataverse.api.FetchException;
import edu.harvard.iq.dataverse.api.RequestBodyException;
import edu.harvard.iq.dataverse.api.UpdateException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.net.URI;
import java.util.List;
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

    public License getById(Long id) throws FetchException {
        List<License> licenses = em.createNamedQuery("License.findById", License.class)
                .setParameter("id", id )
                .getResultList();
        if (licenses.isEmpty()) {
            throw new FetchException("License with that ID doesn't exist.");
        }
        return licenses.get(0);
    }

    public License getDefault() {
        List<License> licenses = em.createNamedQuery("License.findDefault", License.class)
                .getResultList();
        return licenses.get(0);
    }

    public void setDefault(Long id) throws UpdateException, FetchException {
        License candidate = getById(id);
        if (candidate.isActive()) {
            try {
                em.createNamedQuery("License.clearDefault").executeUpdate();
                em.createNamedQuery("License.setDefault").setParameter("id", id).executeUpdate();
            }
            catch (PersistenceException e) {
                throw new UpdateException("Inactive license cannot be default.");
            }
        }
        else
            throw new IllegalArgumentException("Cannot set an inactive license as default");
    }

    public License save(License license) throws RequestBodyException, ConflictException {
        if (license.getId() != null) {
            throw new RequestBodyException("There shouldn't be an ID in the request body");
        }
        try {
            em.persist(license);
            em.flush();
        }
        catch (PersistenceException p) {
            if (p.getMessage().contains("duplicate key")) {
                throw new ConflictException("A license with the same URI or name is already present.");
            }
            else {
                throw p;
            }
        }
        return license;
    }

    public void setById(long id, String name, URI uri, URI iconUrl, boolean active) throws UpdateException {
        List<License> licenses = em.createNamedQuery("License.findById", License.class)
                .setParameter("id", id )
                .getResultList();

        if(licenses.size() > 0) {
            License license = licenses.get(0);
            license.setName(name);
            license.setUri(uri);
            license.setIconUrl(iconUrl);
            license.setActive(active);        
            em.merge(license);
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "set")
                .setInfo(name + ": " + uri + ": " + iconUrl + ": " + active));
        } else {
            throw new UpdateException("There is no existing License with that ID. To add a license use POST.");
        }
    }

    public int deleteById(long id) throws ConflictException {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
                            .setInfo(Long.toString(id)));
        try {
            return em.createNamedQuery("License.deleteById").setParameter("id", id).executeUpdate();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("violates foreign key constraint")) {
                throw new ConflictException("License with id " + id + " is referenced and cannot be deleted.");
            } else {
                throw p;
            }
        }
    }
}
