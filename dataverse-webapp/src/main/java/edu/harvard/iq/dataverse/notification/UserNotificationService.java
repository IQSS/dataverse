package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationMapper;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import org.awaitility.Awaitility;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class responsible for managing user notifications.
 */
@Stateless
public class UserNotificationService {

    private UserNotificationRepository userNotificationRepository;
    private MailService mailService;
    private EmailNotificationMapper mailMapper;

    private ExecutorService executorService;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated /* JEE requirement*/
    public UserNotificationService() {
    }

    @Inject
    public UserNotificationService(UserNotificationRepository userNotificationRepository, MailService mailService, EmailNotificationMapper mailMapper) {
        this.userNotificationRepository = userNotificationRepository;
        this.mailService = mailService;
        this.mailMapper = mailMapper;

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // -------------------- LOGIC --------------------

    /**
     * Saves notification to database, hereby sends notification to users dashboard.
     */
    public void sendNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, String type) {
        UserNotification userNotification = createUserNotification(dataverseUser, sendDate, type);

        userNotificationRepository.save(userNotification);
    }

    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param dvObjectId
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          String type,
                                          Long dvObjectId,
                                          NotificationObjectType notificationObjectType) {

        UserNotification userNotification = createUserNotification(dataverseUser, sendDate, type, dvObjectId);

        userNotificationRepository.saveAndFlush(userNotification);

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType));
    }


    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param dvObjectId
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          String type,
                                          Long dvObjectId,
                                          NotificationObjectType notificationObjectType,
                                          AuthenticatedUser requestor) {

        UserNotification userNotification = createUserNotification(dataverseUser, sendDate, type, dvObjectId, requestor);

        userNotificationRepository.saveAndFlush(userNotification);

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType));
    }

    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param dvObjectId
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          String type,
                                          Long dvObjectId,
                                          NotificationObjectType notificationObjectType,
                                          AuthenticatedUser requestor,
                                          String comment) {

        UserNotification userNotification = createUserNotification(dataverseUser, sendDate, type, dvObjectId, requestor, comment);

        userNotificationRepository.saveAndFlush(userNotification);

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType));
    }

    /**
     * Saves notification to database, then sends email asynchronously.
     *
     * @param notificationObjectType - type has to match correct #{@link NotificationType}
     * @param comment                - custom user message added to notification on '{@link NotificationType#RETURNEDDS}
     */
    public void sendNotificationWithEmail(AuthenticatedUser dataverseUser,
                                          Timestamp sendDate,
                                          String type,
                                          Long dvObjectId,
                                          NotificationObjectType notificationObjectType,
                                          String comment) {

        UserNotification userNotification = createUserNotification(dataverseUser, sendDate, type, dvObjectId, comment);

        userNotificationRepository.saveAndFlush(userNotification);

        executorService.submit(() -> sendEmail(userNotification.getId(), notificationObjectType));
    }
    // -------------------- PRIVATE --------------------

    private boolean sendEmail(long emailNotificationid, NotificationObjectType notificationObjectType) {
        UserNotification notification = Awaitility.await()
                .with()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .until(() -> userNotificationRepository.findById(emailNotificationid), Optional::isPresent)
                .get();

        EmailNotificationDto emailNotificationDto = mailMapper.toDto(notification,
                notificationObjectType);

        Boolean emailSent = mailService.sendNotificationEmail(emailNotificationDto);

        if (emailSent) {
            userNotificationRepository.updateEmailSent(emailNotificationDto.getUserNotificationId());
        }

        return emailSent;
    }

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, String type,
                                              Long dvObjectId, AuthenticatedUser requestor) {
        return createUserNotification(dataverseUser, sendDate, type, dvObjectId, requestor, null);
    }

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, String type, Long dvObjectId) {
        return createUserNotification(dataverseUser, sendDate, type, dvObjectId, null, null);
    }

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, String type, Long dvObjectId, String userMessage) {
        return createUserNotification(dataverseUser, sendDate, type, dvObjectId, null, userMessage);
    }

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, String type) {
        return createUserNotification(dataverseUser, sendDate, type, null, null, null);
    }

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, String type,
                                              Long dvObjectId, AuthenticatedUser requestor, String userMessage) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        userNotification.setObjectId(dvObjectId);
        userNotification.setRequestor(requestor);
        userNotification.setAdditionalMessage(userMessage);

        return userNotification;
    }
}
