package edu.harvard.iq.dataverse.notification.dto;

import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.user.UserNotification;

import javax.ejb.Stateless;

@Stateless
public class EmailNotificationMapper {

    public EmailNotificationDto toDto(UserNotification userNotification,
                                      long dvObjectId,
                                      NotificationObjectType notificationObjectType) {

        return new EmailNotificationDto(userNotification.getId(),
                                        userNotification.getUser().getDisplayInfo().getEmailAddress(),
                                        userNotification.getType(),
                                        dvObjectId,
                                        notificationObjectType,
                                        userNotification.getUser()
        );
    }

    public EmailNotificationDto toDto(UserNotification userNotification,
                                      Long dvObjectId,
                                      NotificationObjectType notificationObjectType, String returnReason) {


        return new EmailNotificationDto(userNotification.getId(),
                userNotification.getUser().getDisplayInfo().getEmailAddress(),
                userNotification.getType(),
                dvObjectId,
                notificationObjectType,
                userNotification.getUser(),
                returnReason
        );
    }
}
