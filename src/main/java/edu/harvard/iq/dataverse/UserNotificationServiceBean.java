/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import java.sql.Timestamp;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class UserNotificationServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<UserNotification> findByUser(Long userId) {
        Query query = em.createQuery("select object(o) from UserNotification as o where o.user.id =:userId order by o.sendDate desc");
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    public List<UserNotification> findUnreadByUser(Long userId) {
        Query query = em.createQuery("select object(o) from UserNotification as o where o.user.id =:userId and o.readNotification = 'false' order by o.sendDate desc");
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    public List<UserNotification> findUnemailed() {
        Query query = em.createQuery("select object(o) from UserNotification as o where o.readNotification = 'false' and o.emailed = 'false'");
        return query.getResultList();
    }
    
    public UserNotification find(Object pk) {
        return (UserNotification) em.find(UserNotification.class, pk);
    }

    public UserNotification save(UserNotification userNotification) {
        return em.merge(userNotification);
    }
    
    public void delete(UserNotification userNotification) {
        em.remove(em.merge(userNotification));
    }
    
    public void sendNotification(DataverseUser dataverseUser, Timestamp sendDate, Type type, Long objectId) {
        
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(objectId);
        save(userNotification);
    }
}
