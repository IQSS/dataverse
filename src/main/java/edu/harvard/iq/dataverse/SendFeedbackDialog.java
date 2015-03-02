package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Naomi
 */
@ViewScoped
@Named
public class SendFeedbackDialog implements java.io.Serializable {

    private String userEmail = "";
    private String userMessage = "";
    private String messageSubject = "";
    private String messageTo = "";
    private String defaultRecipientEmail = "support@thedata.org";
    // Either the dataverse or the dataset that the message is pertaining to
    // If there is no recipient, this is a general feeback message
    private DvObject recipient;
    private Logger logger = Logger.getLogger(SendFeedbackDialog.class.getCanonicalName());
    
    @EJB
    MailServiceBean mailService;
    @EJB
    DataverseServiceBean dataverseService; 
    @Inject DataverseSession dataverseSession;
    
    public void setUserEmail (String uEmail) {
        userEmail = uEmail;
    }

    public String getUserEmail() {
        return userEmail;
    }
    
    public void initUserInput(ActionEvent ae) {
        userEmail="";
        userMessage="";
        messageTo="";
        messageSubject="";
    }
    
    
    public String getMessageTo() {
        if (recipient == null) {
            return JH.localize("feedback.support");
        } else if (recipient.isInstanceofDataverse()) {
            return  ((Dataverse)recipient).getDisplayName() +" "+ JH.localize("feedback.contact");
        } else 
            return JH.localize("dataset") + " " + JH.localize("feedback.contact");
    }
    
    public String getFormHeader() {
        if (recipient == null) {
            return JH.localize("feedback.header");
        } else if (recipient.isInstanceofDataverse()) {
            return   JH.localize("feedback.dataverse.header");
        } else 
            return JH.localize("feedback.dataset.header");
    }

    public void setUserMessage (String mess) {
        userMessage = mess;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
    }
    
    public String getMessageSubject() {
         return messageSubject; 
    }
    
    public boolean isLoggedIn() {
        return dataverseSession.getUser().isAuthenticated();
    }
    
    public String loggedInUserEmail() {
        return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
    }

    public DvObject getRecipient() {
        return recipient;
    }

    public void setRecipient(DvObject recipient) {
          this.recipient = recipient;
    }
    
    private String getDataverseEmail() {
        String email = "";
        Dataverse dv = (Dataverse) recipient;
        for (DataverseContact dc : dv.getDataverseContacts()) {
            if (!email.isEmpty()) {
                email += ",";
            }
            email += dc.getContactEmail();
        }
        return email;
    }
    
    public String sendMessage() {
        String email = "";
        if (recipient!=null) {
            if (recipient.isInstanceofDataverse() ) {
               email = getDataverseEmail();
            }
            else if (recipient.isInstanceofDataset()) {
                Dataset d = (Dataset)recipient;
                for (DatasetField df : d.getLatestVersion().getFlatDatasetFields()){
                    if (df.getDatasetFieldType().equals(DatasetFieldConstant.datasetContactEmail)) {
                        if (!email.isEmpty()) {
                            email+=",";
                        }
                        email+=df.getValue();
                    }
                }
                if (email.isEmpty()) {
                    email = getDataverseEmail();
                }
            }
        }
        if (email.isEmpty()) {
            email = defaultRecipientEmail;
        }
        logger.info("sending email to: "+email);
        if (isLoggedIn() && userMessage!=null) {
        //    mailService.sendMail(loggedInUserEmail(), email, getMessageSubject(), userMessage);
            userMessage = "";
            return null;
        } else {
            if (userEmail != null && userMessage != null) {
            //    mailService.sendMail(userEmail, email, getMessageSubject(), userMessage);
                userMessage = "";
                return null;
            } else {
                userMessage = "";
                return null;
            }
        }
    }

    
}
