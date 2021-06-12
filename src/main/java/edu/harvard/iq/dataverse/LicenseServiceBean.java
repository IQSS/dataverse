package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.ConflictException;
import edu.harvard.iq.dataverse.api.FetchException;
import edu.harvard.iq.dataverse.api.RequestBodyException;
import edu.harvard.iq.dataverse.api.UpdateException;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;
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

    public License getByName(String name) throws FetchException {
        List<License> licenses = em.createNamedQuery("License.findByName", License.class)
                .setParameter("name", name )
                .getResultList();
        if (licenses.isEmpty()) {
            throw new FetchException("License with that name doesn't exist.");
        }
        return licenses.get(0);
    }

    public License getCC0() {
        List<License> licenses = em.createNamedQuery("License.findDefault", License.class)
                .getResultList();
        // TODO: Move this to flyway script
        if (licenses.isEmpty()) {
            String shortDescription = "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.";
            URI uri = URI.create("https://creativecommons.org/publicdomain/zero/1.0/");
            URI iconUrl = URI.create("https://www.researchgate.net/profile/Donat-Agosti/publication/51971424/figure/fig2/AS:203212943564807@1425461149299/Logo-of-the-CC-Zero-or-CC0-Public-Domain-Dedication-License-No-Rights-Reserved-CC.png");
            License license = new License("CC0", shortDescription, uri, iconUrl, true);
            em.persist(license);
            em.flush();
            return license;
        }
        return licenses.get(0);
    }

    public License save(License license) throws RequestBodyException, ConflictException {
        if (license.getId() != null) {
            throw new RequestBodyException("There shouldn't be an ID in the request body");
        }
        List<License> licenses = em.createNamedQuery("License.findByNameOrUri", License.class)
            .setParameter("name", license.getName() )
            .setParameter("uri", license.getUri().toASCIIString() )
            .getResultList();
        if (!licenses.isEmpty()) {
            throw new ConflictException("A license with the same URI or name is already present.");
        }
        em.persist(license);
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

    public int deleteById(long id) throws PersistenceException {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
                            .setInfo(Long.toString(id)));
        return em.createNamedQuery("License.deleteById")
                .setParameter("id", id)
                .executeUpdate();
    }
}
