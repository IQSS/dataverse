package edu.harvard.iq.dataverse.mail;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;
import org.simplejavamail.email.Recipient;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.persistence.user.NotificationType.ASSIGNROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CHECKSUMFAIL;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CHECKSUMIMPORT;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CONFIRMEMAIL;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEACC;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.CREATEDV;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.FILESYSTEMIMPORT;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.GRANTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.MAPLAYERDELETEFAILED;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.MAPLAYERUPDATED;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.PUBLISHEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REJECTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REQUESTFILEACCESS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.RETURNEDDS;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.REVOKEROLE;
import static edu.harvard.iq.dataverse.persistence.user.NotificationType.SUBMITTEDDS;

/**
 * Class takes care of creating text templates for emails.
 */
@Stateless
public class MailMessageCreator {

    private SystemConfig systemConfig;

    private PermissionServiceBean permissionService;

    private DataverseDao dataverseDao;

    private ConfirmEmailServiceBean confirmEmailService;

    private GenericDao genericDao;

    private static final Logger logger = Logger.getLogger(MailMessageCreator.class.getCanonicalName());

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated /* JEE requirement */
    public MailMessageCreator() {
    }

    @Inject
    public MailMessageCreator(SystemConfig systemConfig, PermissionServiceBean permissionService,
                              DataverseDao dataverseDao, ConfirmEmailServiceBean confirmEmailService,
                              GenericDao genericDao) {
        this.systemConfig = systemConfig;
        this.permissionService = permissionService;
        this.dataverseDao = dataverseDao;
        this.confirmEmailService = confirmEmailService;
        this.genericDao = genericDao;
    }

    // -------------------- LOGIC --------------------

    /**
     * Creates footer for email message.
     */
    public String createMailFooterMessage(Locale messageLocale, String rootDataverseName, InternetAddress systemAddress) {

        return BundleUtil.getStringFromBundle("notification.email.closing",
                                                            messageLocale,
                                                            Arrays.asList(BrandingUtil.getSupportTeamEmailAddress(
                                                                    systemAddress),
                                                                          BrandingUtil.getSupportTeamName(systemAddress,
                                                                                                          rootDataverseName,
                                                                                                        messageLocale)));
    }

    /**
     * Divides recipientsEmails into multiple recipients with their own emails.
     *
     * @param recipientsEmails - comma separated emails.
     * @param recipientsName   - common name for all recipients, usually it is blank since it is only visible in email header.
     */
    public List<Recipient> createRecipients(String recipientsEmails, String recipientsName) {
        return Arrays.stream(recipientsEmails.split(","))
                .map(recipient -> new Recipient(recipientsName, recipient, Message.RecipientType.TO))
                .collect(Collectors.toList());
    }

    /**
     * Retrives message and subject template based on {@link NotificationObjectType} and {@link NotificationType}.
     *
     * @return message and subject or blank tuple if notificationType didn't match any template.
     */
    public Tuple2<String, String> getMessageAndSubject(EmailNotificationDto notificationDto, String systemEmail) {
        Lazy<String> rootDataverseName = Lazy.of(() -> dataverseDao.findRootDataverse().getName());

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATAVERSE) {
            Dataverse dataverse = dataverseDao.find(notificationDto.getDvObjectId());
            String message = dataverseMessage(notificationDto, dataverse);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATASET) {
            Dataset dataset = genericDao.find(notificationDto.getDvObjectId(), Dataset.class);
            String message = datasetMessage(notificationDto, dataset);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATASET_VERSION) {
            DatasetVersion datasetVersion = genericDao.find(notificationDto.getDvObjectId(), DatasetVersion.class);
            String message = datasetVersionMessage(notificationDto, datasetVersion);

            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return subject.isEmpty() ?
                    Tuple.of(message,
                             getSubjectTextForDatasetVersion(notificationDto,
                                                             rootDataverseName.get(),
                                                             datasetVersion)) :
                    Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.AUTHENTICATED_USER) {

            String message = authenticatedUserMessage(notificationDto,
                                                      rootDataverseName.get(),
                                                      MailUtil.parseSystemAddress(systemEmail));
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.FILEMETADATA) {
            FileMetadata fileMetadata = genericDao.find(notificationDto.getDvObjectId(), FileMetadata.class);
            String message = fileMetadataMessage(notificationDto, fileMetadata);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        return Tuple.of(StringUtils.EMPTY, StringUtils.EMPTY);
    }

    /**
     * Retrives message and subject template based on {@link NotificationObjectType} and {@link NotificationType} which requires requester.
     *
     * @return message and subject or blank tuple if notificationType didn't match any template.
     */
    public Tuple2<String, String> getMessageAndSubject(EmailNotificationDto notificationDto, AuthenticatedUser requester) {
        Lazy<String> rootDataverseName = Lazy.of(() -> dataverseDao.findRootDataverse().getName());

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATASET_VERSION) {
            DatasetVersion datasetVersion = genericDao.find(notificationDto.getDvObjectId(), DatasetVersion.class);
            String message = datasetVersionMessage(notificationDto, datasetVersion, requester);

            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return subject.isEmpty() ?
                    Tuple.of(message,
                             getSubjectTextForDatasetVersion(notificationDto,
                                                             rootDataverseName.get(),
                                                             datasetVersion)) :
                    Tuple.of(message, subject);
        }

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATAFILE) {
            DataFile dataFile = genericDao.find(notificationDto.getDvObjectId(), DataFile.class);
            String message = dataFileMessage(notificationDto, dataFile, requester);
            String subject = getSubjectText(notificationDto, rootDataverseName.get());

            return Tuple.of(message, subject);
        }

        return Tuple.of(StringUtils.EMPTY, StringUtils.EMPTY);
    }

    // -------------------- PRIVATE --------------------

    private String dataverseMessage(EmailNotificationDto notificationDto, Dataverse dataverse) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting",
                notificationsEmailLanguage);
        String objectType = NotificationObjectType.DATAVERSE.toString().toLowerCase();

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:

                String joinedRoleNames = permissionService.getRolesOfUser(notificationDto.getNotificationReceiver(), dataverse).stream()
                        .map(roleAssignment -> roleAssignment.getRole().getAlias())
                        .collect(Collectors.joining("/"));

                String pattern = BundleUtil.getStringFromBundle("notification.email.assignRole",
                        notificationsEmailLanguage);

                messageText += MessageFormat.format(pattern,
                                                    joinedRoleNames,
                                                    objectType,
                                                    dataverse.getDisplayName(),
                                                    getDataverseLink(dataverse));

                if (joinedRoleNames.contains("fileDownloader")) {
                    pattern = BundleUtil.getStringFromBundle(
                            "notification.access.granted.fileDownloader.additionalDataverse",
                            notificationsEmailLanguage);
                    messageText += MessageFormat.format(pattern, " ");
                }

                return messageText;
            case REVOKEROLE:
                messageText += MessageFormat.format(BundleUtil.getStringFromBundle("notification.email.revokeRole",
                        notificationsEmailLanguage),
                                                    objectType,
                                                    dataverse.getDisplayName(),
                                                    getDataverseLink(dataverse));
                return messageText;
            case CREATEDV:
                Dataverse parentDataverse = dataverse.getOwner();

                String dataverseCreatedMessage = BundleUtil.getStringFromBundle("notification.email.createDataverse",
                        notificationsEmailLanguage,
                                                                                Arrays.asList(
                                                                                        dataverse.getDisplayName(),
                                                                                        getDataverseLink(dataverse),
                                                                                        parentDataverse != null ? parentDataverse.getDisplayName() : "",
                                                                                        parentDataverse != null ? getDataverseLink(
                                                                                                parentDataverse) : "",
                                                                                        systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                                                                                        systemConfig.getGuidesVersion()));

                logger.fine(dataverseCreatedMessage);
                return messageText + dataverseCreatedMessage;
        }

        return StringUtils.EMPTY;
    }

    private String datasetMessage(EmailNotificationDto notificationDto, Dataset dataset) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting",
                notificationsEmailLanguage);
        String objectType = notificationDto.getNotificationObjectType().toString().toLowerCase();
        String pattern;

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:

                String joinedRoleNames = permissionService.getRolesOfUser(notificationDto.getNotificationReceiver(), dataset).stream()
                        .map(roleAssignment -> roleAssignment.getRole().getAlias())
                        .collect(Collectors.joining("/"));

                pattern = BundleUtil.getStringFromBundle("notification.email.assignRole",
                        notificationsEmailLanguage);

                messageText += MessageFormat.format(pattern,
                                                    joinedRoleNames,
                                                    objectType,
                                                    dataset.getDisplayName(),
                                                    getDatasetLink(dataset));

                if (joinedRoleNames.contains("File Downloader")) {
                    pattern = BundleUtil.getStringFromBundle(
                            "notification.access.granted.fileDownloader.additionalDataverse",
                            notificationsEmailLanguage);
                    messageText += MessageFormat.format(pattern, " ");
                }

                return messageText;
            case GRANTFILEACCESS:
                pattern = BundleUtil.getStringFromBundle("notification.email.grantFileAccess",
                        notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                                                    dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case REJECTFILEACCESS:
                pattern = BundleUtil.getStringFromBundle("notification.email.rejectFileAccess",
                        notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                                                    dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case CHECKSUMFAIL:
                String checksumFailMsg = BundleUtil.getStringFromBundle("notification.checksumfail",
                        notificationsEmailLanguage,
                                                                        Collections.singletonList(
                                                                                dataset.getGlobalIdString()
                                                                        ));
                logger.fine("checksumFailMsg: " + checksumFailMsg);
                return messageText + checksumFailMsg;
        }

        return StringUtils.EMPTY;
    }

    private String datasetVersionMessage(EmailNotificationDto notificationDto, DatasetVersion version, AuthenticatedUser requestor) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(SUBMITTEDDS)) {

            String requestorName = requestor.getFirstName() + " " + requestor.getLastName();

            String requestorEmail = requestor.getEmail();

            String pattern = BundleUtil.getStringFromBundle("notification.email.wasSubmittedForReview",
                    notificationsEmailLanguage);

            pattern += addUserCustomMessage(notificationDto,
                    BundleUtil.getStringFromBundle("dataset.reject.messageBox.label", notificationsEmailLanguage));

            messageText += MessageFormat.format(pattern,
                                                version.getDataset().getDisplayName(),
                                                getDatasetDraftLink(version.getDataset()),
                                                version.getDataset().getOwner().getDisplayName(),
                                                getDataverseLink(version.getDataset().getOwner()),
                                                requestorName,
                                                requestorEmail);
            return messageText;
        }

        return StringUtils.EMPTY;
    }

    private String datasetVersionMessage(EmailNotificationDto notificationDto, DatasetVersion version) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting", notificationsEmailLanguage);
        String pattern;

        switch (notificationDto.getNotificationType()) {
            case CREATEDS:
                String datasetCreatedMessage = BundleUtil.getStringFromBundle("notification.email.createDataset",
                        notificationsEmailLanguage,
                                                                              Arrays.asList(
                                                                                      version.getDataset().getDisplayName(),
                                                                                      getDatasetLink(version.getDataset()),
                                                                                      version.getDataset().getOwner().getDisplayName(),
                                                                                      getDataverseLink(version.getDataset().getOwner()),
                                                                                      systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                                                                                      systemConfig.getGuidesVersion()
                                                                              ));

                return messageText + datasetCreatedMessage;
            case MAPLAYERUPDATED:
                pattern = BundleUtil.getStringFromBundle("notification.email.worldMap.added",
                        notificationsEmailLanguage);

                messageText += MessageFormat.format(pattern,
                                                    version.getDataset().getDisplayName(),
                                                    getDatasetLink(version.getDataset()));
                return messageText;
            case PUBLISHEDDS:
                pattern = BundleUtil.getStringFromBundle("notification.email.wasPublished",
                        notificationsEmailLanguage);

                messageText += MessageFormat.format(pattern,
                                                    version.getDataset().getDisplayName(),
                                                    getDatasetLink(version.getDataset()),
                                                    version.getDataset().getOwner().getDisplayName(),
                                                    getDataverseLink(version.getDataset().getOwner()));
                return messageText;
            case SUBMITTEDDS:
                pattern = BundleUtil.getStringFromBundle("notification.email.wasSubmittedForReview",
                        notificationsEmailLanguage);

                pattern += addUserCustomMessage(notificationDto,
                        BundleUtil.getStringFromBundle("dataset.reject.messageBox.label", notificationsEmailLanguage));

                messageText += MessageFormat.format(pattern,
                        version.getDataset().getDisplayName(),
                        getDatasetDraftLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()),
                        "",
                        "");
                return messageText;
            case RETURNEDDS:
                pattern = BundleUtil.getStringFromBundle("notification.email.wasReturnedByReviewer",
                        notificationsEmailLanguage);

                pattern += addUserCustomMessage(notificationDto,
                        BundleUtil.getStringFromBundle("dataset.reject.messageBox.label", notificationsEmailLanguage));

                messageText += MessageFormat.format(pattern,
                                                    version.getDataset().getDisplayName(),
                                                    getDatasetDraftLink(version.getDataset()),
                                                    version.getDataset().getOwner().getDisplayName(),
                                                    getDataverseLink(version.getDataset().getOwner()),
                                                    "");
                return messageText;
            case FILESYSTEMIMPORT:

                String fileImportMsg = BundleUtil.getStringFromBundle("notification.mail.import.filesystem",
                        notificationsEmailLanguage,
                                                                      Arrays.asList(
                                                                              systemConfig.getDataverseSiteUrl(),
                                                                              version.getDataset().getGlobalIdString(),
                                                                              version.getDataset().getDisplayName()
                                                                      ));

                return messageText + fileImportMsg;

            case CHECKSUMIMPORT:

                String checksumImportMsg = BundleUtil.getStringFromBundle("notification.import.checksum",
                        notificationsEmailLanguage,
                                                                          Arrays.asList(
                                                                                  version.getDataset().getGlobalIdString(),
                                                                                  version.getDataset().getDisplayName()
                                                                          ));

                return messageText + checksumImportMsg;
        }
        return StringUtils.EMPTY;
    }

    private String addUserCustomMessage(EmailNotificationDto notificationDto, String messagePrefix) {
        if(StringUtils.isNotEmpty(notificationDto.getCustomUserMessage())) {
            return "\n\n"
                    + messagePrefix
                    + "\n\n"
                    + notificationDto.getCustomUserMessage();
        }
         return StringUtils.EMPTY;
    }

    private String dataFileMessage(EmailNotificationDto notificationDto, DataFile dataFile, AuthenticatedUser requestor) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(REQUESTFILEACCESS)) {

            String pattern = BundleUtil.getStringFromBundle("notification.email.requestFileAccess",
                    notificationsEmailLanguage);

            String requestorName = requestor.getFirstName() + " " + requestor.getLastName();

            String requestorEmail = requestor.getEmail();

            messageText += MessageFormat.format(pattern,
                                                dataFile.getOwner().getDisplayName(), requestorName,
                                                requestorEmail, getDatasetManageFileAccessLink(dataFile));
            return messageText;
        }
        return StringUtils.EMPTY;
    }

    private String fileMetadataMessage(EmailNotificationDto notificationDto, FileMetadata fileMetadata) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(MAPLAYERDELETEFAILED)) {

            DatasetVersion version = fileMetadata.getDatasetVersion();
            String pattern = BundleUtil.getStringFromBundle("notification.email.maplayer.deletefailed.text",
                    notificationsEmailLanguage);

            messageText += MessageFormat.format(pattern,
                                                fileMetadata.getLabel(), getDatasetLink(version.getDataset()));
            return messageText;
        }
        return StringUtils.EMPTY;
    }

    private String authenticatedUserMessage(EmailNotificationDto notificationDto, String rootDataverseName, InternetAddress systemAddress) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(CREATEACC)) {

            String accountCreatedMessage = BundleUtil.getStringFromBundle("notification.email.welcome", notificationsEmailLanguage,
                                                                          Arrays.asList(
                                                                                  rootDataverseName,
                                                                                  systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                                                                                  systemConfig.getGuidesVersion(),
                                                                                  BrandingUtil.getSupportTeamName(
                                                                                          systemAddress,
                                                                                          rootDataverseName,
                                                                                          notificationsEmailLanguage),
                                                                                  BrandingUtil.getSupportTeamEmailAddress(
                                                                                          systemAddress)
                                                                          ));
            String optionalConfirmEmailAddon = confirmEmailService.optionalConfirmEmailAddonMsg(notificationDto.getNotificationReceiver());
            accountCreatedMessage += optionalConfirmEmailAddon;
            logger.fine("accountCreatedMessage: " + accountCreatedMessage);
            return messageText + accountCreatedMessage;
        }

        return StringUtils.EMPTY;
    }

    private String getSubjectText(EmailNotificationDto notificationDto, String rootDataverseName) {
        List<String> rootDvNameAsList = Collections.singletonList(rootDataverseName);
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:
                return BundleUtil.getStringFromBundle("notification.email.assign.role.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case REVOKEROLE:
                return BundleUtil.getStringFromBundle("notification.email.revoke.role.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case CREATEDV:
                return BundleUtil.getStringFromBundle("notification.email.create.dataverse.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case REQUESTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.request.file.access.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case GRANTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.grant.file.access.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case REJECTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.rejected.file.access.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case MAPLAYERUPDATED:
                return BundleUtil.getStringFromBundle("notification.email.update.maplayer",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case MAPLAYERDELETEFAILED:
                return BundleUtil.getStringFromBundle("notification.email.maplayer.deletefailed.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case CREATEDS:
                return BundleUtil.getStringFromBundle("notification.email.create.dataset.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case SUBMITTEDDS:
                return BundleUtil.getStringFromBundle("notification.email.submit.dataset.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case PUBLISHEDDS:
                return BundleUtil.getStringFromBundle("notification.email.publish.dataset.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case RETURNEDDS:
                return BundleUtil.getStringFromBundle("notification.email.returned.dataset.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case CREATEACC:
                return BundleUtil.getStringFromBundle("notification.email.create.account.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case CHECKSUMFAIL:
                return BundleUtil.getStringFromBundle("notification.email.checksumfail.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case CHECKSUMIMPORT:
                return BundleUtil.getStringFromBundle("notification.email.import.checksum.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
            case CONFIRMEMAIL:
                return BundleUtil.getStringFromBundle("notification.email.verifyEmail.subject",
                                                      notificationsEmailLanguage,
                                                      rootDvNameAsList);
        }
        return StringUtils.EMPTY;
    }

    private String getSubjectTextForDatasetVersion(EmailNotificationDto notificationDto, String rootDataverseName, DatasetVersion datasetVersion) {
        if (notificationDto.getNotificationType().equals(FILESYSTEMIMPORT)) {
            Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
            try {
                List<String> dsNameAsList = Collections.singletonList(datasetVersion.getDataset().getDisplayName());
                return BundleUtil.getStringFromBundle("notification.email.import.filesystem.subject",
                        notificationsEmailLanguage,
                                                      dsNameAsList);
            } catch (Exception e) {
                return BundleUtil.getStringFromBundle("notification.email.import.filesystem.subject",
                        notificationsEmailLanguage,
                                                      Collections.singletonList(rootDataverseName));
            }
        }

        return StringUtils.EMPTY;
    }

    private String getDatasetManageFileAccessLink(DataFile datafile) {
        return systemConfig.getDataverseSiteUrl() + "/permissions-manage-files.xhtml?id=" + datafile.getOwner().getId();
    }

    private String getDatasetLink(Dataset dataset) {
        return systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString();
    }

    private String getDatasetDraftLink(Dataset dataset) {
        return systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=DRAFT" + "&faces-redirect=true";
    }

    private String getDataverseLink(Dataverse dataverse) {
        return systemConfig.getDataverseSiteUrl() + "/dataverse/" + dataverse.getAlias();
    }

}
