package edu.harvard.iq.dataverse.mail;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.persistence.user.NotificationType.*;

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

        return BundleUtil.getStringFromBundleWithLocale("notification.email.closing", messageLocale,
                        BrandingUtil.getSupportTeamEmailAddress(systemAddress),
                        BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName, messageLocale));
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

        if (notificationDto.getNotificationObjectType() == NotificationObjectType.DATAFILE) {
            DataFile dataFile = genericDao.find(notificationDto.getDvObjectId(), DataFile.class);
            String message = dataFileMessage(notificationDto, dataFile);
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

    // -------------------- PRIVATE --------------------

    private String dataverseMessage(EmailNotificationDto notificationDto, Dataverse dataverse) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);
        String objectType = NotificationObjectType.DATAVERSE.toString().toLowerCase();

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:

                String joinedRoleNames = permissionService.getRolesOfUser(notificationDto.getNotificationReceiver(), dataverse).stream()
                        .map(RoleAssignment::getRole)
                        .map(DataverseRole::getAlias)
                        .map(RoleTranslationUtil::getLocaleNameFromAlias)
                        .collect(Collectors.joining("/"));

                String pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.assignRole",
                        notificationsEmailLanguage);

                messageText += MessageFormat.format(pattern,
                                                    joinedRoleNames,
                                                    objectType,
                                                    dataverse.getDisplayName(),
                                                    getDataverseLink(dataverse));

                if (joinedRoleNames.contains(BuiltInRole.FILE_DOWNLOADER.getAlias())) {
                    pattern = BundleUtil.getStringFromBundleWithLocale(
                            "notification.access.granted.fileDownloader.additionalDataverse",
                            notificationsEmailLanguage);
                    messageText += MessageFormat.format(pattern, " ");
                }

                return messageText;
            case REVOKEROLE:
                messageText += MessageFormat.format(BundleUtil.getStringFromBundleWithLocale("notification.email.revokeRole",
                        notificationsEmailLanguage),
                                                    objectType,
                                                    dataverse.getDisplayName(),
                                                    getDataverseLink(dataverse));
                return messageText;
            case CREATEDV:
                Dataverse parentDataverse = dataverse.getOwner();

                String dataverseCreatedMessage = BundleUtil.getStringFromBundleWithLocale("notification.email.createDataverse", notificationsEmailLanguage,
                                                                                        dataverse.getDisplayName(),
                                                                                        getDataverseLink(dataverse),
                                                                                        parentDataverse != null ? parentDataverse.getDisplayName() : "",
                                                                                        parentDataverse != null ? getDataverseLink(
                                                                                                parentDataverse) : "",
                                                                                        systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                                                                                        systemConfig.getGuidesVersion());

                logger.fine(dataverseCreatedMessage);
                return messageText + dataverseCreatedMessage;
        }

        return StringUtils.EMPTY;
    }

    private String datasetMessage(EmailNotificationDto notificationDto, Dataset dataset) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);
        String objectType = notificationDto.getNotificationObjectType().toString().toLowerCase();
        String pattern;

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:

                String joinedRoleNames = permissionService.getRolesOfUser(notificationDto.getNotificationReceiver(), dataset).stream()
                        .map(roleAssignment -> roleAssignment.getRole().getAlias())
                        .collect(Collectors.joining("/"));

                pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.assignRole",
                        notificationsEmailLanguage);

                messageText += MessageFormat.format(pattern,
                        joinedRoleNames, objectType, dataset.getDisplayName(), getDatasetLink(dataset));

                if (joinedRoleNames.contains(BuiltInRole.FILE_DOWNLOADER.getAlias())) {
                    pattern = BundleUtil.getStringFromBundleWithLocale(
                            "notification.access.granted.fileDownloader.additionalDataverse", notificationsEmailLanguage);
                    messageText += MessageFormat.format(pattern, " ");
                }
                return messageText;
            case REVOKEROLE:
                messageText += MessageFormat.format(BundleUtil.getStringFromBundleWithLocale("notification.email.revokeRole",
                        notificationsEmailLanguage), objectType, dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case GRANTFILEACCESS:
                pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.grantFileAccess",
                        notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case REJECTFILEACCESS:
                pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.rejectFileAccess",
                        notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        dataset.getDisplayName(), getDatasetLink(dataset));
                return messageText;
            case CHECKSUMFAIL:
                String checksumFailMsg = BundleUtil.getStringFromBundleWithLocale("notification.checksumfail",
                        notificationsEmailLanguage, dataset.getGlobalIdString());
                logger.fine("checksumFailMsg: " + checksumFailMsg);
                return messageText + checksumFailMsg;
            case GRANTFILEACCESSINFO:
                pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.grant.file.access.info.text", notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        notificationDto.getCustomUserMessage(), dataset.getDisplayName(), getDatasetLink(dataset),
                        getNumberOfUsersRequestingForFileAccess(dataset), getDatasetManageFilePermissionsLink(dataset));
                return messageText;
            case REJECTFILEACCESSINFO:
                pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.reject.file.access.info.text", notificationsEmailLanguage);
                messageText += MessageFormat.format(pattern,
                        notificationDto.getCustomUserMessage(), dataset.getDisplayName(), getDatasetLink(dataset),
                        getNumberOfUsersRequestingForFileAccess(dataset), getDatasetManageFilePermissionsLink(dataset));
                return messageText;
        }

        return StringUtils.EMPTY;
    }

    private String datasetVersionMessage(EmailNotificationDto notificationDto, DatasetVersion version) {

        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String greetingsText = BundleUtil.getStringFromBundleWithLocale("notification.email.greeting", notificationsEmailLanguage);

        switch (notificationDto.getNotificationType()) {
            case CREATEDS:
                String datasetCreatedMessage = BundleUtil.getStringFromBundleWithLocale("notification.email.createDataset",
                        notificationsEmailLanguage,
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()),
                        systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                        systemConfig.getGuidesVersion());

                return greetingsText + datasetCreatedMessage;
            case MAPLAYERUPDATED:
                return greetingsText + BundleUtil.getStringFromBundleWithLocale("notification.email.worldMap.added",
                        notificationsEmailLanguage,
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()));
            case PUBLISHEDDS:
                return greetingsText + BundleUtil.getStringFromBundleWithLocale("notification.email.wasPublished",
                        notificationsEmailLanguage,
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()));
            case SUBMITTEDDS:
                AuthenticatedUser requestor = notificationDto.getRequestor();
                String requestorName = requestor.getFirstName() + " " + requestor.getLastName();

                String requestorEmail = requestor.getEmail();

                return greetingsText +
                        BundleUtil.getStringFromBundleWithLocale("notification.email.wasSubmittedForReview",
                                notificationsEmailLanguage,
                                version.getDataset().getDisplayName(),
                                getDatasetDraftLink(version.getDataset()),
                                version.getDataset().getOwner().getDisplayName(),
                                getDataverseLink(version.getDataset().getOwner()),
                                requestorName,
                                requestorEmail) +
                        addUserCustomMessage(notificationDto,
                                BundleUtil.getStringFromBundleWithLocale("dataset.reject.messageBox.label", notificationsEmailLanguage));
            case RETURNEDDS:
                return greetingsText +
                        BundleUtil.getStringFromBundleWithLocale("notification.email.wasReturnedByReviewer",
                                notificationsEmailLanguage,
                                version.getDataset().getDisplayName(),
                                getDatasetDraftLink(version.getDataset()),
                                version.getDataset().getOwner().getDisplayName(),
                                getDataverseLink(version.getDataset().getOwner())) +
                        addUserCustomMessage(notificationDto,
                                BundleUtil.getStringFromBundleWithLocale("dataset.reject.messageBox.label", notificationsEmailLanguage));
            case FILESYSTEMIMPORT:

                return greetingsText +
                        BundleUtil.getStringFromBundleWithLocale("notification.mail.import.filesystem", notificationsEmailLanguage,
                                                                              systemConfig.getDataverseSiteUrl(),
                                                                              version.getDataset().getGlobalIdString(),
                                                                              version.getDataset().getDisplayName());

            case CHECKSUMIMPORT:

                return greetingsText + BundleUtil.getStringFromBundleWithLocale("notification.import.checksum", notificationsEmailLanguage,
                                                                                  version.getDataset().getGlobalIdString(),
                                                                                  version.getDataset().getDisplayName());

        }
        return StringUtils.EMPTY;
    }

    private String addUserCustomMessage(EmailNotificationDto notificationDto, String messagePrefix) {
        if(StringUtils.isNotEmpty(notificationDto.getCustomUserMessage())) {
            return String.format("\n\n%s\n\n%s", messagePrefix, notificationDto.getCustomUserMessage());
        }
        return StringUtils.EMPTY;
    }

    private String dataFileMessage(EmailNotificationDto notificationDto, DataFile dataFile) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(REQUESTFILEACCESS)) {
            AuthenticatedUser requestor = notificationDto.getRequestor();
            String pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.requestFileAccess",
                    notificationsEmailLanguage);

            String requestorName = requestor.getFirstName() + " " + requestor.getLastName();
            String requestorEmail = requestor.getEmail();

            messageText += MessageFormat.format(pattern, dataFile.getOwner().getDisplayName(), requestorName,
                    requestorEmail, getDatasetManageFileAccessLink(dataFile));
            return messageText;
        }
        return StringUtils.EMPTY;
    }

    private String fileMetadataMessage(EmailNotificationDto notificationDto, FileMetadata fileMetadata) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(MAPLAYERDELETEFAILED)) {

            DatasetVersion version = fileMetadata.getDatasetVersion();
            String pattern = BundleUtil.getStringFromBundleWithLocale("notification.email.maplayer.deletefailed.text",
                    notificationsEmailLanguage);

            messageText += MessageFormat.format(pattern, fileMetadata.getLabel(), getDatasetLink(version.getDataset()));
            return messageText;
        }
        return StringUtils.EMPTY;
    }

    private String authenticatedUserMessage(EmailNotificationDto notificationDto, String rootDataverseName, InternetAddress systemAddress) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
        String messageText = BundleUtil.getStringFromBundleWithLocale("notification.email.greeting",
                notificationsEmailLanguage);

        if (notificationDto.getNotificationType().equals(CREATEACC)) {

            String accountCreatedMessage = BundleUtil.getStringFromBundleWithLocale("notification.email.welcome", notificationsEmailLanguage,
                    rootDataverseName,
                    systemConfig.getGuidesBaseUrl(notificationsEmailLanguage),
                    systemConfig.getGuidesVersion(),
                    BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName, notificationsEmailLanguage),
                    BrandingUtil.getSupportTeamEmailAddress(systemAddress));
            String optionalConfirmEmailAddon = confirmEmailService.optionalConfirmEmailAddonMsg(notificationDto.getNotificationReceiver());
            accountCreatedMessage += optionalConfirmEmailAddon;
            logger.fine("accountCreatedMessage: " + accountCreatedMessage);
            return messageText + accountCreatedMessage;
        }

        return StringUtils.EMPTY;
    }

    private String getSubjectText(EmailNotificationDto notificationDto, String rootDataverseName) {
        Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();

        switch (notificationDto.getNotificationType()) {
            case ASSIGNROLE:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.assign.role.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case REVOKEROLE:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.revoke.role.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case CREATEDV:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.create.dataverse.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case REQUESTFILEACCESS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.request.file.access.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case GRANTFILEACCESS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.grant.file.access.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case REJECTFILEACCESS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.rejected.file.access.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case MAPLAYERUPDATED:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.update.maplayer",
                                                      notificationsEmailLanguage, rootDataverseName);
            case MAPLAYERDELETEFAILED:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.maplayer.deletefailed.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case CREATEDS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.create.dataset.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case SUBMITTEDDS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.submit.dataset.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case PUBLISHEDDS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.publish.dataset.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case RETURNEDDS:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.returned.dataset.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case CREATEACC:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.create.account.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case CHECKSUMFAIL:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.checksumfail.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case CHECKSUMIMPORT:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.import.checksum.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case CONFIRMEMAIL:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.verifyEmail.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case GRANTFILEACCESSINFO:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.grant.file.access.info.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
            case REJECTFILEACCESSINFO:
                return BundleUtil.getStringFromBundleWithLocale("notification.email.reject.file.access.info.subject",
                                                      notificationsEmailLanguage, rootDataverseName);
        }
        return StringUtils.EMPTY;
    }

    private String getSubjectTextForDatasetVersion(EmailNotificationDto notificationDto, String rootDataverseName, DatasetVersion datasetVersion) {
        if (notificationDto.getNotificationType().equals(FILESYSTEMIMPORT)) {
            Locale notificationsEmailLanguage = notificationDto.getNotificationReceiver().getNotificationsLanguage();
            try {
                return BundleUtil.getStringFromBundleWithLocale("notification.email.import.filesystem.subject",
                        notificationsEmailLanguage, datasetVersion.getDataset().getDisplayName());
            } catch (Exception e) {
                return BundleUtil.getStringFromBundleWithLocale("notification.email.import.filesystem.subject",
                        notificationsEmailLanguage, rootDataverseName);
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
        return systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=DRAFT";
    }

    private String getDatasetManageFilePermissionsLink(Dataset dataset) {
        return systemConfig.getDataverseSiteUrl() + "/permissions-manage-files.xhtml?id=" + dataset.getId();
    }

    private String getDataverseLink(Dataverse dataverse) {
        return systemConfig.getDataverseSiteUrl() + "/dataverse/" + dataverse.getAlias();
    }

    private String getNumberOfUsersRequestingForFileAccess(Dataset dataset) {
        long numberOfUsersRequestingForAccess = dataset.getFiles().stream()
                .filter(f -> f.getFileMetadatas().stream()
                        .anyMatch(m -> FileTermsOfUse.TermsOfUseType.RESTRICTED.equals(m.getTermsOfUse().getTermsOfUseType())))
                .map(DataFile::getFileAccessRequesters)
                .flatMap(Collection::stream)
                .distinct()
                .count();
        return String.valueOf(numberOfUsersRequestingForAccess);
    }
}
