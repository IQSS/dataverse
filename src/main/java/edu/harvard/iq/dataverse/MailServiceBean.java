/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.sun.mail.smtp.SMTPSendFailedException;
import com.sun.mail.smtp.SMTPSenderFailedException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
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
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean versionService; 
    @EJB
    SystemConfig systemConfig;
    @EJB
    SettingsServiceBean settingsService;
    
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
        try {
             Message msg = new MimeMessage(session);

            InternetAddress systemAddress = getSystemAddress();
            if (systemAddress != null) {
                msg.setFrom(systemAddress);
                msg.setSentDate(new Date());
                msg.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(to, false));
                msg.setSubject(subject);
                msg.setText(messageText + "\n\nPlease do not reply to this email.\nThank you,\nThe Dataverse Network Project");
                try {
                    Transport.send(msg);
                    sent = true;
                } catch (SMTPSendFailedException ssfe) {
                    logger.warning("Failed to send mail to " + to + " (SMTPSendFailedException)");
                }
            } else {
                logger.warning("Skipping sending mail to " + to + ", because the \"no-reply\" address not set.");
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
      
       if (systemEmail!=null) {
           try { 
            return new InternetAddress(systemEmail);
           } catch(AddressException e) {
               return null;
           }
       }
       return null;
     
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
 
    public void sendCreateDataverseNotification(UserNotification notification) {
        String emailAddress = getUserEmailAddress(notification);

        if (emailAddress != null) {
            Long object_id = notification.getObjectId();
            Dataverse dataverse = null;
            if (object_id != null) {
                dataverse = dataverseService.find(notification.getObjectId());
            } else {
                logger.fine("Null dataverse id");
            }
            String dataverseName = null;
            String ownerDataverseName = null;
            if (dataverse != null) {
                dataverseName = dataverse.getName();
                logger.fine("Dataverse name: " + dataverseName);
                ownerDataverseName = getOwnerDataverseName(dataverse);
                logger.fine("Owner Dataverse name: " + ownerDataverseName);
                
                String messageText = "";
                
                if (ownerDataverseName != null) {
                    messageText = "Hello, \nYour new dataverse named '" + dataverseName + "' was"
                            + " created in the " + ownerDataverseName + " Dataverse. Remember to release your dataverse.";
                } else {
                    messageText = "Hello, \nYour new Root dataverse was"
                            + " created. Remember to release your Root dataverse.";
                }
                String subject = "Dataverse: Your dataverse has been created";
                sendSystemEmail(emailAddress, subject, messageText);
                
            } else {
                logger.warning("Skipping create dataverse notification, because no valid dataverse id was supplied");
            }
        } else {
            logger.warning("Skipping create dataverse notification, because email address is null");
        }
        
    }
    
    public void sendCreateDatasetNotification(UserNotification notification) {
        String emailAddress = getUserEmailAddress(notification);

        if (emailAddress != null) {
            Long object_id = notification.getObjectId();
            Dataset dataset = null;
            DatasetVersion version = null; 
            if (object_id != null) {
                version = versionService.find(notification.getObjectId());
            } else {
                logger.fine("Null version id");
            }
            if (version != null) {
                String datasetName = null;
                String ownerDataverseName = null;
                dataset = version.getDataset();
                if (dataset != null) {
                    datasetName = version.getTitle();
                    logger.fine("Dataset name: " + datasetName);
                    Dataverse ownerDataverse = dataset.getOwner();
                    if (ownerDataverse != null) {
                        ownerDataverseName = ownerDataverse.getName();
                        logger.fine("Owner Dataverse name: " + ownerDataverseName);

                        String messageText = "";

                        if ("Root".equals(ownerDataverseName)) {
                            messageText = "Hello, \nYour new dataset named '" + datasetName + "' was"
                                    + " created in the Root Dataverse. Remember to release your dataset.";
                        } else {
                            messageText = "Hello, \nYour new dataset named '" + datasetName + "' was"
                                    + " created in the " + ownerDataverseName + " Dataverse. Remember to release your dataset.";
                        }
                        String subject = "Dataverse: Your dataset has been created";
                        sendSystemEmail(emailAddress, subject, messageText);
                    } else {
                        logger.warning("Skipping create dataset notification, because the owner dataverse is NULL (?!)");
                    }
                } else {
                    logger.warning("Skipping create dataset notification, because no valid dataset was found!");
                }
            } else {
                logger.warning("Skipping create dataset notification, because no valid dataset version id was supplied");
            }
        } else {
            logger.warning("Skipping create dataset notification, because email address is null");
        }
    }
    
    public void sendCreateAccountNotification(UserNotification notification) {
        String emailAddress = getUserEmailAddress(notification);

        if (emailAddress != null) {

            String messageText = "Welcome to Dataverse 4.0 Beta! Please take a look around, try everything out, \n"
                    + "and check out our Google Group (https://groups.google.com/forum/#!forum/dataverse-community)\n"
                    + "to leave feedback.";

            String subject = "Dataverse: Your account has been created.";
            sendSystemEmail(emailAddress, subject, messageText);

        } else {
            logger.warning("Skipping create account notification, because email address is null");
        }
    }
    
    public void sendMapLayerUpdatedNotification(UserNotification notification) {
        String emailAddress = getUserEmailAddress(notification);

        if (emailAddress != null) {
            Long object_id = notification.getObjectId();
            Dataset dataset = null;
            DatasetVersion version = null; 
            if (object_id != null) {
                version = versionService.find(notification.getObjectId());
            } 
            
            if (version != null) {
                String datasetName = null;
                dataset = version.getDataset();
                if (dataset != null) {
                    datasetName = version.getTitle();
                    logger.fine("Dataset name: " + datasetName);

                    String messageText = "";

                    messageText = "Hello, \nWorldMap layer data has been added to the dataset named '" + datasetName + ".";
                    String subject = "Dataverse: WorldMap layer added to dataset";
                    sendSystemEmail(emailAddress, subject, messageText);

                } else {
                    logger.warning("Skipping Map Layer Updated notification, because no valid dataset was found!");
                }
            } else {
                logger.warning("Skipping Map Layer Updated notification, because no valid dataset versio id was supplied");
            }
        } else {
            logger.warning("Skipping Map Layer Updated notification, because email address is null");
        }
    }
    
    public Boolean sendNotificationEmail(UserNotification notification){
        boolean retval = false;
        String emailAddress = getUserEmailAddress(notification);
        if (emailAddress != null){
           Object objectOfNotification =  getObjectOfNotification(notification);
           if (objectOfNotification != null){
               String messageText = getMessageTextBasedOnNotification(notification);
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
        }
        return "";
    }
   
    private String getMessageTextBasedOnNotification(UserNotification userNotification){       
        
        String messageText = ResourceBundle.getBundle("Bundle").getString("notification.email.greeting");
        DatasetVersion version = null;
        String datasetName = "";
        String pattern ="";
        switch (userNotification.getType()) {
            case CREATEDV:
                Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
                String ownerDataverseName = getOwnerDataverseName(dataverse);
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.createDataverse");
                if (ownerDataverseName != null) {
                    String[] paramArray = {dataverse.getDisplayName(), ownerDataverseName};
                    messageText += MessageFormat.format(pattern, paramArray);
                } else {
                    messageText += MessageFormat.format(pattern, "Root Dataverse", "Root Dataverse");
                }
                return messageText;
            case REQUESTFILEACCESS:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.requestFileAccess");
                messageText += MessageFormat.format(pattern, version.getTitle());
                return messageText;
            case GRANTFILEACCESS:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.grantFileAccess");
                messageText += MessageFormat.format(pattern, version.getTitle());
                return messageText;
            case REJECTFILEACCESS:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.rejectFileAccess");
                messageText += MessageFormat.format(pattern, version.getTitle());
                return messageText;
            case CREATEDS:
                version = versionService.find(userNotification.getObjectId());
                datasetName = version.getTitle();
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.createDataset");
                String[] paramArray = {datasetName, version.getDataset().getOwner().getDisplayName()};
                messageText += MessageFormat.format(pattern, paramArray);
                return messageText;
            case MAPLAYERUPDATED:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.worldMap.added");
                messageText += MessageFormat.format(pattern, version.getTitle());
                return messageText;                   
            case SUBMITTEDDS:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.wasSubmittedForReview");
                messageText += MessageFormat.format(pattern, version.getTitle(), version.getDataset().getOwner().getDisplayName());
                return messageText;
            case PUBLISHEDDS:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.wasPublished");
                messageText += MessageFormat.format(pattern, version.getTitle(), version.getDataset().getOwner().getDisplayName());
                return messageText;
            case RETURNEDDS:
                version = versionService.find(userNotification.getObjectId());
                pattern = ResourceBundle.getBundle("Bundle").getString("notification.email.wasReturnedByReviewer");
                messageText += MessageFormat.format(pattern, version.getTitle(), version.getDataset().getOwner().getDisplayName());
                return messageText;
            case CREATEACC:
                messageText += ResourceBundle.getBundle("Bundle").getString("notification.email.welcome");
                return messageText;
        }
        
        return "";
    }
    
    private Object getObjectOfNotification (UserNotification userNotification){
        switch (userNotification.getType()) {
            case CREATEDV:
                return dataverseService.find(userNotification.getObjectId());
            case REQUESTFILEACCESS:
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
        }
        return null;
    }
    
    public void bulkSendNotifications() {
        List<UserNotification> notificationsList = userNotificationService.findUnemailed();
        if (notificationsList != null) {
            for (UserNotification notification : notificationsList) {
                
                if (UserNotification.Type.CREATEDV.equals(notification.getType())) {
                    logger.fine("sending Create Dataverse notification");
                     sendCreateDataverseNotification(notification);
                } else if (UserNotification.Type.CREATEDS.equals(notification.getType())) {
                    logger.fine("sending Create Dataset notification");
                    sendCreateDatasetNotification(notification);
                } else if (UserNotification.Type.CREATEACC.equals(notification.getType())) {
                    logger.fine("Sending CREATE ACCOUNT notification");
                    sendCreateAccountNotification(notification);
                } else if (UserNotification.Type.MAPLAYERUPDATED.equals(notification.getType())) {
                    logger.fine("Sending MAPLAYERUPDATED notification");
                    sendMapLayerUpdatedNotification(notification);
                }
                
                // As more types of notifications become available, this code 
                // will need to be modified, with proper handling methods added.
                // -- L.A. 4.0 beta 9
                
                notification.setEmailed(true);
                notification.setSendDate(new Timestamp(new Date().getTime()));
                userNotificationService.save(notification);
                // TODO: 
                // would be prudent to add some logic to only update the 
                // "emailed" status and the timestamp if the send was 
                // actually successful. 
                // -- L.A. 4.0 beta 9
               
            }
        }
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
     
    private String getOwnerDataverseName(Dataverse dataverse) {
        if (dataverse.getOwner() != null) {
            return dataverse.getOwner().getDisplayName();
        } 
        return null;
    }
}
