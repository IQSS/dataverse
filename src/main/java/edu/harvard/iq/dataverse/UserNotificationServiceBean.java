/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

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

    @EJB
    SettingsServiceBean settingsService;
    
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

    public UserNotification markAsRead(UserNotification userNotification) {
        userNotification.setReadNotification(true);
        return em.merge(userNotification);
    }

    public void delete(UserNotification userNotification) {
        em.remove(em.merge(userNotification));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendNotificationInNewTransaction(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId) {
        sendNotification(dataverseUser, sendDate, type, objectId, "");
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
        sendNotification(dataverseUser, sendDate, type, objectId, comment, requestor, isHtmlContent, null);
    }
    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, Type type, Long objectId, String comment, AuthenticatedUser requestor, boolean isHtmlContent, String additionalInfo) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(objectId);
        userNotification.setRequestor(requestor);
        userNotification.setAdditionalInfo(additionalInfo);

        if (!isEmailMuted(userNotification) && mailService.sendNotificationEmail(userNotification, comment, requestor, isHtmlContent)) {
            logger.fine("email was sent");
            userNotification.setEmailed(true);
        } else {
            logger.fine("email was not sent");
        }
        if (!isNotificationMuted(userNotification)) {
            save(userNotification);
        }
    }
    

    public boolean isEmailMuted(UserNotification userNotification) {
        final Type type = userNotification.getType();
        final AuthenticatedUser user = userNotification.getUser();
        final boolean alwaysMuted = settingsService.containsCommaSeparatedValueForKey(Key.AlwaysMuted, type.name());
        final boolean neverMuted = settingsService.containsCommaSeparatedValueForKey(Key.NeverMuted, type.name());
        if (alwaysMuted && neverMuted) {
            logger.warning("Both; AlwaysMuted and NeverMuted are set for " + type.name() + ", email is muted");
        }
        return alwaysMuted || (!neverMuted && user.hasEmailMuted(type));
    }
    
    public boolean isNotificationMuted(UserNotification userNotification) {
        final Type type = userNotification.getType();
        final AuthenticatedUser user = userNotification.getUser();
        final boolean alwaysMuted = settingsService.containsCommaSeparatedValueForKey(Key.AlwaysMuted, type.name());
        final boolean neverMuted = settingsService.containsCommaSeparatedValueForKey(Key.NeverMuted, type.name());
        if (alwaysMuted && neverMuted) {
            logger.warning("Both; AlwaysMuted and NeverMuted are set for " + type.name() + ", notification is muted");
        }
        return alwaysMuted || (!neverMuted && user.hasNotificationMuted(type));
    }
}
