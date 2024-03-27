package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.validator.routines.EmailValidator;

@ViewScoped
@Named
public class SendFeedbackDialog implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SendFeedbackDialog.class.getCanonicalName());

    /**
     * The email address supplied by the person filling out the contact form.
     */
    private String userEmail = "";

    /**
     * Body of the message.
     */
    private String userMessage = "";

    /**
     * Becomes the subject of the email.
     */
    private String messageSubject = "";

    /**
     * First operand in addition problem.
     */
    Long op1;

    /**
     * Second operand in addition problem.
     */
    Long op2;

    /**
     * The guess the user makes in addition problem.
     */
    Long userSum;

    /**
     * Either the dataverse or the dataset that the message is pertaining to. If
     * there is no recipient, this is a general feedback message.
     */
    private DvObject feedbackTarget;

    /**
     * :SystemEmail (the main support address for an installation).
     */
    private InternetAddress systemAddress;

    @EJB
    MailServiceBean mailService;

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession dataverseSession;

    public void setUserEmail(String uEmail) {
        userEmail = uEmail;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void initUserInput(ActionEvent ae) {
        userEmail = "";
        userMessage = "";
        messageSubject = "";
        Random random = new Random();
        op1 = Long.valueOf(random.nextInt(10));
        op2 = Long.valueOf(random.nextInt(10));
        userSum = null;
        systemAddress = mailService.getSupportAddress().orElse(null);
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
        if (feedbackTarget == null) {
            return BrandingUtil.getSupportTeamName(systemAddress);
        } else if (feedbackTarget.isInstanceofDataverse()) {
            return ((Dataverse) feedbackTarget).getDisplayName() + " " + BundleUtil.getStringFromBundle("contact.contact");
        } else {
            return BundleUtil.getStringFromBundle("dataset") + " " + BundleUtil.getStringFromBundle("contact.contact");
        }
    }
    
    public String getMessageCC() {
        if (ccSupport()) {
            return BrandingUtil.getSupportTeamName(systemAddress);
        }
        return null;
    }


    public String getFormHeader() {
        if (feedbackTarget == null) {
            return BrandingUtil.getContactHeader(systemAddress);
        } else if (feedbackTarget.isInstanceofDataverse()) {
            return BundleUtil.getStringFromBundle("contact.dataverse.header");
        } else {
            return BundleUtil.getStringFromBundle("contact.dataset.header");
        }
    }

    public void setUserMessage(String mess) {
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
        return feedbackTarget;
    }

    public void setRecipient(DvObject recipient) {
        this.feedbackTarget = recipient;
    }

    public void validateUserSum(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (op1 + op2 != (Long) value) {
            // TODO: Remove this English "Sum is incorrect" string. contactFormFragment.xhtml uses contact.sum.invalid instead.
            FacesMessage msg = new FacesMessage(BundleUtil.getStringFromBundle("contact.sum.invalid"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    public void validateUserEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!EmailValidator.getInstance().isValid((String) value)) {
            FacesMessage msg = new FacesMessage(BundleUtil.getStringFromBundle("oauth2.newAccount.emailInvalid"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    public String sendMessage() {
        String installationBrandName = BrandingUtil.getInstallationBrandName();
        String supportTeamName = BrandingUtil.getSupportTeamName(systemAddress);

        Feedback feedback = FeedbackUtil.gatherFeedback(feedbackTarget, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, systemConfig.getDataverseSiteUrl(), installationBrandName, supportTeamName, ccSupport());
        if (feedback==null) {
            logger.warning("No feedback has been sent!");
            return null;
        }
            logger.fine("sending feedback: " + feedback);
            mailService.sendMail(feedback.getFromEmail(), feedback.getToEmail(), feedback.getCcEmail(), feedback.getSubject(), feedback.getBody());
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("contact.sent"));
        return null;
    }
    
    public boolean ccSupport() {
        return ccSupport(feedbackTarget);
    }
    
    public static boolean ccSupport(DvObject feedbackTarget) {
        //Setting is enabled and this isn't already a direct message to support (no feedbackTarget)
        Optional<Boolean> ccSupport = JvmSettings.CC_SUPPORT_ON_CONTACT_EMAIL.lookupOptional(Boolean.class);
        
        return feedbackTarget!=null && ccSupport.isPresent() &&ccSupport.get();
    }

}
