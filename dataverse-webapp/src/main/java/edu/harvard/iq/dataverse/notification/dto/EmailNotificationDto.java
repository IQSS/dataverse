package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;

public class EmailNotificationDto {

    private long userNotificationId;
    private String userEmail;
    private NotificationType notificationType;
    private long dvObjectId;
    private NotificationObjectType notificationObjectType;
    private AuthenticatedUser user;

    // -------------------- CONSTRUCTORS --------------------


    public EmailNotificationDto(long userNotificationId, String userEmail, NotificationType notificationType,
                                long dvObjectId, NotificationObjectType notificationObjectType, AuthenticatedUser user) {
        this.userNotificationId = userNotificationId;
        this.userEmail = userEmail;
        this.notificationType = notificationType;
        this.dvObjectId = dvObjectId;
        this.notificationObjectType = notificationObjectType;
        this.user = user;
    }

    // -------------------- GETTERS --------------------

    public long getUserNotificationId() {
        return userNotificationId;
    }

    public NotificationObjectType getNotificationObjectType() {
        return notificationObjectType;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public long getDvObjectId() {
        return dvObjectId;
    }
}
