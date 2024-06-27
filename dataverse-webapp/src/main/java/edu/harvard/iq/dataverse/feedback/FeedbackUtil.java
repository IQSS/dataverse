package edu.harvard.iq.dataverse.feedback;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FeedbackUtil {

    private static final Logger logger = Logger.getLogger(FeedbackUtil.class.getCanonicalName());

    private static final String NO_DATASET_CONTACT_INTRO = BundleUtil.getStringFromBundle("contact.context.dataset.noContact");

    public static <T extends DvObject> List<Feedback> gatherFeedback(FeedbackInfo<T> feedbackInfo) {
        if (feedbackInfo.getFeedbackTarget() == null) {
            return Lists.newArrayList(getFeedbackToRepoSupport(feedbackInfo));
        }

        feedbackInfo.withMessageSubject(BundleUtil.getStringFromBundle("contact.context.subject.dvobject", feedbackInfo.getInstallationBrandName(), feedbackInfo.getMessageSubject()));

        if (feedbackInfo.getFeedbackTarget().isInstanceofDataverse()) {
            return getFeedbacksToDataverse((FeedbackInfo<Dataverse>) feedbackInfo);
        } else if (feedbackInfo.getFeedbackTarget().isInstanceofDataset()) {
            return getFeedbacksToDataset((FeedbackInfo<Dataset>) feedbackInfo);
        } else {
            return getFeedbacksToDataFile((FeedbackInfo<DataFile>) feedbackInfo);
        }
    }

    private static List<Feedback> getFeedbacksToDataverse(FeedbackInfo<Dataverse> feedbackInfo) {
        List<FeedbackContact> dataverseContacts = resolveContactsForDataverse(feedbackInfo);

        String dataverseContextEnding = BundleUtil.getStringFromBundle("contact.context.dataverse.ending",
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail(),
                feedbackInfo.getDataverseSiteUrl(),
                feedbackInfo.getFeedbackTarget().getAlias(),
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail());

        if (dataverseContacts.isEmpty()) {
            String dataverseContextIntroError = BundleUtil.getStringFromBundle("contact.context.dataverse.noContact");
            return Lists.newArrayList(new Feedback(feedbackInfo.getUserEmail(), feedbackInfo.getSystemEmail(),
                    feedbackInfo.getMessageSubject(),
                    dataverseContextIntroError + feedbackInfo.getUserMessage() + dataverseContextEnding));
        }

        String placeHolderIfDataverseContactsGetNames = "";
        String dataverseContextIntro = BundleUtil.getStringFromBundle("contact.context.dataverse.intro",
                placeHolderIfDataverseContactsGetNames,
                feedbackInfo.getUserEmail(),
                feedbackInfo.getInstallationBrandName(),
                feedbackInfo.getFeedbackTarget().getAlias());
        String body = dataverseContextIntro + feedbackInfo.getUserMessage() + dataverseContextEnding;

        return dataverseContacts.stream()
                .map(dataverseContact ->
                        new Feedback(feedbackInfo.getUserEmail(), dataverseContact.getEmail(), feedbackInfo.getMessageSubject(), body))
                .collect(Collectors.toList());
    }

    private static List<Feedback> getFeedbacksToDataset(FeedbackInfo<Dataset> feedbackInfo) {
        List<FeedbackContact> recipients = resolveContactsForDataset(feedbackInfo);
        String datasetPid = feedbackInfo.getFeedbackTarget().getGlobalIdString();

        String datasetContextEnding = BundleUtil.getStringFromBundle("contact.context.dataset.ending",
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail(),
                feedbackInfo.getDataverseSiteUrl(),
                datasetPid,
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail());

        if (recipients.isEmpty()) {
            return Lists.newArrayList(new Feedback(feedbackInfo.getUserEmail(), feedbackInfo.getSystemEmail(), feedbackInfo.getMessageSubject(),
                    NO_DATASET_CONTACT_INTRO + feedbackInfo.getUserMessage() + datasetContextEnding));
        }

        String datasetTitle = feedbackInfo.getFeedbackTarget().getLatestVersion().getParsedTitle();

        return recipients.stream().map(datasetContact -> {
            String datasetContextIntro = BundleUtil.getStringFromBundle("contact.context.dataset.intro",
                    getGreeting(datasetContact),
                    feedbackInfo.getUserEmail(),
                    feedbackInfo.getInstallationBrandName(),
                    datasetTitle,
                    datasetPid);
            String body = datasetContextIntro + feedbackInfo.getUserMessage() + datasetContextEnding;
            return new Feedback(feedbackInfo.getUserEmail(), datasetContact.getEmail(), feedbackInfo.getMessageSubject(), body);
        }).collect(Collectors.toList());
    }

    private static List<Feedback> getFeedbacksToDataFile(FeedbackInfo<DataFile> feedbackInfo) {
        List<FeedbackContact> datasetContacts = resolveContactsForDataFile(feedbackInfo);

        DataFile dataFile = feedbackInfo.getFeedbackTarget();
        String fileContextEnding = BundleUtil.getStringFromBundle("contact.context.file.ending",
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail(),
                feedbackInfo.getDataverseSiteUrl(),
                dataFile.getId().toString(),
                feedbackInfo.getSupportTeamName(),
                feedbackInfo.getSystemEmail());

        if (datasetContacts.isEmpty()) {
            return Lists.newArrayList(new Feedback(feedbackInfo.getUserEmail(), feedbackInfo.getSystemEmail(),
                    feedbackInfo.getMessageSubject(), NO_DATASET_CONTACT_INTRO + feedbackInfo.getUserMessage() + fileContextEnding));
        }

        String filename = dataFile.getFileMetadatas().get(0).getLabel();
        String datasetTitle = dataFile.getOwner().getLatestVersion().getParsedTitle();
        String datasetPid = dataFile.getOwner().getGlobalIdString();

        return datasetContacts.stream().map(datasetContact -> {
            String fileContextIntro = BundleUtil.getStringFromBundle("contact.context.file.intro",
                    getGreeting(datasetContact),
                    feedbackInfo.getUserEmail(),
                    feedbackInfo.getInstallationBrandName(),
                    filename,
                    datasetTitle,
                    datasetPid);
            String body = fileContextIntro + feedbackInfo.getUserMessage() + fileContextEnding;

            return new Feedback(feedbackInfo.getUserEmail(), datasetContact.getEmail(), feedbackInfo.getMessageSubject(), body);
        }).collect(Collectors.toList());
    }

    private static Feedback getFeedbackToRepoSupport(FeedbackInfo<?> feedbackInfo) {
        String noDvObjectContextIntro = BundleUtil.getStringFromBundle("contact.context.support.intro", feedbackInfo.getSupportTeamName(), feedbackInfo.getUserEmail());
        String noDvObjectContextEnding = BundleUtil.getStringFromBundle("contact.context.support.ending", "");
        return new Feedback(
                feedbackInfo.getUserEmail(),
                feedbackInfo.getSystemEmail(),
                BundleUtil.getStringFromBundle("contact.context.subject.support", feedbackInfo.getInstallationBrandName(), feedbackInfo.getMessageSubject()),
                noDvObjectContextIntro + feedbackInfo.getUserMessage() + noDvObjectContextEnding);
    }

    public static List<FeedbackContact> resolveContactsForDataverse(FeedbackInfo<Dataverse> feedbackInfo) {
        if (feedbackInfo.getRecipient() == null || feedbackInfo.getRecipient() == FeedbackRecipient.DATAVERSE_CONTACT) {
            return FeedbackContact.fromDataverse(feedbackInfo.getFeedbackTarget());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.SYSTEM_SUPPORT) {
            return Lists.newArrayList(new FeedbackContact(feedbackInfo.getSystemEmail()));
        }

        return Collections.emptyList();
    }

    public static List<FeedbackContact> resolveContactsForDataset(FeedbackInfo<Dataset> feedbackInfo) {
        if (feedbackInfo.getRecipient() == null || feedbackInfo.getRecipient() == FeedbackRecipient.DATASET_CONTACT) {
            return FeedbackContact.fromDataset(feedbackInfo.getFeedbackTarget());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.DATAVERSE_CONTACT) {
            return FeedbackContact.fromDataverse(feedbackInfo.getFeedbackTarget().getOwner());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.SYSTEM_SUPPORT) {
            return Lists.newArrayList(new FeedbackContact(feedbackInfo.getSystemEmail()));
        }

        return Collections.emptyList();
    }

    public static List<FeedbackContact> resolveContactsForDataFile(FeedbackInfo<DataFile> feedbackInfo) {
        if (feedbackInfo.getRecipient() == null || feedbackInfo.getRecipient() == FeedbackRecipient.DATASET_CONTACT) {
            return FeedbackContact.fromDataset(feedbackInfo.getFeedbackTarget().getOwner());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.DATAVERSE_CONTACT) {
            return FeedbackContact.fromDataverse(feedbackInfo.getFeedbackTarget().getOwner().getOwner());
        } else if (feedbackInfo.getRecipient() == FeedbackRecipient.SYSTEM_SUPPORT) {
            return Lists.newArrayList(new FeedbackContact(feedbackInfo.getSystemEmail()));
        }

        return Collections.emptyList();
    }


    /**
     * When contacts are people we suggest that they be stored as "Simpson,
     * Homer" so the idea of this method is that it returns "Homer Simpson", if
     * it can.
     * <p>
     * Contacts don't necessarily have to be people, however. They can be
     * organizations. We ran into similar trouble (but for authors) when
     * implementing Schema.org JSON-LD support. See getJsonLd on DatasetVersion.
     * Some day it might be nice to store whether an author or a contact is a
     * person or an organization.
     */
    private static String getGreeting(FeedbackContact dvObjectContact) {
        logger.fine("dvObjectContact: " + dvObjectContact);
        try {
            return dvObjectContact.getName().map(name -> {
                logger.fine("dvObjectContact name: " + name);
                String[] lastFirstParts = name.split(",");
                String last = lastFirstParts[0];
                String first = lastFirstParts[1];
                return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.helloFirstLast", first.trim(), last.trim());
            }).getOrElse(() -> BundleUtil.getStringFromBundle("contact.context.dataset.greeting.organization"));
        } catch (Exception ex) {
            logger.warning("problem in getGreeting: " + ex);
            return BundleUtil.getStringFromBundle("contact.context.dataset.greeting.organization");
        }
    }
}
