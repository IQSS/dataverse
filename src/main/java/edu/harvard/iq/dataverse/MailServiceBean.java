/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.sun.mail.smtp.SMTPSendFailedException;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.StringUtils;

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

    public void sendMail(String host, String reply, String to, String subject, String messageText) {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(props, null);

        try {
            MimeMessage msg = new MimeMessage(session);
            String[] recipientStrings = to.split(",");
            InternetAddress[] recipients = new InternetAddress[recipientStrings.length];
            try {
            	InternetAddress fromAddress=getSystemAddress();
            	fromAddress.setPersonal(BundleUtil.getStringFromBundle("contact.delegation", Arrays.asList(
                        fromAddress.getPersonal(), reply)), charset);
            	msg.setFrom(fromAddress);
                msg.setReplyTo(new Address[] {new InternetAddress(reply, charset)});
                for (int i = 0; i < recipients.length; i++) {
                    recipients[i] = new InternetAddress(recipientStrings[i], "", charset);
                }
            } catch (UnsupportedEncodingException ex) {
                logger.severe(ex.getMessage());
            }
            msg.setRecipients(Message.RecipientType.TO, recipients);
            msg.setSubject(subject, charset);
            msg.setText(messageText, charset);
            Transport.send(msg, recipients);
        } catch (AddressException ae) {
            ae.printStackTrace(System.out);
        } catch (MessagingException me) {
            me.printStackTrace(System.out);
        }
    }
    
    @Resource(name = "mail/notifyMailSession")
    private Session session;

    public boolean sendSystemEmail(String to, String subject, String messageText) {

        boolean sent = false;
        String rootDataverseName = dataverseService.findRootDataverse().getName();
        InternetAddress systemAddress = getSystemAddress();
        String body = messageText + BundleUtil.getStringFromBundle("notification.email.closing", Arrays.asList(BrandingUtil.getSupportTeamEmailAddress(systemAddress), BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName)));
        logger.fine("Sending email to " + to + ". Subject: <<<" + subject + ">>>. Body: " + body);
        try {
            MimeMessage msg = new MimeMessage(session);
            if (systemAddress != null) {
                msg.setFrom(systemAddress);
                msg.setSentDate(new Date());
                String[] recipientStrings = to.split(",");
                InternetAddress[] recipients = new InternetAddress[recipientStrings.length];
                for (int i = 0; i < recipients.length; i++) {
                    try {
                        recipients[i] = new InternetAddress(recipientStrings[i], "", charset);
                    } catch (UnsupportedEncodingException ex) {
                        logger.severe(ex.getMessage());
                    }
                }
                msg.setRecipients(Message.RecipientType.TO, recipients);
                msg.setSubject(subject, charset);
                msg.setText(body, charset);
                try {
                    Transport.send(msg, recipients);
                    sent = true;
                } catch (SMTPSendFailedException ssfe) {
                    logger.warning("Failed to send mail to: " + to);
                    logger.warning("SMTPSendFailedException Message: " + ssfe);
                }
            } else {
                logger.fine("Skipping sending mail to " + to + ", because the \"no-reply\" address not set (" + Key.SystemEmail + " setting).");
            }
        } catch (AddressException ae) {
            logger.warning("Failed to send mail to " + to);
            ae.printStackTrace(System.out);
        } catch (MessagingException me) {
            logger.warning("Failed to send mail to " + to);
            me.printStackTrace(System.out);
        }
        return sent;
    }
    
    private InternetAddress getSystemAddress() {
       String systemEmail =  settingsService.getValueForKey(Key.SystemEmail);
       return MailUtil.parseSystemAddress(systemEmail);
    }

    //@Resource(name="mail/notifyMailSession")
    public void sendMail(String from, String to, String subject, String messageText) {
        sendMail(from, to, subject, messageText, new HashMap<>());
    }

    public void sendMail(String reply, String to, String subject, String messageText, Map<Object, Object> extraHeaders) {
        try {
            MimeMessage msg = new MimeMessage(session);
            //Always send from system address to avoid email being blocked
            InternetAddress fromAddress=getSystemAddress();
            try {
              fromAddress.setPersonal(BundleUtil.getStringFromBundle("contact.delegation", Arrays.asList(
                      fromAddress.getPersonal(), reply)), charset);
            } catch (UnsupportedEncodingException ex) {
                logger.severe(ex.getMessage());
            }
            msg.setFrom(fromAddress);
            if (EMailValidator.isEmailValid(reply, null)) {
            	//But set the reply-to address to direct replies to the requested 'from' party if it is a valid email address	
                msg.setReplyTo(new Address[] {new InternetAddress(reply)});
            } else {
                //Otherwise include the invalid 'from' address in the message
                messageText = "From: " + reply + "\n\n" + messageText;
            }
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            msg.setSubject(subject, charset);
            msg.setText(messageText, charset);

            if (extraHeaders != null) {
                for (Object key : extraHeaders.keySet()) {
                    String headerName = key.toString();
                    String headerValue = extraHeaders.get(key).toString();

                    msg.addHeader(headerName, headerValue);
                }
            }

            Transport.send(msg);
        } catch (AddressException ae) {
            ae.printStackTrace(System.out);
        } catch (MessagingException me) {
            me.printStackTrace(System.out);
        }
    }
    
    public Boolean sendNotificationEmail(UserNotification notification){  
        return sendNotificationEmail(notification, "");
    }
    
    
    public Boolean sendNotificationEmail(UserNotification notification, String comment){  

        boolean retval = false;
        String emailAddress = getUserEmailAddress(notification);
        if (emailAddress != null){
           Object objectOfNotification =  getObjectOfNotification(notification);
           if (objectOfNotification != null){
               String messageText = getMessageTextBasedOnNotification(notification, objectOfNotification);
               String rootDataverseName = dataverseService.findRootDataverse().getName();
               String subjectText = MailUtil.getSubjectTextBasedOnNotification(notification, rootDataverseName, objectOfNotification);
               if (!(messageText.isEmpty() || subjectText.isEmpty())){
                    retval = sendSystemEmail(emailAddress, subjectText, messageText); 
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
        return  systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString();
    } 
    
    private String getDatasetDraftLink(Dataset dataset){        
        return  systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=DRAFT" + "&faces-redirect=true"; 
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
    
    public String getMessageTextBasedOnNotification(UserNotification userNotification, Object targetObject, String comment){       
        
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
                DataFile datafile = (DataFile) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.requestFileAccess");
                String[] paramArrayRequestFileAccess = {datafile.getOwner().getDisplayName(), getDatasetManageFileAccessLink(datafile)};
                messageText += MessageFormat.format(pattern, paramArrayRequestFileAccess);
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
            case MAPLAYERUPDATED:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.worldMap.added");
                String[] paramArrayMapLayer = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset())};
                messageText += MessageFormat.format(pattern, paramArrayMapLayer);
                return messageText;
            case MAPLAYERDELETEFAILED:
                FileMetadata targetFileMetadata = (FileMetadata) targetObject;
                version =  targetFileMetadata.getDatasetVersion();
                pattern = BundleUtil.getStringFromBundle("notification.email.maplayer.deletefailed.text");
                String[] paramArrayMapLayerDelete = {targetFileMetadata.getLabel(), getDatasetLink(version.getDataset())};
                messageText += MessageFormat.format(pattern, paramArrayMapLayerDelete);
                return messageText;                   
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
                pattern = BundleUtil.getStringFromBundle("notification.email.wasSubmittedForReview");
                String[] paramArraySubmittedDataset = {version.getDataset().getDisplayName(), getDatasetDraftLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner()), mightHaveSubmissionComment};
                messageText += MessageFormat.format(pattern, paramArraySubmittedDataset);
                return messageText;
            case PUBLISHEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.wasPublished");
                String[] paramArrayPublishedDataset = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArrayPublishedDataset);
                return messageText;
            case RETURNEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = BundleUtil.getStringFromBundle("notification.email.wasReturnedByReviewer");
                
                String optionalReturnReason = "";
                /*
                FIXME
                Setting up to add single comment when design completed
                optionalReturnReason = ".";
                if (comment != null && !comment.isEmpty()) {
                    optionalReturnReason = ".\n\n" + BundleUtil.getStringFromBundle("wasReturnedReason") + "\n\n" + comment;
                }
                */
                String[] paramArrayReturnedDataset = {version.getDataset().getDisplayName(), getDatasetDraftLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner()), optionalReturnReason};
                messageText += MessageFormat.format(pattern, paramArrayReturnedDataset);
                return messageText;
            case CREATEACC:
                String rootDataverseName = dataverseService.findRootDataverse().getName();
                InternetAddress systemAddress = getSystemAddress();
                String accountCreatedMessage = BundleUtil.getStringFromBundle("notification.email.welcome", Arrays.asList(
                        BrandingUtil.getInstallationBrandName(rootDataverseName),
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion(),
                        BrandingUtil.getSupportTeamName(systemAddress, rootDataverseName),
                        BrandingUtil.getSupportTeamEmailAddress(systemAddress)
                ));
                String optionalConfirmEmailAddon = confirmEmailService.optionalConfirmEmailAddonMsg(userNotification.getUser());
                accountCreatedMessage += optionalConfirmEmailAddon;
                logger.fine("accountCreatedMessage: " + accountCreatedMessage);
                return messageText += accountCreatedMessage;

            case CHECKSUMFAIL:
                dataset =  (Dataset) targetObject;
                String checksumFailMsg = BundleUtil.getStringFromBundle("notification.checksumfail", Arrays.asList(
                        dataset.getGlobalIdString()
                ));
                logger.fine("checksumFailMsg: " + checksumFailMsg);
                return messageText += checksumFailMsg;

            case FILESYSTEMIMPORT:
                version =  (DatasetVersion) targetObject;
                String fileImportMsg = BundleUtil.getStringFromBundle("notification.mail.import.filesystem", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        version.getDataset().getGlobalIdString(),
                        version.getDataset().getDisplayName()
                ));
                logger.fine("fileImportMsg: " + fileImportMsg);
                return messageText += fileImportMsg;

            case CHECKSUMIMPORT:
                version =  (DatasetVersion) targetObject;
                String checksumImportMsg = BundleUtil.getStringFromBundle("notification.import.checksum", Arrays.asList(
                        version.getDataset().getGlobalIdString(),
                        version.getDataset().getDisplayName()
                ));
                logger.fine("checksumImportMsg: " + checksumImportMsg);
                return messageText += checksumImportMsg;

        }
        
        return "";
    }
    
    private Object getObjectOfNotification (UserNotification userNotification){
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
                return dataFileService.find(userNotification.getObjectId());
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
                return datasetService.find(userNotification.getObjectId());
            case MAPLAYERDELETEFAILED:
                return dataFileService.findFileMetadata(userNotification.getObjectId());
            case MAPLAYERUPDATED:
            case CREATEDS:
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case RETURNEDDS:
                return versionService.find(userNotification.getObjectId());
            case CREATEACC:
                return userNotification.getUser();
            case CHECKSUMFAIL:
                return datasetService.find(userNotification.getObjectId());
            case FILESYSTEMIMPORT:
                return versionService.find(userNotification.getObjectId());
            case CHECKSUMIMPORT:
                return versionService.find(userNotification.getObjectId());
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
