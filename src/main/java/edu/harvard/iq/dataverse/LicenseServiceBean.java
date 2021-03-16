package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.settings.Setting;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

/**
 * @author Jing Ma
 */
//@Stateless
//@Named
public class LicenseServiceBean {
    
//    @PersistenceContext
//    EntityManager em;
//    
//    @EJB
//    ActionLogServiceBean actionLogSvc;
//    
//    public List<License> listAll() {
//        return em.createNamedQuery("License.findAll", License.class).getResultList();
//    }
//    
//    public License get( long id ) {
//        List<License> tokens = em.createNamedQuery("License.findById", License.class)
//                .setParameter("id", id )
//                .getResultList();
//        return tokens.isEmpty() ? null : tokens.get(0);
//    }
//    
//    public License save(License l) throws PersistenceException {
//        if (l.getId() == null) {
//            em.persist(l);
//            return l;
//        } else {
//            return null;
//        }
//    }
//
//    public License set( long id, String name, String shortDescription, String uri, String iconUrl, boolean active ) {
//        List<License> tokens = em.createNamedQuery("License.findById", License.class)
//                .setParameter("id", Long.toString(id) )
//                .getResultList();
//        
//        if(tokens.size() > 0) {
//            License l = tokens.get(0);
//            l.setName(name);
//            l.setShortDescription(shortDescription);
//            l.setUri(uri);
//            l.setIconUrl(iconUrl);
//            l.setActive(active);        
//            em.merge(l);
//            actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "set")
//                .setInfo(name + ": " + shortDescription + ": " + uri + ": " + iconUrl + ": " + active));
//            return l;
//        } else {
//            return null;
//        }
//    }
//    
//    public void delete( long id ) throws PersistenceException {
//        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Admin, "delete")
//                            .setInfo(Long.toString(id)));
//        em.createNamedQuery("License.deleteById")
//                .setParameter("id", id)
//                .executeUpdate();
//    }

}
