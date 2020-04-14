/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class UserNotificationServiceBean {

    private static final Logger logger = Logger.getLogger(UserNotificationServiceBean.class.getCanonicalName());

    @EJB
    MailServiceBean mailService;
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<UserNotification> findByUser(Long userId) {
        TypedQuery<UserNotification> query = em.createQuery("select un from UserNotification un where un.user.id =:userId order by un.sendDate desc", UserNotification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    public List<UserNotification> findByRequestor(Long userId) {
        TypedQuery<UserNotification> query = em.createQuery("select un from UserNotification un where un.requestor.id =:userId order by un.sendDate desc", UserNotification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    public List<UserNotification> findByDvObject(Long dvObjId) {
        TypedQuery<UserNotification> query = em.createQuery("select object(o) from UserNotification as o where o.objectId =:dvObjId order by o.sendDate desc", UserNotification.class);
        query.setParameter("dvObjId", dvObjId);
        return query.getResultList();
    }
    
    public List<UserNotification> findUnreadByUser(Long userId) {
        TypedQuery<UserNotification> query = em.createQuery("select object(o) from UserNotification as o where o.user.id =:userId and o.readNotification = 'false' order by o.sendDate desc", UserNotification.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    
    public Long getUnreadNotificationCountByUser(Long userId){
        if (userId == null){
            return new Long("0");
        }
        Query query = em.createNativeQuery("select count(id) from usernotification as o where o.user_id = " + userId + " and o.readnotification = 'false';");
        return (Long) query.getSingleResult();    
    }
    
    public List<UserNotification> findUnemailed() {
        TypedQuery<UserNotification> query = em.createQuery("select object(o) from UserNotification as o where o.readNotification = 'false' and o.emailed = 'false'", UserNotification.class);
        return query.getResultList();
    }
    
    public UserNotification find(Object pk) {
        return em.find(UserNotification.class, pk);
    }

    public UserNotification save(UserNotification userNotification) {
        return em.merge(userNotification);
    }
    
    public void delete(UserNotification userNotification) {
        em.remove(em.merge(userNotification));
    }

    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId) {
        sendNotification(dataverseUser, sendDate, type, objectId, "");
    }

    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId, String comment) {
        sendNotification(dataverseUser, sendDate, type, objectId, comment, null, false);
    }

    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId, String comment, boolean isHtmlContent) {
        sendNotification(dataverseUser, sendDate, type, objectId, comment, null, isHtmlContent);
    }

    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId, String comment, AuthenticatedUser requestor, boolean isHtmlContent) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(objectId);
        userNotification.setRequestor(requestor);

        if (mailService.sendNotificationEmail(userNotification, comment, requestor, isHtmlContent)) {
            logger.fine("email was sent");
            userNotification.setEmailed(true);
            save(userNotification);
        } else {
            logger.fine("email was not sent");
            save(userNotification);
        }
    }
    
    /**
     * Returns a UserNotification that was sent to a dataverseUser.
     * Sends ONLY the UserNotification (no email is sent via this method).
     * All parameters are assumed to be valid, non-null objects.
     * 
     * @param dataverseUser - the AuthenticatedUser to whom the notification is to be sent
     * @param sendDate - the time and date the notification was sent.
     * @param type - the type of notification to be sent (see UserNotification for the different types)
     * @param objectId -  the ID of the Dataverse object (Dataverse, Dataset, etc.) that the notification pertains to 
     * @return The UserNotification that was sent to the dataverseUser
     */
    
    public UserNotification sendUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(objectId);
        this.save(userNotification);
        return userNotification;
    }
}
