package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.FetchException;
import edu.harvard.iq.dataverse.api.RequestBodyException;
import edu.harvard.iq.dataverse.api.UpdateException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

/**
 * @author Jing Ma
 */
@Stateless
@Named
public class LicenseServiceBean {
    
    @PersistenceContext
    EntityManager em;

    @EJB
    ActionLogServiceBean actionLogSvc;

    public List<License> listAll() {
        return em.createNamedQuery("License.findAll", License.class).getResultList();
    }

    public License getById(long id) throws FetchException {
        List<License> licenses = em.createNamedQuery("License.findById", License.class)
                .setParameter("id", id )
                .getResultList();
        if (licenses.isEmpty()) {
            throw new FetchException("License with that ID doesn't exist.");
        }
        return licenses.get(0);
    }

    public License getByName(String name) throws FetchException {
        List<License> licenses = em.createNamedQuery("License.findByName", License.class)
                .setParameter("name", name )
                .getResultList();
        if (licenses.isEmpty()) {
            throw new FetchException("License with that name doesn't exist.");
        }
        return licenses.get(0);
    }

    public void save(License license) throws PersistenceException, RequestBodyException {
        if (license.getId() == null) {
            em.persist(license);
        } else {
            throw new RequestBodyException("There shouldn't be an ID in the request body");
        }
    }

    public void setById(long id, String name, String shortDescription, URI uri, URI iconUrl, boolean active) throws UpdateException {
        List<License> licenses = em.createNamedQuery("License.findById", License.class)
                .setParameter("id", id )
                .getResultList();

        if(licenses.size() > 0) {
            License license = licenses.get(0);
            license.setName(name);
            license.setShortDescription(shortDescription);
            license.setUri(uri);
            license.setIconUrl(iconUrl);
            license.setActive(active);        
            em.merge(license);
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "set")
                .setInfo(name + ": " + shortDescription + ": " + uri + ": " + iconUrl + ": " + active));
        } else {
            throw new UpdateException("There is no existing License with that ID. To add a license use POST.");
        }
    }

    public void setByName(String name, String shortDescription, URI uri, URI iconUrl, boolean active) throws UpdateException {
        List<License> licenses = em.createNamedQuery("License.findByName", License.class)
                .setParameter("name", name )
                .getResultList();

        if(licenses.size() > 0) {
            License license = licenses.get(0);
            license.setShortDescription(shortDescription);
            license.setUri(uri);
            license.setIconUrl(iconUrl);
            license.setActive(active);        
            em.merge(license);
            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "set")
                .setInfo(name + ": " + shortDescription + ": " + uri + ": " + iconUrl + ": " + active));
        } else {
            throw new UpdateException("There is no existing License with that name. To add a license use POST.");
        }
    }

    public int deleteById(long id) throws PersistenceException {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
                            .setInfo(Long.toString(id)));
        return em.createNamedQuery("License.deleteById")
                .setParameter("id", id)
                .executeUpdate();
    }

    public int deleteByName(String name) throws PersistenceException {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
                            .setInfo(name));
        return em.createNamedQuery("License.deleteByName")
                .setParameter("name", name)
                .executeUpdate();
    }

}
