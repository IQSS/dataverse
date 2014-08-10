package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.GuestUser;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.hibernate.validator.constraints.Email;

/**
 *
 * @author Naomi
 */
@ViewScoped
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
        return dataverseSession.getUser() != GuestUser.get();
    }
    
    public String loggedInUserEmail() {
        return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
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
