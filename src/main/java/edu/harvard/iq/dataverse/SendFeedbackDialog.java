package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.MailUtil;
import java.util.Random;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.InternetAddress;

import org.apache.commons.validator.routines.EmailValidator;

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
    Long op1, op2, userSum;
    // Either the dataverse or the dataset that the message is pertaining to
    // If there is no recipient, this is a general feeback message
    private DvObject recipient;
    private Logger logger = Logger.getLogger(SendFeedbackDialog.class.getCanonicalName());
    
    @EJB
    MailServiceBean mailService;
    @EJB
    SettingsServiceBean settingsService;
    
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
        System.out.println("initUserInput()");
        userEmail="";
        userMessage="";
        messageTo="";
        messageSubject="";
        Random random = new Random();
        op1 = new Long(random.nextInt(10));
        op2 = new Long(random.nextInt(10));
        userSum=null;
        
    }

    public Long getOp1() {
        return op1;
    }

    public void setOp1(Long op1) {
        this.op1 = op1;
    }

    public Long getOp2() {
        return op2;
    }

    public void setOp2(Long op2) {
        this.op2 = op2;
    }

    public Long getUserSum() {
        return userSum;
    }

    public void setUserSum(Long userSum) {
        this.userSum = userSum;
    }
    
    
    public String getMessageTo() {
        if (recipient == null) {
            return JH.localize("contact.support");
        } else if (recipient.isInstanceofDataverse()) {
            return  ((Dataverse)recipient).getDisplayName() +" "+ JH.localize("contact.contact");
        } else 
            return JH.localize("dataset") + " " + JH.localize("contact.contact");
    }
    
    public String getFormHeader() {
        if (recipient == null) {
            return JH.localize("contact.header");
        } else if (recipient.isInstanceofDataverse()) {
            return   JH.localize("contact.dataverse.header");
        } else 
            return JH.localize("contact.dataset.header");
    }

    public void setUserMessage (String mess) {
        System.out.println("setUserMessage: "+mess);
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
    
    private String getDataverseEmail(Dataverse dataverse) {
        String email = "";
       
        for (DataverseContact dc : dataverse.getDataverseContacts()) {
            if (!email.isEmpty()) {
                email += ",";
            }
            email += dc.getContactEmail();
        }
        return email;
    }
      public void validateUserSum(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        if (op1 + op2 !=(Long)value) {

            FacesMessage msg
                    = new FacesMessage("Sum is incorrect, please try again.");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }

    }
      
  public void validateUserEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        if (!EmailValidator.getInstance().isValid((String)value)) {

            FacesMessage msg
                    = new FacesMessage("Invalid email.");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(msg);
        }

    }     
    public String sendMessage() {
        String email = "";
        if (recipient!=null) {
            if (recipient.isInstanceofDataverse() ) {
               email = getDataverseEmail((Dataverse)recipient);
            }
            else if (recipient.isInstanceofDataset()) {
                Dataset d = (Dataset)recipient;
                for (DatasetField df : d.getLatestVersion().getFlatDatasetFields()){
                    if (df.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                        if (!email.isEmpty()) {
                            email+=",";
                        }
                        email+=df.getValue();
                    }
                }
                if (email.isEmpty()) {
                    email = getDataverseEmail(d.getOwner());
                }
            }
        }
        if (email.isEmpty()) {
                String systemEmail =  settingsService.getValueForKey(SettingsServiceBean.Key.SystemEmail);
                InternetAddress systemAddress =  MailUtil.parseSystemAddress(systemEmail);
                if (systemAddress != null){
                    email = systemAddress.toString();
                } else{
                    email = defaultRecipientEmail;
                }
        }
        if (isLoggedIn() && userMessage!=null) {
            mailService.sendMail(loggedInUserEmail(), email, getMessageSubject(), userMessage);
            userMessage = "";
            return null;
        } else {
            if (userEmail != null && userMessage != null) {
                mailService.sendMail(userEmail, email, getMessageSubject(), userMessage);
                userMessage = "";
                return null;
            } else {
                userMessage = "";
                return null;
            }
        }
    }

    
}
