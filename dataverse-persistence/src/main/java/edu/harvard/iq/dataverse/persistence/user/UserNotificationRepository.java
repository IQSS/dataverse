package edu.harvard.iq.dataverse.persistence.user;


import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * @author xyang
 */
@Stateless
public class UserNotificationRepository extends JpaRepository<Long, UserNotification> {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- CONSTRUCTORS --------------------

    public UserNotificationRepository() {
        super(UserNotification.class);
    }

    // -------------------- LOGIC --------------------

    public List<UserNotification> findByUser(Long userId) {
        return em.createQuery("select un from UserNotification un where un.user.id =:userId order by un.sendDate desc", UserNotification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<UserNotification> findByRequestor(Long userId) {
        return em.createQuery("select un from UserNotification un where un.requestor.id =:userId order by un.sendDate desc", UserNotification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public Long getUnreadNotificationCountByUser(Long userId) {
        return em.createQuery("select count(un) from UserNotification as un where un.user.id = :userId and un.readNotification = :readNotification", Long.class)
                .setParameter("userId", userId)
                .setParameter("readNotification", false)
                .getSingleResult();
    }

    public int updateEmailSent(long userNotificationId) {
        return em.createQuery("UPDATE UserNotification notification SET notification.emailed = :emailSent" +
                                      " WHERE notification.id = :userNotificationId")
                .setParameter("emailSent", true)
                .setParameter("userNotificationId", userNotificationId)
                .executeUpdate();
    }

}
