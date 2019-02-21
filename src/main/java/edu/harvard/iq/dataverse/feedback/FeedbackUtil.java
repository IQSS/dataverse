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
import java.util.List;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    private static final String NO_DATASET_CONTACT_INTRO = BundleUtil.getStringFromBundle("contact.context.dataset.noContact");

    // TODO: consider changing "recipient" into an object called something like FeedbackTarget
    public static List<Feedback> gatherFeedback(DvObject recipient, DataverseSession dataverseSession, String messageSubject, String userMessage, InternetAddress systemAddress, String userEmail, String dataverseSiteUrl, String installationBrandName, String supportTeamName) {
        String systemEmail = null;
        if (systemAddress != null) {
            systemEmail = systemAddress.getAddress();
        }
        logger.fine("systemAddress: " + systemAddress);
        List<Feedback> feedbacks = new ArrayList<>();
        if (isLoggedIn(dataverseSession)) {
            userEmail = loggedInUserEmail(dataverseSession);
        }
        if (recipient != null) {
            messageSubject = BundleUtil.getStringFromBundle("contact.context.subject.dvobject", Arrays.asList(installationBrandName, messageSubject));
            if (recipient.isInstanceofDataverse()) {
                Dataverse dataverse = (Dataverse) recipient;
                String dataverseContextEnding = BundleUtil.getStringFromBundle("contact.context.dataverse.ending", Arrays.asList(supportTeamName, systemEmail, dataverseSiteUrl, dataverse.getAlias(), supportTeamName, systemEmail));
                List<DvObjectContact> dataverseContacts = getDataverseContacts(dataverse);
                for (DvObjectContact dataverseContact : dataverseContacts) {
                    String placeHolderIfDataverseContactsGetNames = "";
                    String dataverseContextIntro = BundleUtil.getStringFromBundle("contact.context.dataverse.intro", Arrays.asList(placeHolderIfDataverseContactsGetNames, userEmail, installationBrandName, dataverse.getAlias()));
                    Feedback feedback = new Feedback(userEmail, dataverseContact.getEmail(), messageSubject, dataverseContextIntro + userMessage + dataverseContextEnding);
                    feedbacks.add(feedback);
                }
                if (!feedbacks.isEmpty()) {
                    return feedbacks;
                } else {
                    String dataverseContextIntroError = BundleUtil.getStringFromBundle("contact.context.dataverse.noContact");
                    Feedback feedback = new Feedback(userEmail, systemEmail, messageSubject, dataverseContextIntroError + userMessage + dataverseContextEnding);
                    feedbacks.add(feedback);
                    return feedbacks;
                }
            } else if (recipient.isInstanceofDataset()) {
                Dataset dataset = (Dataset) recipient;
                String datasetTitle = dataset.getLatestVersion().getTitle();
                String datasetPid = dataset.getGlobalIdString();
                String datasetContextEnding = BundleUtil.getStringFromBundle("contact.context.dataset.ending", Arrays.asList(supportTeamName, systemEmail, dataverseSiteUrl, dataset.getGlobalIdString(), supportTeamName, systemEmail));
                List<DvObjectContact> datasetContacts = getDatasetContacts(dataset);
                for (DvObjectContact datasetContact : datasetContacts) {
                    String contactFullName = getGreeting(datasetContact);
                    String datasetContextIntro = BundleUtil.getStringFromBundle("contact.context.dataset.intro", Arrays.asList(contactFullName, userEmail, installationBrandName, datasetTitle, datasetPid));
                    Feedback feedback = new Feedback(userEmail, datasetContact.getEmail(), messageSubject, datasetContextIntro + userMessage + datasetContextEnding);
                    feedbacks.add(feedback);
                }
                if (!feedbacks.isEmpty()) {
                    return feedbacks;
                } else {
                    // TODO: Add more of an intro for the person receiving the system email in this "no dataset contact" scenario?
                    Feedback feedback = new Feedback(userEmail, systemEmail, messageSubject, NO_DATASET_CONTACT_INTRO + userMessage + datasetContextEnding);
                    feedbacks.add(feedback);
                    return feedbacks;
                }
            } else {
                DataFile datafile = (DataFile) recipient;
                String datasetTitle = datafile.getOwner().getLatestVersion().getTitle();
                String datasetPid = datafile.getOwner().getGlobalIdString();
                String filename = datafile.getFileMetadatas().get(0).getLabel();
                List<DvObjectContact> datasetContacts = getDatasetContacts(datafile.getOwner());
                String fileContextEnding = BundleUtil.getStringFromBundle("contact.context.file.ending", Arrays.asList(supportTeamName, systemEmail, dataverseSiteUrl, datafile.getId().toString(), supportTeamName, systemEmail));
                for (DvObjectContact datasetContact : datasetContacts) {
                    String contactFullName = getGreeting(datasetContact);
                    String fileContextIntro = BundleUtil.getStringFromBundle("contact.context.file.intro", Arrays.asList(contactFullName, userEmail, installationBrandName, filename, datasetTitle, datasetPid));
                    Feedback feedback = new Feedback(userEmail, datasetContact.getEmail(), messageSubject, fileContextIntro + userMessage + fileContextEnding);
                    feedbacks.add(feedback);
                }
                if (!feedbacks.isEmpty()) {
                    return feedbacks;
                } else {
                    // TODO: Add more of an intro for the person receiving the system email in this "no dataset contact" scenario?
                    Feedback feedback = new Feedback(userEmail, systemEmail, messageSubject, NO_DATASET_CONTACT_INTRO + userMessage + fileContextEnding);
                    feedbacks.add(feedback);
                    return feedbacks;
                }
            }
        } else {
            messageSubject = BundleUtil.getStringFromBundle("contact.context.subject.support", Arrays.asList(installationBrandName, messageSubject));
            String noDvObjectContextIntro = BundleUtil.getStringFromBundle("contact.context.support.intro", Arrays.asList(supportTeamName, userEmail));
            String noDvObjectContextEnding = BundleUtil.getStringFromBundle("contact.context.support.ending", Arrays.asList(""));
            Feedback feedback = new Feedback(userEmail, systemEmail, messageSubject, noDvObjectContextIntro + userMessage + noDvObjectContextEnding);
            feedbacks.add(feedback);
            return feedbacks;
        }
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

    private static List<DvObjectContact> getDataverseContacts(Dataverse dataverse) {
        List<DvObjectContact> dataverseContacts = new ArrayList<>();
        for (DataverseContact dc : dataverse.getDataverseContacts()) {
            DvObjectContact dataverseContact = new DvObjectContact("", dc.getContactEmail());
            dataverseContacts.add(dataverseContact);
        }
        return dataverseContacts;
    }

    private static List<DvObjectContact> getDatasetContacts(Dataset dataset) {
        List<DvObjectContact> datasetContacts = new ArrayList<>();
        for (DatasetField dsf : dataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact)) {
                String contactName = null;
                String contactEmail = null;
                for (DatasetFieldCompoundValue datasetContactValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : datasetContactValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactName)) {
                            contactName = subField.getValue();
                            logger.fine("contactName: " + contactName);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                            contactEmail = subField.getValue();
                            logger.fine("contactEmail: " + contactEmail);
                        }
                    }
                    if (contactEmail != null) {
                        DvObjectContact datasetContact = new DvObjectContact(contactName, contactEmail);
                        datasetContacts.add(datasetContact);
                    } else {
                        logger.warning("email missing for contact in dataset " + dataset.getIdentifier());
                    }
                }

            }
        }
        return datasetContacts;
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
    private static String getGreeting(DvObjectContact dvObjectContact) {
        logger.fine("dvObjectContact: " + dvObjectContact);
        try {
            String name = dvObjectContact.getName();
            logger.fine("dvObjectContact name: " + name);
            String lastFirstString = dvObjectContact.getName();
            String[] lastFirstParts = lastFirstString.split(",");
            String last = lastFirstParts[0];
            String first = lastFirstParts[1];
            return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.helloFirstLast", Arrays.asList(first.trim(), last.trim()));
        } catch (Exception ex) {
            logger.warning("problem in getGreeting: " + ex);
            return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.organization");
        }
    }

}
