/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

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
    
    public UserNotification find(Object pk) {
        return (UserNotification) em.find(UserNotification.class, pk);
    }

    public UserNotification save(UserNotification userNotification) {
        return em.merge(userNotification);
    }
    
    public void delete(UserNotification userNotification) {
        em.remove(em.merge(userNotification));
    }
    
    public void sendNotification(String notification, DataverseUser dataverseUser, Timestamp sendDate) {
        UserNotification userNotification = new UserNotification();
        userNotification.setNotification(notification);
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        save(userNotification);
    }
}
