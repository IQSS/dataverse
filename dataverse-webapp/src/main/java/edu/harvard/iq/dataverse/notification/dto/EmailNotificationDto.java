package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.apache.commons.lang.StringUtils;

public class EmailNotificationDto {

    private long userNotificationId;
    private String userEmail;
    private String notificationType;
    private Long dvObjectId;
    private NotificationObjectType notificationObjectType;
    private AuthenticatedUser notificationReceiver;
    private AuthenticatedUser requestor;
    private String customUserMessage;

    // -------------------- CONSTRUCTORS --------------------


    public EmailNotificationDto(long userNotificationId, String userEmail, String notificationType,
                                Long dvObjectId, NotificationObjectType notificationObjectType, AuthenticatedUser notificationReceiver) {
        this(userNotificationId, userEmail, notificationType, dvObjectId, notificationObjectType, 
                notificationReceiver, null, null);
    }

    public EmailNotificationDto(long userNotificationId, String userEmail, String notificationType,
                                Long dvObjectId, NotificationObjectType notificationObjectType,
                                AuthenticatedUser notificationReceiver, AuthenticatedUser requestor, String customUserMessage) {
        this.userNotificationId = userNotificationId;
        this.userEmail = userEmail;
        this.notificationType = notificationType;
        this.dvObjectId = dvObjectId;
        this.notificationObjectType = notificationObjectType;
        this.notificationReceiver = notificationReceiver;
        this.requestor = requestor;
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

    public AuthenticatedUser getRequestor() {
        return requestor;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public long getDvObjectId() {
        return dvObjectId;
    }

    public String getCustomUserMessage() {
        return customUserMessage;
    }
}
