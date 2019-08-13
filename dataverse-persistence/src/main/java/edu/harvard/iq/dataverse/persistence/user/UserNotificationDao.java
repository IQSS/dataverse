/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.persistence.user;


import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author xyang
 */
@Stateless
public class UserNotificationDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<UserNotification> findByUser(Long userId) {
        TypedQuery<UserNotification> query = em.createQuery("select un from UserNotification un where un.user.id =:userId order by un.sendDate desc", UserNotification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    public Long getUnreadNotificationCountByUser(Long userId) {
        if (userId == null) {
            return new Long("0");
        }
        Query query = em.createNativeQuery("select count(id) from usernotification as o where o.user_id = " + userId + " and o.readnotification = 'false';");
        return (Long) query.getSingleResult();
    }

    public int updateEmailSent(long userNotificationId) {
        return em.createQuery("UPDATE UserNotification notification SET notification.emailed = :emailSent" +
                                      " WHERE notification.id = :userNotificationId")
                .setParameter("emailSent", true)
                .setParameter("userNotificationId", userNotificationId)
                .executeUpdate();
    }

    public UserNotification find(Object pk) {
        return em.find(UserNotification.class, pk);
    }

    public void flush() {
        em.flush();
    }

    public void save(UserNotification userNotification) {
        em.persist(userNotification);
    }

    public UserNotification merge(UserNotification userNotification) {
        return em.merge(userNotification);
    }

    public void delete(UserNotification userNotification) {
        em.remove(em.merge(userNotification));
    }

}
