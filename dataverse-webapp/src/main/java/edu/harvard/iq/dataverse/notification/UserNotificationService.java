package edu.harvard.iq.dataverse.notification;

import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationMapper;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationDao;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
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

        EmailNotificationDto emailNotificationDto = mailMapper.toDto(userNotification, dvObjectId, notificationObjectType);

        executorService.submit(() -> sendEmail(emailNotificationDto));
    }

    /**
     * Saves notification to database, then sends email asynchronously.
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

        EmailNotificationDto emailNotificationDto = mailMapper.toDto(userNotification, dvObjectId, notificationObjectType);

        executorService.submit(() -> sendEmail(emailNotificationDto, requestor));
    }

    // -------------------- PRIVATE --------------------

    private boolean sendEmail(EmailNotificationDto emailNotificationDto, AuthenticatedUser requester) {
        Boolean emailSent = mailService.sendNotificationEmail(emailNotificationDto, requester);

        if (emailSent) {
            userNotificationDao.updateEmailSent(emailNotificationDto.getUserNotificationId());
        }

        return emailSent;
    }

    private boolean sendEmail(EmailNotificationDto emailNotificationDto) {
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

    private UserNotification createUserNotification(AuthenticatedUser dataverseUser, Timestamp sendDate, NotificationType type) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(dataverseUser);
        userNotification.setSendDate(sendDate);
        userNotification.setType(type);
        return userNotification;
    }
}
