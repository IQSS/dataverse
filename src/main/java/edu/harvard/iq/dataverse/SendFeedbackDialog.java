package edu.harvard.iq.dataverse;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Naomi
 */
@Stateless
@Named
public class SendFeedbackDialog {

    String userEmail = "";
    String userMessage = "";
    
    
    @EJB
    MailServiceBean mailService;
    @Inject DataverseSession dataverseSession;
    
    public void setUserEmail (String uEmail) {
        userEmail = uEmail;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserMessage (String mess) {
        userMessage = mess;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public boolean isLoggedIn() {
        return !dataverseSession.getUser().isGuest();
    }
    
    public String loggedInUserEmail() {
        return dataverseSession.getUser().getEmail();
    }
    
    
    public String sendMessage() {
        if (isLoggedIn()) {
            mailService.sendMail(loggedInUserEmail(), "support@thedata.org", "Dataverse 4.0 Beta Feedback", userMessage);
            userMessage = "";
            return null;
        } else {
            if (userEmail != null && userMessage != null) {
                mailService.sendMail(userEmail, "support@thedata.org", "Dataverse 4.0 Beta Feedback", userMessage);
                userMessage = "";
                return null;
            } else {
                userMessage = "";
                return null;
            }
        }
    }
    
    
}
