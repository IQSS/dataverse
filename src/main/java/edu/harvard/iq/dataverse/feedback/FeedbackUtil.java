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
import java.util.Arrays;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    public static Feedback gatherFeedback(DvObject recipient, DataverseSession dataverseSession, String messageSubject, String userMessage, InternetAddress systemAddress, String userEmail, String dataverseSiteUrl) {
        String toEmail = "";
        String preambleWithContext = "";
        if (recipient != null) {
            if (recipient.isInstanceofDataverse()) {
                Dataverse dataverse = (Dataverse) recipient;
                preambleWithContext = BundleUtil.getStringFromBundle("contact.context.preamble.dataverse", Arrays.asList(dataverseSiteUrl, dataverse.getAlias()));
                toEmail = getDataverseEmail(dataverse);
            } else if (recipient.isInstanceofDataset()) {
                Dataset dataset = (Dataset) recipient;
                preambleWithContext = BundleUtil.getStringFromBundle("contact.context.preamble.dataset", Arrays.asList(dataverseSiteUrl, dataset.getGlobalId()));
                toEmail = getDatasetEmail(dataset);
            } else if (recipient.isInstanceofDataFile()) {
                DataFile datafile = (DataFile) recipient;
                preambleWithContext = BundleUtil.getStringFromBundle("contact.context.preamble.file", Arrays.asList(dataverseSiteUrl, datafile.getId().toString()));
                toEmail = getDatasetEmail(datafile.getOwner());
            }
        }
        if (toEmail.isEmpty()) {
            if (systemAddress != null) {
                toEmail = systemAddress.toString();
            } else {
                String defaultRecipientEmail = null;
                toEmail = defaultRecipientEmail;
            }
        }
        if (isLoggedIn(dataverseSession) && userMessage != null) {
            // TODO: Get the test in FeedbackUtilTest working.
            return new Feedback(loggedInUserEmail(dataverseSession), toEmail, messageSubject, preambleWithContext + userMessage);
        } else {
            if (userEmail != null && userMessage != null) {
                return new Feedback(userEmail, toEmail, messageSubject, preambleWithContext + userMessage);
            } else {
                logger.warning("No feedback gathered because userEmail or userMessage was null! userEmail: " + userEmail + ". userMessage: " + userMessage);
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
        return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
    }

    // Testing multiple dataset contacts is done in FeedbackApiIT.java
    private static String getDatasetEmail(Dataset dataset) {
        String toEmail = "";
        for (DatasetField df : dataset.getLatestVersion().getFlatDatasetFields()) {
            if (df.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                if (!toEmail.isEmpty()) {
                    toEmail += ",";
                }
                toEmail += df.getValue();
            }
        }
        if (toEmail.isEmpty()) {
            logger.warning("Dataset has no contact! This is a required field. Looking for a contact one level up at its parent dataverse.");
            toEmail = getDataverseEmail(dataset.getOwner());
        }
        return toEmail;
    }

}
