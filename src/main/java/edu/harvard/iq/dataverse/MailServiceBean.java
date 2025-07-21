/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import edu.harvard.iq.dataverse.validation.EMailValidator;
import jakarta.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * original author: roberttreacy
 */
@Stateless
public class MailServiceBean implements java.io.Serializable {
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean versionService; 
    @EJB
    SystemConfig systemConfig;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    GroupServiceBean groupService;
    @EJB
    ConfirmEmailServiceBean confirmEmailService;

    private static final Logger logger = Logger.getLogger(MailServiceBean.class.getCanonicalName());

    private static final String charset = "UTF-8";

    /**
     * Creates a new instance of MailServiceBean
     */
    public MailServiceBean() {
    }
    
    /**
     * Creates a new instance of MailServiceBean with explicit injection, as used during testing.
     */
    public MailServiceBean(Session session, SettingsServiceBean settingsService) {
        this.session = session;
        this.settingsService = settingsService;
    }

    @Inject
    @Named("mail/systemSession")
    private Session session;

    public boolean sendSystemEmail(String to, String subject, String messageText) {
        return sendSystemEmail(to, subject, messageText, false);
    }
    
    /**
     * Send a system notification to one or multiple recipients by email.
     * Will skip sending when {@link #getSystemAddress()} doesn't return a configured "from" address.
     * @param to A comma separated list of one or multiple recipients' addresses. May contain a "personal name" and
     *           the recipients address in &lt;&gt;. See also {@link InternetAddress}.
     * @param subject The message's subject
     * @param messageText The message's text
     * @param isHtmlContent Determine if the message text is formatted using HTML or plain text.
     * @return Status: true if sent successfully, false otherwise
     */
    public boolean sendSystemEmail(String to, String subject, String messageText, boolean isHtmlContent) {
        Optional<InternetAddress> optionalAddress = getSystemAddress();
        if (optionalAddress.isEmpty()) {
            logger.fine(() -> "Skipping sending mail to " + to + ", because no system address has been set.");
            return false;
        }
        InternetAddress systemAddress = optionalAddress.get();
        InternetAddress supportAddress = getSupportAddress().orElse(systemAddress);

        String body = messageText +
            BundleUtil.getStringFromBundle(isHtmlContent ? "notification.email.closing.html" : "notification.email.closing",
                List.of(BrandingUtil.getSupportTeamEmailAddress(supportAddress), BrandingUtil.getSupportTeamName(supportAddress)));

        logger.fine(() -> "Sending email to %s. Subject: <<<%s>>>. Body: %s".formatted(to, subject, body));
        try {
            // Since JavaMail 1.6, we have support for UTF-8 mail addresses and do not need to handle these ourselves.
            InternetAddress[] recipients = InternetAddress.parse(to);
            
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(systemAddress);
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, recipients);
            msg.setSubject(subject, charset);
            if (isHtmlContent) {
                msg.setText(body, charset, "html");
            } else {
                msg.setText(body, charset);
            }
            
            Transport.send(msg, recipients);
            return true;
        } catch (MessagingException ae) {
            logger.log(Level.WARNING, "Failed to send mail to %s: %s".formatted(to, ae.getMessage()), ae);
            logger.info("When UTF-8 characters in recipients: make sure MTA supports it and JVM option " + JvmSettings.MAIL_MTA_SUPPORT_UTF8.getScopedKey() + "=true");
        }
        return false;
    }
    
    /**
     * Lookup the system mail address ({@code InternetAddress} may contain personal and actual address).
     * @return The system mail address or an empty {@code Optional} if not configured.
     */
    public Optional<InternetAddress> getSystemAddress() {
        boolean providedByDB = false;
        String mailAddress = JvmSettings.SYSTEM_EMAIL.lookupOptional().orElse(null);
        
        // Try lookup of (deprecated) database setting only if not configured via MPCONFIG
        if (mailAddress == null) {
            mailAddress = settingsService.getValueForKey(Key.SystemEmail);
            // Encourage people to migrate from deprecated setting
            if (mailAddress != null) {
                providedByDB = true;
                logger.warning("The :SystemMail DB setting has been deprecated, please reconfigure using JVM option " + JvmSettings.SYSTEM_EMAIL.getScopedKey());
            }
        }
        
        try {
            // Parse and return.
            return Optional.of(new InternetAddress(Objects.requireNonNull(mailAddress), true));
        } catch (AddressException e) {
            logger.log(Level.WARNING, "Could not parse system mail address '%s' provided by %s: "
                .formatted(providedByDB ? "DB setting" : "JVM option", mailAddress), e);
        } catch (NullPointerException e) {
            // Do not pester the logs - no configuration may mean someone wants to disable mail notifications
            logger.fine("Could not find a system mail setting in database (key :SystemEmail, deprecated) or JVM option '" + JvmSettings.SYSTEM_EMAIL.getScopedKey() + "'");
        }
        // We define the system email address as an optional setting, in case people do not want to enable mail
        // notifications (like in a development context, but might be useful elsewhere, too).
        return Optional.empty();
    }
    
    /**
     * Lookup the support team mail address ({@code InternetAddress} may contain personal and actual address).
     * Will default to return {@code #getSystemAddress} if not configured.
     * @return Support team mail address
     */
    public Optional<InternetAddress> getSupportAddress() {
        Optional<String> supportMailAddress = JvmSettings.SUPPORT_EMAIL.lookupOptional();
        if (supportMailAddress.isPresent()) {
            try {
                return Optional.of(new InternetAddress(supportMailAddress.get(), true));
            } catch (AddressException e) {
                logger.log(Level.WARNING, "Could not parse support mail address '%s', defaulting to system address: ".formatted(supportMailAddress.get()), e);
            }
        }
        return getSystemAddress();
    }

    //@Resource(name="mail/notifyMailSession")
    public void sendMail(String reply, String to, String cc, String subject, String messageText) {
        Optional<InternetAddress> optionalAddress = getSystemAddress();
        if (optionalAddress.isEmpty()) {
            logger.fine(() -> "Skipping sending mail to " + to + ", because no system address has been set.");
            return;
        }
        // Always send from system address to avoid email being blocked
        InternetAddress fromAddress = optionalAddress.get();
        
        try {
            MimeMessage msg = new MimeMessage(session);
            
            try {
                setContactDelegation(reply, fromAddress);
            } catch (UnsupportedEncodingException ex) {
                logger.severe(ex.getMessage());
            }
            msg.setFrom(fromAddress);
            if (EMailValidator.isEmailValid(reply)) {
            	// But set the reply-to address to direct replies to the requested 'from' party if it is a valid email address
                msg.setReplyTo(new Address[] {new InternetAddress(reply)});
            } else {
                // Otherwise include the invalid 'from' address in the message
                messageText = "From: " + reply + "\n\n" + messageText;
            }
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            if (cc != null) {
                msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc, false));
            }
            msg.setSubject(subject, charset);
            msg.setText(messageText, charset);

            Transport.send(msg);
        } catch (AddressException ae) {
            ae.printStackTrace(System.out);
        } catch (MessagingException me) {
            me.printStackTrace(System.out);
        }
    }

    /**
     * Set the contact delegation as "[dataverse team] on behalf of [user email]"
     * @param reply The user's email address as give via the contact form
     * @param fromAddress The system email address
     * @throws UnsupportedEncodingException
     */
    public void setContactDelegation(String reply, InternetAddress fromAddress)
            throws UnsupportedEncodingException {
        String personal = fromAddress.getPersonal() != null
            ? fromAddress.getPersonal()
            : BrandingUtil.getInstallationBrandName() != null
                ? BrandingUtil.getInstallationBrandName()
                : BundleUtil.getStringFromBundle("contact.delegation.default_personal");
        fromAddress.setPersonal(
            BundleUtil.getStringFromBundle(
                "contact.delegation",
                Arrays.asList(personal, reply)),
            charset
        );
    }

    public Boolean sendNotificationEmail(UserNotification notification){
        return sendNotificationEmail(notification, "");
    }

    public Boolean sendNotificationEmail(UserNotification notification, String comment) {
        return sendNotificationEmail(notification, comment, null, false);
    }

    public Boolean sendNotificationEmail(UserNotification notification, String comment, boolean isHtmlContent) {
        return sendNotificationEmail(notification, comment, null, isHtmlContent);
    }

    public Boolean sendNotificationEmail(UserNotification notification, String comment, AuthenticatedUser requestor, boolean isHtmlContent){

        boolean retval = false;
        String emailAddress = getUserEmailAddress(notification);
        if (emailAddress != null){
           Object objectOfNotification =  getObjectOfNotification(notification);
           if (objectOfNotification != null){
               String messageText = getMessageTextBasedOnNotification(notification, objectOfNotification, comment, requestor, null);
               String subjectText = MailUtil.getSubjectTextBasedOnNotification(notification, objectOfNotification);
               if (!(StringUtils.isEmpty(messageText) || StringUtils.isEmpty(subjectText))){
                   retval = sendSystemEmail(emailAddress, subjectText, messageText, isHtmlContent);
               } else {
                   logger.warning("Skipping " + notification.getType() +  " notification, because couldn't get valid message");
               }
           } else { 
               logger.warning("Skipping " + notification.getType() +  " notification, because no valid Object was found");
           }           
        } else {
            logger.warning("Skipping " + notification.getType() +  " notification, because email address is null");
        }
        return retval;
    }

    private String getDatasetManageFileAccessLink(DataFile datafile){
        return  systemConfig.getDataverseSiteUrl() + "/permissions-manage-files.xhtml?id=" + datafile.getOwner().getId();
    } 

    private String getDatasetLink(Dataset dataset){        
        return  systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString();
    } 

    private String getDatasetDraftLink(Dataset dataset){        
        return  systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&version=DRAFT" + "&faces-redirect=true"; 
    } 

    private String getDataverseLink(Dataverse dataverse){       
        return  systemConfig.getDataverseSiteUrl() + "/dataverse/" + dataverse.getAlias();
    }

    /**
     * Returns a '/'-separated string of roles that are effective for {@code au}
     * over {@code dvObj}. Traverses the containment hierarchy of the {@code d}.
     * Takes into consideration all groups that {@code au} is part of.
     * @param au The authenticated user whose role assignments we look for.
     * @param dvObj The Dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code ra} over {@code d}.
     */
    private String getRoleStringFromUser(AuthenticatedUser au, DvObject dvObj) {
        // Find user's role(s) for given dataverse/dataset
        Set<RoleAssignment> roles = permissionService.assignmentsFor(au, dvObj);
        List<String> roleNames = new ArrayList<>();

        // Include roles derived from a user's groups
        Set<Group> groupsUserBelongsTo = groupService.groupsFor(au, dvObj);
        for (Group g : groupsUserBelongsTo) {
            roles.addAll(permissionService.assignmentsFor(g, dvObj));
        }

        for (RoleAssignment ra : roles) {
            roleNames.add(ra.getRole().getName());
        }
        return StringUtils.join(roleNames, "/");
    }

    /**
     * Returns the URL to a given {@code DvObject} {@code d}. If {@code d} is a
     * {@code DataFile}, return a link to its {@code DataSet}.
     * @param d The Dataverse object to get a link for.
     * @return A string with a URL to the given Dataverse object.
     */
    private String getDvObjectLink(DvObject d) {
        if (d instanceof Dataverse) {
            return getDataverseLink((Dataverse) d);
        } else if (d instanceof Dataset) {
            return getDatasetLink((Dataset) d);
        } else if (d instanceof DataFile) {
            return getDatasetLink(((DataFile) d).getOwner());
        }
        return "";
    }

    /**
     * Returns string representation of the type of {@code DvObject} {@code d}.
     * @param d The Dataverse object to get the string for
     * @return A string that represents the type of a given Dataverse object.
     */
    private String getDvObjectTypeString(DvObject d) {
        if (d instanceof Dataverse) {
            return "dataverse";
        } else if (d instanceof Dataset) {
            return "dataset";
        } else if (d instanceof DataFile) {
            return "data file";
        }
        return "";
    }

    public String getMessageTextBasedOnNotification(UserNotification userNotification, Object targetObject){
        return getMessageTextBasedOnNotification(userNotification, targetObject, "");
    }

    public String getMessageTextBasedOnNotification(UserNotification userNotification, Object targetObject, String comment) {
        return getMessageTextBasedOnNotification(userNotification, targetObject, comment, null, null);
    }

    public String getMessageTextBasedOnNotification(UserNotification userNotification, Object targetObject, String comment, AuthenticatedUser requestor, SystemConfig.UI ui) {
        String messageText = BundleUtil.getStringFromBundle("notification.email.greeting");
        DatasetVersion version;
        Dataset dataset;
        DvObject dvObj;
        String dvObjURL;
        String dvObjTypeStr;
        String pattern;

        switch (userNotification.getType()) {
            case ASSIGNROLE:
                AuthenticatedUser au = userNotification.getUser();
                dvObj = (DvObject) targetObject;

                String joinedRoleNames = getRoleStringFromUser(au, dvObj);

                dvObjURL = getDvObjectLink(dvObj);
                dvObjTypeStr = getDvObjectTypeString(dvObj);

                pattern = BundleUtil.getStringFromBundle("notification.email.assignRole");
                String[] paramArrayAssignRole = {joinedRoleNames, dvObjTypeStr, dvObj.getDisplayName(), dvObjURL};
                messageText += MessageFormat.format(pattern, paramArrayAssignRole);
                if (joinedRoleNames.contains("File Downloader")){
                    if (dvObjTypeStr.equals("dataset")){
                         pattern = BundleUtil.getStringFromBundle("notification.access.granted.fileDownloader.additionalDataset");
                         String[]  paramArrayAssignRoleDS = {" "};
                        messageText += MessageFormat.format(pattern, paramArrayAssignRoleDS);
                    }
                    if (dvObjTypeStr.equals("dataverse")){
                        pattern = BundleUtil.getStringFromBundle("notification.access.granted.fileDownloader.additionalDataverse");
                         String[]  paramArrayAssignRoleDV = {" "};
                        messageText += MessageFormat.format(pattern, paramArrayAssignRoleDV);
                    }                   
                }
                return messageText;
            case REVOKEROLE:
                dvObj = (DvObject) targetObject;

                dvObjURL = getDvObjectLink(dvObj);
                dvObjTypeStr = getDvObjectTypeString(dvObj);

                pattern = BundleUtil.getStringFromBundle("notification.email.revokeRole");
                String[] paramArrayRevokeRole = {dvObjTypeStr, dvObj.getDisplayName(), dvObjURL};
                messageText += MessageFormat.format(pattern, paramArrayRevokeRole);
                return messageText;
            case CREATEDV:
                Dataverse dataverse = (Dataverse) targetObject;
                Dataverse parentDataverse = dataverse.getOwner();
                // initialize to empty string in the rare case that there is no parent dataverse (i.e. root dataverse just created)
                String parentDataverseDisplayName = "";
                String parentDataverseUrl = "";
                if (parentDataverse != null) {
                    parentDataverseDisplayName = parentDataverse.getDisplayName();
                    parentDataverseUrl = getDataverseLink(parentDataverse);
                }
                String dataverseCreatedMessage = BundleUtil.getStringFromBundle("notification.email.createDataverse", Arrays.asList(
                        dataverse.getDisplayName(),
                        getDataverseLink(dataverse),
                        parentDataverseDisplayName,
                        parentDataverseUrl,
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion()));
                logger.fine(dataverseCreatedMessage);
                return messageText += dataverseCreatedMessage;
            case REQUESTFILEACCESS:
                //Notification to those who can grant file access requests on a dataset when a user makes a request
                DataFile datafile = (DataFile) targetObject;
                
                pattern = BundleUtil.getStringFromBundle("notification.email.requestFileAccess");
                String requestorName = (requestor.getLastName() != null && requestor.getLastName() != null) ? requestor.getFirstName() + " " + requestor.getLastName() : BundleUtil.getStringFromBundle("notification.email.info.unavailable");
                String requestorEmail = requestor.getEmail() != null ? requestor.getEmail() : BundleUtil.getStringFromBundle("notification.email.info.unavailable"); 
                String[] paramArrayRequestFileAccess = {datafile.getOwner().getDisplayName(), requestorName, requestorEmail, getDatasetManageFileAccessLink(datafile)};
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                messageText += MessageFormat.format(pattern, paramArrayRequestFileAccess);
                FileAccessRequest far = datafile.getAccessRequestForAssignee(requestor);
                GuestbookResponse gbr = far.getGuestbookResponse();
                if (gbr != null) {
                    messageText += MessageFormat.format(
                        BundleUtil.getStringFromBundle("notification.email.requestFileAccess.guestbookResponse"), gbr.toHtmlFormattedResponse(requestor));
                }
                return messageText;
            case GRANTFILEACCESS:
                dataset = (Dataset) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.grantFileAccess");
                String[] paramArrayGrantFileAccess = {dataset.getDisplayName(), getDatasetLink(dataset)};
                messageText += MessageFormat.format(pattern, paramArrayGrantFileAccess);
                return messageText;
            case REJECTFILEACCESS:
                dataset = (Dataset) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.rejectFileAccess");
                String[] paramArrayRejectFileAccess = {dataset.getDisplayName(), getDatasetLink(dataset)};
                messageText += MessageFormat.format(pattern, paramArrayRejectFileAccess);
                return messageText;
            case DATASETCREATED:
                dataset = (Dataset) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.datasetWasCreated");
                String[] paramArrayDatasetCreated = {getDatasetLink(dataset), dataset.getDisplayName(), userNotification.getRequestor().getName(), dataset.getOwner().getDisplayName()};
                messageText += MessageFormat.format(pattern, paramArrayDatasetCreated);
                return messageText;
            case CREATEDS:
                version =  (DatasetVersion) targetObject;
                String datasetCreatedMessage = BundleUtil.getStringFromBundle("notification.email.createDataset", Arrays.asList(
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()),
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion()
                ));
                logger.fine(datasetCreatedMessage);
                return messageText += datasetCreatedMessage;
            case SUBMITTEDDS:
                version =  (DatasetVersion) targetObject;
                String mightHaveSubmissionComment = "";              
                /*
                FIXME
                Setting up to add single comment when design completed
                "submissionComment" needs to be added to Bundle
                mightHaveSubmissionComment = ".";
                if (comment != null && !comment.isEmpty()) {
                    mightHaveSubmissionComment = ".\n\n" + BundleUtil.getStringFromBundle("submissionComment") + "\n\n" + comment;
                }
                */                
                 requestorName = (requestor.getLastName() != null && requestor.getLastName() != null) ? requestor.getFirstName() + " " + requestor.getLastName() : BundleUtil.getStringFromBundle("notification.email.info.unavailable");
                 requestorEmail = requestor.getEmail() != null ? requestor.getEmail() : BundleUtil.getStringFromBundle("notification.email.info.unavailable");               
                pattern = BundleUtil.getStringFromBundle("notification.email.wasSubmittedForReview");

                String[] paramArraySubmittedDataset = {version.getDataset().getDisplayName(), getDatasetDraftLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner()),
                   requestorName, requestorEmail  };
                messageText += MessageFormat.format(pattern, paramArraySubmittedDataset);
                return messageText;
            case PUBLISHEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.wasPublished");
                String[] paramArrayPublishedDataset = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArrayPublishedDataset);
                return messageText;
            case PUBLISHFAILED_PIDREG:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.publishFailedPidReg");
                String[] paramArrayPublishFailedDatasetPidReg = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArrayPublishFailedDatasetPidReg);
                return messageText;
            case RETURNEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.wasReturnedByReviewer");

                String[] paramArrayReturnedDataset = {version.getDataset().getDisplayName(), getDatasetDraftLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArrayReturnedDataset);

                if (comment != null && !comment.isEmpty()) {
                    messageText += "\n\n" + MessageFormat.format(BundleUtil.getStringFromBundle("notification.email.wasReturnedByReviewerReason"), comment);
                }

                Dataverse d = (Dataverse) version.getDataset().getOwner();
                List<String> contactEmailList = new ArrayList<String>();
                for (DataverseContact dc : d.getDataverseContacts()) {
                    contactEmailList.add(dc.getContactEmail());
                }
                if (!contactEmailList.isEmpty()) {
                    String contactEmails = String.join(", ", contactEmailList);
                    messageText += "\n\n" + MessageFormat.format(BundleUtil.getStringFromBundle("notification.email.wasReturnedByReviewer.collectionContacts"), contactEmails);
                }
                return messageText;

            case WORKFLOW_SUCCESS:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.workflow.success");
                if (comment == null) {
                    comment = BundleUtil.getStringFromBundle("notification.email.workflow.nullMessage");
                }
                String[] paramArrayWorkflowSuccess = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset()), comment};
                messageText += MessageFormat.format(pattern, paramArrayWorkflowSuccess);
                return messageText;
            case WORKFLOW_FAILURE:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.workflow.failure");
                if (comment == null) {
                    comment = BundleUtil.getStringFromBundle("notification.email.workflow.nullMessage");
                }
                String[] paramArrayWorkflowFailure = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset()), comment};
                messageText += MessageFormat.format(pattern, paramArrayWorkflowFailure);
                return messageText;
            case STATUSUPDATED:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.status.change");
                CurationStatus status = version.getCurationStatusAsOfDate(userNotification.getSendDateTimestamp());
                String curationLabel = DatasetUtil.getLocaleCurationStatusLabel(status);
                if(curationLabel == null) {
                    curationLabel = BundleUtil.getStringFromBundle("dataset.curationstatus.none");
                }
                String[] paramArrayStatus = {
                        version.getDataset().getDisplayName(),
                        getDatasetLink(version.getDataset()),
                        version.getDataset().getOwner().getDisplayName(),
                        getDataverseLink(version.getDataset().getOwner()),
                        curationLabel
                    };
                messageText += MessageFormat.format(pattern, paramArrayStatus);
                  
                return messageText;
            case PIDRECONCILED:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.pid.reconciled");
                messageText += MessageFormat.format(pattern, new String[] {version.getDataset().getDisplayName(), version.getDataset().getGlobalId().asString()});
                return messageText;
            case CREATEACC:
                String accountCreatedMessage = BundleUtil.getStringFromBundle("notification.email.welcome", Arrays.asList(
                        BrandingUtil.getInstallationBrandName(),
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion(),
                        BrandingUtil.getSupportTeamName(getSystemAddress().orElse(null)),
                        BrandingUtil.getSupportTeamEmailAddress(getSystemAddress().orElse(null))
                ));
                String optionalConfirmEmailAddon = confirmEmailService.optionalConfirmEmailAddonMsg(userNotification.getUser(), ui);
                accountCreatedMessage += optionalConfirmEmailAddon;
                logger.fine("accountCreatedMessage: " + accountCreatedMessage);
                return messageText += accountCreatedMessage;

            case CHECKSUMFAIL:
                dataset =  (Dataset) targetObject;
                String checksumFailMsg = BundleUtil.getStringFromBundle("notification.checksumfail", Arrays.asList(
                        dataset.getGlobalId().asString()
                ));
                logger.fine("checksumFailMsg: " + checksumFailMsg);
                return messageText += checksumFailMsg;

            case FILESYSTEMIMPORT:
                version =  (DatasetVersion) targetObject;
                String fileImportMsg = BundleUtil.getStringFromBundle("notification.mail.import.filesystem", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        version.getDataset().getGlobalId().asString(),
                        version.getDataset().getDisplayName()
                ));
                logger.fine("fileImportMsg: " + fileImportMsg);
                return messageText += fileImportMsg;

            case GLOBUSUPLOADCOMPLETED:
                dataset =  (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String uploadCompletedMessage = messageText + BundleUtil.getStringFromBundle("notification.mail.globus.upload.completed", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        comment
                ))  ;
                return  uploadCompletedMessage;

            case GLOBUSDOWNLOADCOMPLETED:
                dataset =  (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String downloadCompletedMessage = messageText + BundleUtil.getStringFromBundle("notification.mail.globus.download.completed", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        comment
                ))  ;
                return downloadCompletedMessage;
                        
            case GLOBUSUPLOADCOMPLETEDWITHERRORS:
                dataset =  (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String uploadCompletedWithErrorsMessage = messageText + BundleUtil.getStringFromBundle("notification.mail.globus.upload.completedWithErrors", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        comment
                ))  ;
                return  uploadCompletedWithErrorsMessage;
            
            case GLOBUSUPLOADREMOTEFAILURE:
                dataset =  (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String uploadFailedRemotelyMessage = messageText + BundleUtil.getStringFromBundle("notification.mail.globus.upload.failedRemotely", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        comment
                ))  ;
                return  uploadFailedRemotelyMessage;

            case GLOBUSUPLOADLOCALFAILURE:
                dataset =  (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String uploadFailedLocallyMessage = messageText + BundleUtil.getStringFromBundle("notification.mail.globus.upload.failedLocally", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        comment
                ))  ;
                return  uploadFailedLocallyMessage;
                
                case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
                dataset =  (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String downloadCompletedWithErrorsMessage = messageText + BundleUtil.getStringFromBundle("notification.mail.globus.download.completedWithErrors", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        comment
                ))  ;
                return downloadCompletedWithErrorsMessage;

            case CHECKSUMIMPORT:
                version =  (DatasetVersion) targetObject;
                String checksumImportMsg = BundleUtil.getStringFromBundle("notification.import.checksum", Arrays.asList(
                        version.getDataset().getGlobalId().asString(),
                        version.getDataset().getDisplayName()
                ));
                logger.fine("checksumImportMsg: " + checksumImportMsg);
                return messageText += checksumImportMsg;

            case APIGENERATED:
                String message = BundleUtil.getStringFromBundle("notification.email.apiTokenGenerated", Arrays.asList(
                        userNotification.getUser().getFirstName(), userNotification.getUser().getFirstName() ));
                return message;

            case INGESTCOMPLETED:
                dataset = (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String ingestedCompletedMessage = messageText + BundleUtil.getStringFromBundle("notification.ingest.completed", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion(),
                        comment
                ));

                return ingestedCompletedMessage;
            case INGESTCOMPLETEDWITHERRORS:
                dataset = (Dataset) targetObject;
                messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                String ingestedCompletedWithErrorsMessage = messageText + BundleUtil.getStringFromBundle("notification.ingest.completedwitherrors", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().asString(),
                        dataset.getDisplayName(),
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion(),
                        comment
                ));

                return ingestedCompletedWithErrorsMessage;
            case DATASETMENTIONED:
                String additionalInfo = userNotification.getAdditionalInfo();
                dataset = (Dataset) targetObject;
                JsonObject citingResource = null;
                citingResource = JsonUtil.getJsonObject(additionalInfo);
                

                pattern = BundleUtil.getStringFromBundle("notification.email.datasetWasMentioned");
                Object[] paramArrayDatasetMentioned = {
                        userNotification.getUser().getName(),
                        BrandingUtil.getInstallationBrandName(), 
                        citingResource.getString("@type"),
                        citingResource.getString("@id"),
                        citingResource.getString("name"),
                        citingResource.getString("relationship"), 
                        systemConfig.getDataverseSiteUrl(),
                        dataset.getGlobalId().toString(), 
                        dataset.getDisplayName()};
                messageText = MessageFormat.format(pattern, paramArrayDatasetMentioned);
                return messageText;
            case REQUESTEDFILEACCESS:
                //Notification to requestor when they make a request
                datafile = (DataFile) targetObject;
                
                pattern = BundleUtil.getStringFromBundle("notification.email.requestedFileAccess");
                 messageText = BundleUtil.getStringFromBundle("notification.email.greeting.html");
                 messageText += MessageFormat.format(pattern, getDvObjectLink(datafile), datafile.getOwner().getDisplayName());
                far = datafile.getAccessRequestForAssignee(requestor);
                gbr = far.getGuestbookResponse();
                if (gbr != null) {
                    messageText += MessageFormat.format(
                            BundleUtil.getStringFromBundle("notification.email.requestFileAccess.guestbookResponse"), gbr.toHtmlFormattedResponse());
                }
                return messageText;
        }

        return "";
    }

    public Object getObjectOfNotification (UserNotification userNotification){
        switch (userNotification.getType()) {
            case ASSIGNROLE:
            case REVOKEROLE:
                // Can either be a dataverse or dataset, so search both
                Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
                if (dataverse != null) {
                    return dataverse;
                }

                Dataset dataset = datasetService.find(userNotification.getObjectId());
                return dataset;
            case CREATEDV:
                return dataverseService.find(userNotification.getObjectId());
            case REQUESTFILEACCESS:
            case REQUESTEDFILEACCESS:
                return dataFileService.find(userNotification.getObjectId());
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
            case DATASETCREATED:
            case DATASETMENTIONED:
                return datasetService.find(userNotification.getObjectId());
            case CREATEDS:
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case PUBLISHFAILED_PIDREG:
            case RETURNEDDS:
            case WORKFLOW_SUCCESS:
            case WORKFLOW_FAILURE:
            case PIDRECONCILED:
            case STATUSUPDATED:
                return versionService.find(userNotification.getObjectId());
            case CREATEACC:
                return userNotification.getUser();
            case CHECKSUMFAIL:
                return datasetService.find(userNotification.getObjectId());
            case FILESYSTEMIMPORT:
                return versionService.find(userNotification.getObjectId());
            case GLOBUSUPLOADCOMPLETED:
            case GLOBUSUPLOADCOMPLETEDWITHERRORS:
            case GLOBUSUPLOADREMOTEFAILURE:
            case GLOBUSUPLOADLOCALFAILURE: 
            case GLOBUSDOWNLOADCOMPLETED:
            case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
                return datasetService.find(userNotification.getObjectId());
            case CHECKSUMIMPORT:
                return versionService.find(userNotification.getObjectId());
            case APIGENERATED:
                return userNotification.getUser();
            case INGESTCOMPLETED:
            case INGESTCOMPLETEDWITHERRORS:
                return datasetService.find(userNotification.getObjectId());

        }
        return null;
    }

    private String getUserEmailAddress(UserNotification notification) {
        if (notification != null) {
            if (notification.getUser() != null) {
                if (notification.getUser().getDisplayInfo() != null) {
                    if (notification.getUser().getDisplayInfo().getEmailAddress() != null) {
                        logger.fine("Email address: "+notification.getUser().getDisplayInfo().getEmailAddress());
                        return notification.getUser().getDisplayInfo().getEmailAddress();
                    }
                }
            }
        }

        logger.fine("no email address");
        return null; 
    }
}
