package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    public static List<Feedback> gatherFeedback(DvObject recipient, DataverseSession dataverseSession, String messageSubject, String userMessage, InternetAddress systemAddress, String userEmail, String dataverseSiteUrl, String installationBrandName, String supportTeamName) {
        List<Feedback> feedbacks = new ArrayList<>();
        String contextIntro = "";
        String contextEnding = "";
        String contactFullName = "";
        String toEmail = "";
        if (recipient != null) {
            if (recipient.isInstanceofDataverse()) {
                Dataverse dataverse = (Dataverse) recipient;
                contextIntro = BundleUtil.getStringFromBundle("contact.context.dataverse.intro", Arrays.asList(dataverseSiteUrl, dataverse.getAlias()));
                toEmail = getDataverseEmail(dataverse);
            } else if (recipient.isInstanceofDataset()) {
                Dataset dataset = (Dataset) recipient;
                List<DvObjectContact> datasetContacts = getDatasetContacts(dataset);
                for (DvObjectContact datasetContact : datasetContacts) {
                    logger.fine(datasetContact.getName() + ": " + datasetContact.getEmail());
                }
                // FIXME: support more than one dataset contact!
                DvObjectContact firstDatasetContact = datasetContacts.get(0);
                // TODO: Test this greeting a lot.
                contactFullName = getGreeting(firstDatasetContact);
                String datasetTitle = dataset.getLatestVersion().getTitle();
                String datasetPid = dataset.getGlobalId();
                contextIntro = BundleUtil.getStringFromBundle("contact.context.dataset.intro", Arrays.asList(contactFullName, userEmail, installationBrandName, datasetTitle, datasetPid));
                contextEnding = BundleUtil.getStringFromBundle("contact.context.dataset.ending", Arrays.asList(supportTeamName, systemAddress.toString(), dataverseSiteUrl, dataset.getGlobalId(), supportTeamName, systemAddress.toString()));
                toEmail = getDatasetEmail(dataset);
            } else if (recipient.isInstanceofDataFile()) {
                DataFile datafile = (DataFile) recipient;
                contextIntro = BundleUtil.getStringFromBundle("contact.context.file.intro", Arrays.asList(dataverseSiteUrl, datafile.getId().toString()));
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
            Feedback feedback = new Feedback(loggedInUserEmail(dataverseSession), toEmail, messageSubject, contextIntro + userMessage + contextEnding);
            feedbacks.add(feedback);
        } else {
            if (userEmail != null && userMessage != null) {
                Feedback feedback = new Feedback(userEmail, toEmail, messageSubject, contextIntro + userMessage + contextEnding);
                feedbacks.add(feedback);
                return feedbacks;
            } else {
                logger.warning("No feedback gathered because userEmail or userMessage was null! userEmail: " + userEmail + ". userMessage: " + userMessage);
                return Collections.EMPTY_LIST;
            }
        }
        return feedbacks;
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

    private static List<DvObjectContact> getDatasetContacts(Dataset dataset) {
        List<DvObjectContact> retList = new ArrayList<>();
        for (DatasetField dsf : dataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact)) {
                // FIXME: Put English in bundle
                String saneDefaultForName = "dataset contact";
                String contactName = saneDefaultForName;
                String contactEmail = null;
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactName)) {
                            contactName = subField.getDisplayValue();
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                            contactEmail = subField.getDisplayValue();
                        }
                    }
                    if (contactEmail != null) {
                        DvObjectContact datasetContact = new DvObjectContact(contactName, contactEmail);
                        retList.add(datasetContact);
                    } else {
                        logger.warning("email missing for contact in dataset " + dataset.getIdentifier());
                    }
                }

            }
        }
        return retList;
    }

    /**
     * When contacts are people we suggest that they be stored as "Simpson,
     * Homer" so the idea of this method is that it returns "Homer Simpson", if
     * it can.
     *
     * Contacts don't necessarily have to be people, however. They can be
     * organizations. We ran into similar trouble (but for authors) when
     * implementing Schema.org JSON-LD support. See getJsonLd on DatasetVersion.
     * Some day it might be nice to store whether an author or a contact is a
     * person or an organization.
     */
    private static String getGreeting(DvObjectContact firstDatasetContact) {
        try {
            String lastFirstString = firstDatasetContact.getName();
            String[] lastFirstParts = lastFirstString.split(",");
            String last = lastFirstParts[0];
            String first = lastFirstParts[1];
            // TODO: Move this English to the bundle.
            return "Hello " + first.trim() + " " + last.trim() + ",";
        } catch (Exception ex) {
            logger.warning("problem in getGreeting: " + ex);
            // TODO: Move this English to the bundle.
            return "Attention Dataset Contact";
        }
    }

}
