package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import org.apache.commons.lang.StringUtils;

public class EmailNotificationDto {

    private long userNotificationId;
    private String userEmail;
    private NotificationType notificationType;
    private Long dvObjectId;
    private NotificationObjectType notificationObjectType;
    private AuthenticatedUser notificationReceiver;
    private String customUserMessage;

    // -------------------- CONSTRUCTORS --------------------


    public EmailNotificationDto(long userNotificationId, String userEmail, NotificationType notificationType,
                                Long dvObjectId, NotificationObjectType notificationObjectType, AuthenticatedUser notificationReceiver) {
        this.userNotificationId = userNotificationId;
        this.userEmail = userEmail;
        this.notificationType = notificationType;
        this.dvObjectId = dvObjectId;
        this.notificationObjectType = notificationObjectType;
        this.notificationReceiver = notificationReceiver;
        this.customUserMessage = StringUtils.EMPTY;
    }

    public EmailNotificationDto(long userNotificationId, String userEmail, NotificationType notificationType,
                                Long dvObjectId, NotificationObjectType notificationObjectType, AuthenticatedUser notificationReceiver, String customUserMessage) {
        this.userNotificationId = userNotificationId;
        this.userEmail = userEmail;
        this.notificationType = notificationType;
        this.dvObjectId = dvObjectId;
        this.notificationObjectType = notificationObjectType;
        this.notificationReceiver = notificationReceiver;
        this.customUserMessage = customUserMessage;
    }

    // -------------------- GETTERS --------------------

    public long getUserNotificationId() {
        return userNotificationId;
    }

    public NotificationObjectType getNotificationObjectType() {
        return notificationObjectType;
    }

    public AuthenticatedUser getNotificationReceiver() {
        return notificationReceiver;
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

    public String getCustomUserMessage() {
        return customUserMessage;
    }
}
