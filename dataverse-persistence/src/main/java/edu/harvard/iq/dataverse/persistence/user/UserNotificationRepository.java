package edu.harvard.iq.dataverse.persistence.user;


import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.JpaRepository;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author xyang
 */
@Stateless
public class UserNotificationRepository extends JpaRepository<Long, UserNotification> {
    private final static int DELETE_BATCH_SIZE = 100;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- CONSTRUCTORS --------------------

    public UserNotificationRepository() {
        super(UserNotification.class);
    }

    // -------------------- LOGIC --------------------

    public UserNotificationQueryResult query(UserNotificationQuery queryParam) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<UserNotification> query = criteriaBuilder.createQuery(UserNotification.class);
        Root<UserNotification> root = query.from(UserNotification.class);

        List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.isNotBlank(queryParam.getSearchLabel())) {
            predicates.add(criteriaBuilder.like(root.get("searchLabel"), "%" + queryParam.getSearchLabel().toLowerCase() + "%"));
        }

        if (queryParam.getUserId() != null) {
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), queryParam.getUserId()));
        }

        query.select(root)
                .where(predicates.toArray(new Predicate[]{}))
                .orderBy(queryParam.isAscending() ? criteriaBuilder.asc(root.get("sendDate")) : criteriaBuilder.desc(root.get("sendDate")));

        List<UserNotification> resultList = em.createQuery(query)
                .setFirstResult(queryParam.getOffset())
                .setMaxResults(queryParam.getResultLimit())
                .getResultList();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        countQuery.select(criteriaBuilder.count(root))
                .where(predicates.toArray(new Predicate[]{}));

        Long totalCount = em.createQuery(countQuery).getSingleResult();

        return new UserNotificationQueryResult(resultList, totalCount);
    }

    public List<UserNotification> findByUser(Long userId) {
        return em.createQuery("select un from UserNotification un where un.user.id =:userId order by un.sendDate desc", UserNotification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public int updateRequestor(Long oldId, Long newId) {
        if (oldId == null || newId == null) {
            throw new IllegalArgumentException("Null encountered: [oldId]:" + oldId + ", [newId]:" + newId);
        }
        return em.createNativeQuery(String.format("update usernotification " +
                "set parameters = jsonb_set(parameters::jsonb, '{requestorId}', '\"%s\"')::json " +
                "where parameters ->> 'requestorId' = '%s'", newId.toString(), oldId.toString()))
                .executeUpdate();
    }

    public int deleteByIds(Set<Long> ids) {
        return Lists.partition(Lists.newArrayList(ids), DELETE_BATCH_SIZE).stream()
                .mapToInt(idBatch ->
                        em.createQuery("delete from UserNotification where id in :ids", UserNotification.class)
                                .setParameter("ids", idBatch)
                                .executeUpdate())
                .sum();
    }

    public int deleteByUser(Long userId) {
        return em.createQuery("delete from UserNotification where user.id = :userId", UserNotification.class)
                .setParameter("userId", userId)
                .executeUpdate();
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

    public UserNotification findLastSubmitNotificationByObjectId(long datasetId) {
        List<UserNotification> notifications = em.createQuery("SELECT un FROM UserNotification un " +
                "WHERE un.objectId = :objectId AND un.type = :type " +
                "ORDER BY un.sendDate DESC", UserNotification.class)
                .setParameter("objectId", datasetId)
                .setParameter("type", NotificationType.SUBMITTEDDS)
                .getResultList();
        return notifications.isEmpty() ? null : notifications.get(0);
    }

}
