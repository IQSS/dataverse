package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.feedback.Feedback;
import edu.harvard.iq.dataverse.feedback.FeedbackUtil;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.validator.routines.EmailValidator;
import org.omnifaces.cdi.ViewScoped;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

@ViewScoped
@Named
public class SendFeedbackDialog implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SendFeedbackDialog.class.getCanonicalName());

    private MailService mailService;
    private SettingsServiceBean settingsService;
    private DataverseDao dataverseDao;
    private SystemConfig systemConfig;
    private DataverseSession dataverseSession;

    /** The email address supplied by the person filling out the contact form. */
    private String userEmail = "";

    /** Body of the message. */
    private String userMessage = "";

    /** Becomes the subject of the email. */
    private String messageSubject = "";

    /** First operand in addition problem. */
    private Long op1;

    /** Second operand in addition problem. */
    private Long op2;

    /** The guess the user makes in addition problem. */
    private Long userSum;

    /**
     * Either the dataverse or the dataset that the message is pertaining to.
     * If there is no recipient, this is a general feedback message.
     */
    private DvObject recipient;

    /** Whether a copy of the message should be sent to user's mail */
    private boolean sendCopy;

    /** :SystemEmail (the main support address for an installation). */
    private InternetAddress systemAddress;

    // -------------------- CONSTRUCTORS --------------------

    public SendFeedbackDialog() { }

    @Inject
    public SendFeedbackDialog(MailService mailService, SettingsServiceBean settingsService,
                              DataverseDao dataverseDao, SystemConfig systemConfig,
                              DataverseSession dataverseSession) {
        this.mailService = mailService;
        this.settingsService = settingsService;
        this.dataverseDao = dataverseDao;
        this.systemConfig = systemConfig;
        this.dataverseSession = dataverseSession;
    }

    // -------------------- GETTERS --------------------

    public String getUserEmail() {
        return userEmail;
    }

    public Long getOp1() {
        return op1;
    }

    public Long getOp2() {
        return op2;
    }

    public Long getUserSum() {
        return userSum;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public boolean getSendCopy() {
        return sendCopy;
    }

    public DvObject getRecipient() {
        return recipient;
    }

    // -------------------- LOGIC --------------------

    public void initUserInput() {
        userEmail = "";
        userMessage = "";
        messageSubject = "";
        Random random = new Random();
        op1 = (long) random.nextInt(10);
        op2 = (long) random.nextInt(10);
        userSum = null;
        String systemEmail = settingsService.getValueForKey(SettingsServiceBean.Key.SystemEmail);
        systemAddress = MailUtil.parseSystemAddress(systemEmail);
    }

    public void initUserInput(ActionEvent ae) {
        initUserInput();
    }

    public String getMessageTo() {
        if (recipient == null) {
            return BrandingUtil.getSupportTeamName(systemAddress, dataverseDao.findRootDataverse().getName());
        } else if (recipient.isInstanceofDataverse()) {
            return recipient.getDisplayName() + " " + BundleUtil.getStringFromBundle("contact.contact");
        } else {
            return BundleUtil.getStringFromBundle("dataset") + " " + BundleUtil.getStringFromBundle("contact.contact");
        }
    }

    public String getFormHeader() {
        if (recipient == null) {
            return BrandingUtil.getContactHeader(systemAddress, dataverseDao.findRootDataverse().getName());
        } else if (recipient.isInstanceofDataverse()) {
            return BundleUtil.getStringFromBundle("contact.dataverse.header");
        } else {
            return BundleUtil.getStringFromBundle("contact.dataset.header");
        }
    }

    public void validateUserSum(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (op1 + op2 != (Long) value) {
            FacesMessage msg = new FacesMessage(BundleUtil.getStringFromBundle("contact.sum.invalid"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    public void validateUserEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (!EmailValidator.getInstance().isValid((String) value)) {
            FacesMessage msg = new FacesMessage(BundleUtil.getStringFromBundle("external.newAccount.emailInvalid"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }

    public String sendMessage() {
        // FIXME: move dataverseDao.findRootDataverse() to init
        String rootDataverseName = dataverseDao.findRootDataverse().getName();
        String installationBrandName = BrandingUtil.getInstallationBrandName(rootDataverseName);
        String supportTeamName = BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName);
        List<Feedback> feedbacks = FeedbackUtil.gatherFeedback(recipient, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, systemConfig.getDataverseSiteUrl(), installationBrandName, supportTeamName);
        if (feedbacks.isEmpty()) {
            logger.warning("No feedback has been sent!");
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("contact.send.failure"));
            return null;
        }
        for (Feedback feedback : feedbacks) {
            logger.fine("sending feedback: " + feedback);
            mailService.sendMailAsync(feedback.getFromEmail(), feedback.getToEmail(), feedback.getSubject(), feedback.getBody());
        }
        if (sendCopy) {
            String mail = isLoggedIn() ? loggedInUserEmail() : userEmail;
            mailService.sendMailAsync(null, mail,
                    BundleUtil.getStringFromBundle("contact.copy.message.subject", messageSubject),
                    BundleUtil.getStringFromBundle("contact.copy.message.template", userMessage));
        }
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("contact.send.success"));

        return null;
    }

    public boolean isLoggedIn() {
        return dataverseSession.getUser().isAuthenticated();
    }

    public String loggedInUserEmail() {
        return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
    }

    // -------------------- SETTERS --------------------

    public void setUserEmail(String uEmail) {
        userEmail = uEmail;
    }

    public void setOp1(Long op1) {
        this.op1 = op1;
    }

    public void setOp2(Long op2) {
        this.op2 = op2;
    }

    public void setUserSum(Long userSum) {
        this.userSum = userSum;
    }

    public void setUserMessage(String mess) {
        userMessage = mess;
    }

    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
    }

    public void setSendCopy(boolean sendCopy) {
        this.sendCopy = sendCopy;
    }

    public void setRecipient(DvObject recipient) {
        this.recipient = recipient;
    }
}