package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    public static Feedback gatherFeedback(DvObject recipient, DataverseSession dataverseSession, String messageSubject, String userMessage, String systemEmail, String userEmail, String dataverseSiteUrl) {
        String email = "";
        String preambleWithContext = "";
        if (recipient != null) {
            logger.info("recipient is type " + recipient.getClass().getName());
            if (recipient.isInstanceofDataverse()) {
                Dataverse dataverse = (Dataverse) recipient;
                preambleWithContext = BundleUtil.getStringFromBundle("contact.context.preamble.dataverse", Arrays.asList(dataverseSiteUrl, dataverse.getAlias()));
                email = getDataverseEmail((Dataverse) recipient);
            } else if (recipient.isInstanceofDataset()) {
                Dataset dataset = (Dataset) recipient;
                preambleWithContext = BundleUtil.getStringFromBundle("contact.context.preamble.dataset", Arrays.asList(dataverseSiteUrl, dataset.getGlobalId()));
                Dataset d = (Dataset) recipient;
                for (DatasetField df : d.getLatestVersion().getFlatDatasetFields()) {
                    if (df.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                        if (!email.isEmpty()) {
                            email += ",";
                        }
                        email += df.getValue();
                    }
                }
                if (email.isEmpty()) {
                    email = getDataverseEmail(d.getOwner());
                }
            } else if (recipient.isInstanceofDataFile()) {
                DataFile datafile = (DataFile) recipient;
                preambleWithContext = BundleUtil.getStringFromBundle("contact.context.preamble.file", Arrays.asList(dataverseSiteUrl, datafile.getId().toString()));
            }
        }
        if (email.isEmpty()) {
//            String systemEmail = settingsService.getValueForKey(SettingsServiceBean.Key.SystemEmail);
            InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);
            if (systemAddress != null) {
                email = systemAddress.toString();
            } else {
                String defaultRecipientEmail = null;
                email = defaultRecipientEmail;
            }
        }
        if (isLoggedIn(dataverseSession) && userMessage != null) {
//            mailService.sendMail(loggedInUserEmail(), email, getMessageSubject(), userMessage);
//            userMessage = "";
//               sendMail(String from, String to, String subject, String messageText)
            return new Feedback(loggedInUserEmail(dataverseSession), email, messageSubject, preambleWithContext + userMessage);
        } else {
            if (userEmail != null && userMessage != null) {
//                mailService.sendMail(userEmail, email, getMessageSubject(), userMessage);
//                userMessage = "";
                return new Feedback(userEmail, email, messageSubject, preambleWithContext + userMessage);
            } else {
//                userMessage = "";
                return null;
            }
        }
    }

    private static String getDataverseEmail(Dataverse dataverse) {
        String email = "";

        for (DataverseContact dc : dataverse.getDataverseContacts()) {
            if (!email.isEmpty()) {
                email += ",";
            }
            email += dc.getContactEmail();
        }
        return email;
    }

    private static boolean isLoggedIn(DataverseSession dataverseSession) {
        if (dataverseSession != null) {
            return dataverseSession.getUser().isAuthenticated();
        }
        return false;
    }

    private static String loggedInUserEmail(DataverseSession dataverseSession) {
        if (dataverseSession != null) {
            return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
        }
        return null;
    }

}
