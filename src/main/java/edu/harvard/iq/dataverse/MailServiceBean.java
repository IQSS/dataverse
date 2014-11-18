/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.sun.mail.smtp.SMTPSendFailedException;
import com.sun.mail.smtp.SMTPSenderFailedException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
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

    public void sendDoNotReplyMail(String to, String subject, String messageText) {
        try {
            System.out.print(to);
            Message msg = new MimeMessage(session);
            logger.fine("will be using address "+getNoReplyAddress());
            InternetAddress noReplyAddress = getNoReplyAddress();
            if (noReplyAddress != null) {
                msg.setFrom(noReplyAddress);
                msg.setSentDate(new Date());
                msg.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(to, false));
                msg.setSubject(subject);
                msg.setText(messageText + "\n\nPlease do not reply to this email.\nThank you,\nThe Dataverse Network Project");
                try {
                    Transport.send(msg);
                } catch (SMTPSendFailedException ssfe) {
                    if (ssfe.getNextException() instanceof SMTPSenderFailedException) {
                        // try again, with the "mailinator" address:
                        logger.warning("Failed to send mail from "+noReplyAddress+"; will try again, from invalid.email.address@mailinator.com");
                        msg.setFrom(new InternetAddress("invalid.email.address@mailinator.com"));
                        Transport.send(msg);
                    } else {
                        logger.warning("Failed to send mail to "+to+" (SMTPSendFailedException)");
                    }
                }
            } else {
                logger.warning("Skipping sending mail to "+to+", because the \"no-reply\" address could not be obtained."); 
            }
        } catch (AddressException ae) {
            logger.warning("Failed to send mail to "+to);
            ae.printStackTrace(System.out);
        } catch (MessagingException me) {
            logger.warning("Failed to send mail to "+to);
            me.printStackTrace(System.out);
        }
    }
    
    private InternetAddress getNoReplyAddress() {
        // We want this "fake" address to have our legit domain name - 
        // otherwise the mail server will likely reject relaying:
        
        String fqdn = systemConfig.getDataverseServer();
        String address = "";
        InternetAddress iAddress = null;
        
        if (fqdn != null) {
            try {
                iAddress = new InternetAddress("do-not-reply@" + fqdn);
            } catch (AddressException ae) {
                iAddress = null;
            }
        }
        
        if (iAddress == null) {
            // 
            // there may be relaying issues with this "mailinator" address 
            // - if the destination address is also outside our own domain. 
            // but it's probably better than nothing - so we'll try this:
            try {
                iAddress = new InternetAddress("invalid.email.address@mailinator.com");
            } catch (AddressException ae) {
                iAddress = null;
            }
        }
        
        return iAddress;
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
                msg.setFrom(getNoReplyAddress());
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
                sendDoNotReplyMail(emailAddress, subject, messageText);
                
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
                        sendDoNotReplyMail(emailAddress, subject, messageText);
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
            sendDoNotReplyMail(emailAddress, subject, messageText);

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
            
            if (version == null) {
                String datasetName = null;
                dataset = version.getDataset();
                if (dataset != null) {
                    datasetName = version.getTitle();
                    logger.fine("Dataset name: " + datasetName);

                    String messageText = "";

                    messageText = "Hello, \nWorldMap layer data has been added to the dataset named '" + datasetName + ".";
                    String subject = "Dataverse: WorldMap layer added to dataset";
                    sendDoNotReplyMail(emailAddress, subject, messageText);

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
            return dataverse.getOwner().getName();
        } 
        return null;
    }
}
