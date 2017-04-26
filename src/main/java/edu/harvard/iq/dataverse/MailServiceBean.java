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
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
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
    
    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    
    /**
     * Creates a new instance of MailServiceBean
     */
    public MailServiceBean() {
    }

    public void sendMail(String host, String from, String to, String subject, String messageText) {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(props, null);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(messageText);
            Transport.send(msg);
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
        String body = messageText + ResourceBundle.getBundle("Bundle").getString("notification.email.closing");
        logger.fine("Sending email to " + to + ". Subject: <<<" + subject + ">>>. Body: " + body);
        try {
             Message msg = new MimeMessage(session);

            InternetAddress systemAddress = getSystemAddress();
            if (systemAddress != null) {
                msg.setFrom(systemAddress);
                msg.setSentDate(new Date());
                msg.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(to, false));
                msg.setSubject(subject);
                msg.setText(body);
                try {
                    Transport.send(msg);
                    sent = true;
                } catch (SMTPSendFailedException ssfe) {
                    logger.warning("Failed to send mail to " + to + " (SMTPSendFailedException)");
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
        sendMail(from, to, subject, messageText, new HashMap());
    }

    public void sendMail(String from, String to, String subject, String messageText, Map extraHeaders) {
        try {
            Message msg = new MimeMessage(session);
            if (from.matches(EMAIL_PATTERN)) {
                msg.setFrom(new InternetAddress(from));
            } else {
                // set fake from address; instead, add it as part of the message
                //msg.setFrom(new InternetAddress("invalid.email.address@mailinator.com"));
                msg.setFrom(getSystemAddress());
                messageText = "From: " + from + "\n\n" + messageText;
            }
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(messageText);

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
        boolean retval = false;
        String emailAddress = getUserEmailAddress(notification);
        if (emailAddress != null){
           Object objectOfNotification =  getObjectOfNotification(notification);
           if (objectOfNotification != null){
               String messageText = getMessageTextBasedOnNotification(notification, objectOfNotification);
               String subjectText = getSubjectTextBasedOnNotification(notification);              
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
        
    private String getSubjectTextBasedOnNotification(UserNotification userNotification) {
        switch (userNotification.getType()) {
            case ASSIGNROLE:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.assign.role.subject");
            case REVOKEROLE:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.revoke.role.subject");
            case CREATEDV:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.create.dataverse.subject");
            case REQUESTFILEACCESS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.request.file.access.subject");
            case GRANTFILEACCESS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.grant.file.access.subject");
            case REJECTFILEACCESS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.rejected.file.access.subject");
            case MAPLAYERUPDATED:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.update.maplayer");
            case CREATEDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.create.dataset.subject");
            case SUBMITTEDDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.submit.dataset.subject");
            case PUBLISHEDDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.publish.dataset.subject");
            case RETURNEDDS:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.returned.dataset.subject");
            case CREATEACC:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.create.account.subject");
            case CHECKSUMFAIL:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.checksumfail.subject");
            case FILESYSTEMIMPORT:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.import.filesystem.subject");
            case CHECKSUMIMPORT:
                return ResourceBundle.getBundle("Bundle").getString("notification.email.import.checksum.subject");
        }
        return "";
    }
    
    private String getDatasetManageFileAccessLink(DataFile datafile){
        return  systemConfig.getDataverseSiteUrl() + "/permissions-manage-files.xhtml?id=" + datafile.getOwner().getId();
    } 
    
    private String getDatasetLink(Dataset dataset){        
        return  systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalId();
    } 
    
    private String getDatasetDraftLink(Dataset dataset){        
        return  systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&version=DRAFT" + "&faces-redirect=true"; 
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
        List<String> roleNames = new ArrayList();

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

    private String getMessageTextBasedOnNotification(UserNotification userNotification, Object targetObject){       
        
        String messageText = ResourceBundle.getBundle("Bundle").getString("notification.email.greeting");
        DatasetVersion version = null;
        Dataset dataset = null;
        DvObject dvObj = null;
        String dvObjURL = null;
        String dvObjTypeStr = null;
        String pattern ="";

        switch (userNotification.getType()) {
            case ASSIGNROLE:
                AuthenticatedUser au = userNotification.getUser();
                dvObj = (DvObject) targetObject;

                String joinedRoleNames = getRoleStringFromUser(au, dvObj);

                dvObjURL = getDvObjectLink(dvObj);
                dvObjTypeStr = getDvObjectTypeString(dvObj);

                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.assignRole");
                String[] paramArrayAssignRole = {joinedRoleNames, dvObjTypeStr, dvObj.getDisplayName(), dvObjURL};
                messageText += MessageFormat.format(pattern, paramArrayAssignRole);
                if (joinedRoleNames.contains("File Downloader")){
                    if (dvObjTypeStr.equals("dataset")){
                         pattern = ResourceBundle.getBundle("Bundle").getString("notification.access.granted.fileDownloader.additionalDataset");
                         String[]  paramArrayAssignRoleDS = {" "};
                        messageText += MessageFormat.format(pattern, paramArrayAssignRoleDS);
                    }
                    if (dvObjTypeStr.equals("dataverse")){
                        pattern = ResourceBundle.getBundle("Bundle").getString("notification.access.granted.fileDownloader.additionalDataverse");
                         String[]  paramArrayAssignRoleDV = {" "};
                        messageText += MessageFormat.format(pattern, paramArrayAssignRoleDV);
                    }                   
                }
                return messageText;
            case REVOKEROLE:
                dvObj = (DvObject) targetObject;

                dvObjURL = getDvObjectLink(dvObj);
                dvObjTypeStr = getDvObjectTypeString(dvObj);

                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.revokeRole");
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
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.requestFileAccess");
                String[] paramArrayRequestFileAccess = {datafile.getOwner().getDisplayName(), getDatasetManageFileAccessLink(datafile)};
                messageText += MessageFormat.format(pattern, paramArrayRequestFileAccess);
                return messageText;
            case GRANTFILEACCESS:
                dataset = (Dataset) targetObject;
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.grantFileAccess");
                String[] paramArrayGrantFileAccess = {dataset.getDisplayName(), getDatasetLink(dataset)};
                messageText += MessageFormat.format(pattern, paramArrayGrantFileAccess);
                return messageText;
            case REJECTFILEACCESS:
                dataset = (Dataset) targetObject;
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.rejectFileAccess");
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
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.worldMap.added");
                String[] paramArrayMapLayer = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset())};
                messageText += MessageFormat.format(pattern, paramArrayMapLayer);
                return messageText;                   
            case SUBMITTEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.wasSubmittedForReview");
                String[] paramArraySubmittedDataset = {version.getDataset().getDisplayName(), getDatasetDraftLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArraySubmittedDataset);
                return messageText;
            case PUBLISHEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.wasPublished");
                String[] paramArrayPublishedDataset = {version.getDataset().getDisplayName(), getDatasetLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArrayPublishedDataset);
                return messageText;
            case RETURNEDDS:
                version =  (DatasetVersion) targetObject;
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.wasReturnedByReviewer");
                String[] paramArrayReturnedDataset = {version.getDataset().getDisplayName(), getDatasetDraftLink(version.getDataset()), 
                    version.getDataset().getOwner().getDisplayName(),  getDataverseLink(version.getDataset().getOwner())};
                messageText += MessageFormat.format(pattern, paramArrayReturnedDataset);
                return messageText;
            case CREATEACC:
                String accountCreatedMessage = BundleUtil.getStringFromBundle("notification.email.welcome", Arrays.asList(
                        systemConfig.getGuidesBaseUrl(),
                        systemConfig.getGuidesVersion()
                ));
                String optionalConfirmEmailAddon = confirmEmailService.optionalConfirmEmailAddonMsg(userNotification.getUser());
                accountCreatedMessage += optionalConfirmEmailAddon;
                logger.fine("accountCreatedMessage: " + accountCreatedMessage);
                return messageText += accountCreatedMessage;

            case CHECKSUMFAIL:
                version =  (DatasetVersion) targetObject;
                String checksumFailMsg = BundleUtil.getStringFromBundle("notification.checksumfail", Arrays.asList(
                        version.getDataset().getGlobalId()
                ));
                logger.info("checksumFailMsg: " + checksumFailMsg);
                return messageText += checksumFailMsg;

            case FILESYSTEMIMPORT:
                version =  (DatasetVersion) targetObject;
                String fileImportMsg = BundleUtil.getStringFromBundle("notification.import.filesystem", Arrays.asList(
                        systemConfig.getDataverseSiteUrl(),
                        version.getDataset().getGlobalId(),
                        version.getDataset().getDisplayName()
                ));
                logger.info("fileImportMsg: " + fileImportMsg);
                return messageText += fileImportMsg;

            case CHECKSUMIMPORT:
                version =  (DatasetVersion) targetObject;
                String checksumImportMsg = BundleUtil.getStringFromBundle("notification.import.checksum", Arrays.asList(
                        version.getDataset().getGlobalId(),
                        version.getDataset().getDisplayName()
                ));
                logger.info("checksumImportMsg: " + checksumImportMsg);
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
                if (dataverse != null) return dataverse;

                Dataset dataset = datasetService.find(userNotification.getObjectId());
                return dataset;
            case CREATEDV:
                return dataverseService.find(userNotification.getObjectId());
            case REQUESTFILEACCESS:
                return dataFileService.find(userNotification.getObjectId());
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
                return datasetService.find(userNotification.getObjectId());
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
