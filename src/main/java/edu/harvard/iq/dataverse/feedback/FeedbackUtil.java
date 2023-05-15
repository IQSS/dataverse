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
import edu.harvard.iq.dataverse.util.PersonOrOrgUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.mail.internet.InternetAddress;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    private static final String NO_DATASET_CONTACT_INTRO = BundleUtil.getStringFromBundle("contact.context.dataset.noContact");

    public static Feedback gatherFeedback(DvObject feedbackTarget, DataverseSession dataverseSession, String messageSubject, String userMessage, InternetAddress systemAddress, String userEmail, String dataverseSiteUrl, String installationBrandName, String supportTeamName, boolean ccSupport) {
        String systemEmail = null;
        if (systemAddress != null) {
            systemEmail = systemAddress.getAddress();
        }
        logger.fine("systemAddress: " + systemAddress);
        Feedback feedback = null;
        if (isLoggedIn(dataverseSession)) {
            userEmail = loggedInUserEmail(dataverseSession);
        }
        String contextIntro;
        String contextEnding;
        String contactEmails;
        String ccEmails = ccSupport ? systemEmail : null;

        if (feedbackTarget != null) {
            messageSubject = BundleUtil.getStringFromBundle("contact.context.subject.dvobject", Arrays.asList(installationBrandName, messageSubject));

            String contactGreeting;

            if (feedbackTarget.isInstanceofDataverse()) {
                // Dataverse target
                Dataverse dataverse = (Dataverse) feedbackTarget;
                contextEnding = BundleUtil.getStringFromBundle("contact.context.dataverse.ending", Arrays.asList(supportTeamName, systemEmail, dataverseSiteUrl, dataverse.getAlias(), supportTeamName, systemEmail));
                List<DvObjectContact> contacts = getDataverseContacts(dataverse);
                List<String> contactEmailList = new ArrayList<String>();
                for (DvObjectContact contact : contacts) {
                    contactEmailList.add(contact.getEmail());
                }
                if (!contactEmailList.isEmpty()) {
                    contactEmails = String.join(",", contactEmailList);
                    // Dataverse contacts do not have a name, just email address
                    contactGreeting = "";
                    contextIntro = BundleUtil.getStringFromBundle("contact.context.dataverse.intro", Arrays.asList(contactGreeting, userEmail, installationBrandName, dataverse.getAlias()));
                } else {
                    // No contacts
                    contextIntro = BundleUtil.getStringFromBundle("contact.context.dataverse.noContact");
                    contactEmails = systemEmail;
                    ccEmails = null;
                }
            } else if (feedbackTarget.isInstanceofDataset()) {
                // Dataset target
                Dataset dataset = (Dataset) feedbackTarget;
                String datasetTitle = dataset.getLatestVersion().getTitle();
                String datasetPid = dataset.getGlobalId().asString();
                contextEnding = BundleUtil.getStringFromBundle("contact.context.dataset.ending", Arrays.asList(supportTeamName, systemEmail, dataverseSiteUrl, datasetPid, supportTeamName, systemEmail));
                List<DvObjectContact> contacts = getDatasetContacts(dataset);
                List<String> contactEmailList = new ArrayList<String>();
                List<String> contactNameList = new ArrayList<String>();

                for (DvObjectContact contact : contacts) {
                    String name = getContactName(contact);
                    if (name != null) {
                        contactNameList.add(name);
                    }
                    contactEmailList.add(contact.getEmail());
                }
                if (!contactEmailList.isEmpty()) {
                    contactEmails = String.join(",", contactEmailList);
                    contactGreeting = getGreeting(contactNameList);

                    contextIntro = BundleUtil.getStringFromBundle("contact.context.dataset.intro", Arrays.asList(contactGreeting, userEmail, installationBrandName, datasetTitle, datasetPid));
                } else {
                    // No contacts
                    // TODO: Add more of an intro for the person receiving the system email in this
                    // "no dataset contact" scenario?
                    contextIntro = NO_DATASET_CONTACT_INTRO;
                    contactEmails = systemEmail;
                    ccEmails = null;
                }
            } else {
                // DataFile target
                DataFile datafile = (DataFile) feedbackTarget;
                String datasetTitle = datafile.getOwner().getLatestVersion().getTitle();
                String datasetPid = datafile.getOwner().getGlobalId().asString();
                String filename = datafile.getFileMetadatas().get(0).getLabel();
                List<DvObjectContact> contacts = getDatasetContacts(datafile.getOwner());
                contextEnding = BundleUtil.getStringFromBundle("contact.context.file.ending", Arrays.asList(supportTeamName, systemEmail, dataverseSiteUrl, datafile.getId().toString(), supportTeamName, systemEmail));
                List<String> contactEmailList = new ArrayList<String>();
                List<String> contactNameList = new ArrayList<String>();

                for (DvObjectContact contact : contacts) {
                    String name = getContactName(contact);
                    if (name != null) {
                        contactNameList.add(name);
                    }
                    contactEmailList.add(contact.getEmail());
                }
                if (!contactEmailList.isEmpty()) {
                    contactEmails = String.join(",", contactEmailList);
                    contactGreeting = getGreeting(contactNameList);

                    contextIntro = BundleUtil.getStringFromBundle("contact.context.file.intro", Arrays.asList(contactGreeting, userEmail, installationBrandName, filename, datasetTitle, datasetPid));
                } else {
                    // No contacts
                    // TODO: Add more of an intro for the person receiving the system email in this
                    // "no dataset contact" scenario?
                    contextIntro = NO_DATASET_CONTACT_INTRO;
                    contactEmails = systemEmail;
                    ccEmails = null;
                }
            }
        } else {
            // No target
            messageSubject = BundleUtil.getStringFromBundle("contact.context.subject.support", Arrays.asList(installationBrandName, messageSubject));
            contextIntro = BundleUtil.getStringFromBundle("contact.context.support.intro", Arrays.asList(supportTeamName, userEmail));
            contextEnding = BundleUtil.getStringFromBundle("contact.context.support.ending", Arrays.asList(""));
            contactEmails = systemEmail;
            ccEmails = null;
        }
        feedback = new Feedback(userEmail, contactEmails, ccEmails, messageSubject, contextIntro + userMessage + contextEnding);
        return feedback;
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
     * When contacts are people we suggest that they be stored as "Simpson, Homer"
     * so the idea of this method is that it returns "Homer Simpson", if it can.
     *
     * Contacts don't necessarily have to be people, however. They can be
     * organizations. This method uses the algorithm to detect whether an entry is a
     * Person or Organization and relies on it to create a full name, i.e. removing
     * the comma and reversion the order of names for a Person but not changing the
     * string for an Organization.
     */
    private static String getContactName(DvObjectContact dvObjectContact) {
        String contactName = dvObjectContact.getName();
        String name = null;
        if (contactName != null) {
            JsonObject entity = PersonOrOrgUtil.getPersonOrOrganization(contactName, false, false);
            if (entity.getBoolean("isPerson") && entity.containsKey("givenName") && entity.containsKey("familyName")) {
                name = entity.getString("givenName") + " " + entity.getString("familyName");
            } else {
                name = entity.getString("fullName");
            }
        }
        return name;

    }

    /**
     * Concatenates names using commas and a final 'and' and creates the greeting
     * string, e.g. "Hello Homer Simpson, Bart Simson, and Marge Simpson"
     */
    private static String getGreeting(List<String> contactNameList) {
        int size = contactNameList.size();
        String nameString;
        String finalName = null;
        // Treat the final name separately
        switch (size) {
        case 0:
            return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.organization");
        case 1:
            nameString = contactNameList.get(0);
            break;
        case 2:
            nameString = contactNameList.get(0) + " and " + contactNameList.get(1);
            break;
        default:
            finalName = contactNameList.remove(size - 1);
            nameString = String.join(",", contactNameList) + ", and " + finalName;
        }
        return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.helloFirstLast", Arrays.asList(nameString));
    }

}
