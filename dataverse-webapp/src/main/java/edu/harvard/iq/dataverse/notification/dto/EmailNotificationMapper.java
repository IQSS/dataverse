package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.NotificationParametersUtil;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class EmailNotificationMapper {

    private UserNotificationService userNotificationService;
    private NotificationParametersUtil notificationParametersUtil;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public EmailNotificationMapper() {
        this.notificationParametersUtil = new NotificationParametersUtil();
    }

    @Inject
    public EmailNotificationMapper(UserNotificationService userNotificationService) {
        this();
        this.userNotificationService = userNotificationService;
    }

    // -------------------- LOGIC --------------------

    public EmailNotificationDto toDto(UserNotification userNotification,
                                      NotificationObjectType notificationObjectType) {
        return new EmailNotificationDto(userNotification.getId(),
                userNotification.getUser().getDisplayInfo().getEmailAddress(),
                userNotification.getType(),
                userNotification.getObjectId(),
                notificationObjectType,
                userNotification.getUser(),
                notificationParametersUtil.getParameters(userNotification));
    }
}
