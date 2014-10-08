/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import java.util.List;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 *
 * @author xyang
 */
@Path("mail")
public class Mail {

    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    MailServiceBean mailService;
    @EJB
    DataverseServiceBean dataverseService;

    private List<UserNotification> notificationsList;

    @GET
    public String sendMail() {
        notificationsList = userNotificationService.findUnemailed();
        if (notificationsList != null) {
            for (UserNotification notification : notificationsList) {
                mailService.sendCreateDataverseNotification(notification.getUser().getDisplayInfo().getEmailAddress(), dataverseService.find(notification.getObjectId()).getName(), dataverseService.find(notification.getObjectId()).getOwner().getName());
                notification.setEmailed(true);
                userNotificationService.save(notification);
            }
        }
        return null;
    }
}
