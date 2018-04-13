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

    private static final String NO_DATASET_CONTACT_INTRO = BundleUtil.getStringFromBundle("contact.context.dataset.noContact");

    // TODO: consider changing "recipient" into an object called something like FeedbackTarget
    public static List<Feedback> gatherFeedback(DvObject recipient, DataverseSession dataverseSession, String messageSubject, String userMessage, InternetAddress systemAddress, String userEmail, String dataverseSiteUrl, String installationBrandName, String supportTeamName) {
        List<Feedback> feedbacks = new ArrayList<>();
        if (isLoggedIn(dataverseSession)) {
            userEmail = loggedInUserEmail(dataverseSession);
        }
        if (recipient != null) {
            if (recipient.isInstanceofDataverse()) {
                Dataverse dataverse = (Dataverse) recipient;
                String dataverseContextIntro = BundleUtil.getStringFromBundle("contact.context.dataverse.intro", Arrays.asList(dataverseSiteUrl, dataverse.getAlias()));
                String dataverseContextEnding = BundleUtil.getStringFromBundle("contact.context.dataverse.ending", Arrays.asList(""));
                List<DvObjectContact> dataverseContacts = getDataverseContacts(dataverse);
                for (DvObjectContact dataverseContact : dataverseContacts) {
                    Feedback feedback = new Feedback(userEmail, dataverseContact.getEmail(), messageSubject, dataverseContextIntro + userMessage + dataverseContextEnding);
                    feedbacks.add(feedback);
                }
                if (!feedbacks.isEmpty()) {
                    return feedbacks;
                } else {
                    String dataverseContextIntroError = BundleUtil.getStringFromBundle("contact.context.dataverse.noContact") + dataverseContextIntro;
                    Feedback feedback = new Feedback(userEmail, systemAddress.getAddress(), messageSubject, dataverseContextIntroError + userMessage + dataverseContextEnding);
                    feedbacks.add(feedback);
                    return feedbacks;
                }
            } else if (recipient.isInstanceofDataset()) {
                Dataset dataset = (Dataset) recipient;
                String datasetTitle = dataset.getLatestVersion().getTitle();
                String datasetPid = dataset.getGlobalId();
                String datasetContextEnding = BundleUtil.getStringFromBundle("contact.context.dataset.ending", Arrays.asList(supportTeamName, systemAddress.getAddress(), dataverseSiteUrl, dataset.getGlobalId(), supportTeamName, systemAddress.getAddress()));
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
                    Feedback feedback = new Feedback(userEmail, systemAddress.getAddress(), messageSubject, NO_DATASET_CONTACT_INTRO + userMessage + datasetContextEnding);
                    feedbacks.add(feedback);
                    return feedbacks;
                }
            } else {
                DataFile datafile = (DataFile) recipient;
                List<DvObjectContact> datasetContacts = getDatasetContacts(datafile.getOwner());
                String fileContextIntro = BundleUtil.getStringFromBundle("contact.context.file.intro", Arrays.asList(dataverseSiteUrl, datafile.getId().toString()));
                String fileContextEnding = BundleUtil.getStringFromBundle("contact.context.file.ending", Arrays.asList(""));
                for (DvObjectContact datasetContact : datasetContacts) {
                    Feedback feedback = new Feedback(userEmail, datasetContact.getEmail(), messageSubject, fileContextIntro + userMessage + fileContextEnding);
                    feedbacks.add(feedback);
                }
                if (!feedbacks.isEmpty()) {
                    return feedbacks;
                } else {
                    String datasetContextEnding = "";
                    Feedback feedback = new Feedback(userEmail, systemAddress.getAddress(), messageSubject, NO_DATASET_CONTACT_INTRO + userMessage + datasetContextEnding);
                    feedbacks.add(feedback);
                    return feedbacks;
                }
            }
        } else {
            if (systemAddress != null) {
                String noDvObjectContextIntro = "";
                String noDvObjectContextEnding = "";
                Feedback feedback = new Feedback(userEmail, systemAddress.getAddress(), messageSubject, noDvObjectContextEnding + userMessage + noDvObjectContextIntro);
                feedbacks.add(feedback);
                return feedbacks;
            } else {
                return Collections.EMPTY_LIST;
            }
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
        List<DvObjectContact> retList = new ArrayList<>();
        for (DatasetField dsf : dataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact)) {
                String contactName = null;
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
            return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.helloFirstLast", Arrays.asList(first.trim(), last.trim()));
        } catch (Exception ex) {
            logger.warning("problem in getGreeting: " + ex);
            return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.organization");
        }
    }

}
