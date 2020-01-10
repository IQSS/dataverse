package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationMapper;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationDao;
import org.awaitility.Awaitility;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class responsible for managing user notifications.
 */
@Stateless
public class UserNotificationService {

    private UserNotificationDao userNotificationDao;
    private MailService mailService;
    private EmailNotificationMapper mailMapper;

    private ExecutorService executorService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated /* JEE requirement*/
    public UserNotificationService() {
    }

    @Inject
    public UserNotificationService(UserNotificationDao userNotificationDao, MailService mailService, EmailNotificationMapper mailMapper) {
        this.userNotificationDao = userNotificationDao;
        this.mailService = mailService;
        this.mailMapper = mailMapper;

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // -------------------- LOGIC --------------------

    /**
     * Saves notification to database, hereby sends notification to users dashboard.
     */
    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type) {
        UserNotification userNotification = createUserNotification(dataverseUser, sendDate, type);

        userNotificationDao.save(userNotification);
    }

    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          NotificationType type,
                                          long dvObjectId,
                                          NotificationObjectType notificationObjectType) {

        UserNotification userNotification = sendNotification(dataverseUser, sendDate, type, dvObjectId);

        userNotificationDao.flush();

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType));
    }

    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          NotificationType type,
                                          long dvObjectId,
                                          NotificationObjectType notificationObjectType,
                                          AuthenticatedUser requestor) {

        UserNotification userNotification = sendNotification(dataverseUser, sendDate, type, dvObjectId, requestor);

        userNotificationDao.flush();

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType, requestor));
    }

    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     * @param comment                - custom user message added to notification on '{@link NotificationType#RETURNEDDS}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          NotificationType type,
                                          long dvObjectId,
                                          NotificationObjectType notificationObjectType,
                                          String comment) {

        UserNotification userNotification = sendNotification(dataverseUser, sendDate, type, dvObjectId, comment);

        userNotificationDao.flush();

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType));
    }
    // -------------------- PRIVATE --------------------

    /**
     * Sends an email, Awaitility is necessary since we have to wait until previous transaction will be committed,
     * otherwise there is a chance that we won't be able to retrieve necessary data from database.
     */
    private boolean sendEmail(long emailNotificationid, NotificationObjectType notificationObjectType, AuthenticatedUser requester) {
        UserNotification notification = Awaitility.await()
                .with()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .until(() -> userNotificationDao.find(emailNotificationid), Objects::nonNull);


        EmailNotificationDto emailNotificationDto = mailMapper.toDto(notification,
                                                                     notification.getObjectId(),
                                                                     notificationObjectType,
                                                                     notification.getReturnToAuthorReason());

        Boolean emailSent = mailService.sendNotificationEmail(emailNotificationDto, requester);

        if (emailSent) {
            userNotificationDao.updateEmailSent(emailNotificationDto.getUserNotificationId());
        }

        return emailSent;
    }

    private boolean sendEmail(long emailNotificationid, NotificationObjectType notificationObjectType) {
        UserNotification notification = Awaitility.await()
                .with()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .until(() -> userNotificationDao.find(emailNotificationid), Objects::nonNull);

        EmailNotificationDto emailNotificationDto = mailMapper.toDto(notification,
                                                                     notification.getObjectId(),
                                                                     notificationObjectType,
                                                                     notification.getReturnToAuthorReason());


        Boolean emailSent = mailService.sendNotificationEmail(emailNotificationDto);

        if (emailSent) {
            userNotificationDao.updateEmailSent(emailNotificationDto.getUserNotificationId());
        }

        return emailSent;
    }

    private UserNotification sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type,
                                              long dvObjectId, AuthenticatedUser requestor) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(dvObjectId);
        userNotification.setRequestor(requestor);

        userNotificationDao.save(userNotification);

        return userNotification;
    }

    private UserNotification sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type, long dvObjectId) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(dvObjectId);

        userNotificationDao.save(userNotification);

        return userNotification;
    }

    private UserNotification sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type, long dvObjectId, String returnToAuthorReason) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(dvObjectId);
        userNotification.setReturnToAuthorReason(returnToAuthorReason);

        userNotificationDao.save(userNotification);

        return userNotification;
    }

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        return userNotification;
    }
}
