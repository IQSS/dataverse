/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import javax.annotation.Resource;
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
            msg.setFrom();
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(messageText + "\n\nPlease do not reply to this email.\nThank you,\nThe Dataverse Network Project");
            Transport.send(msg);
        } catch (AddressException ae) {
            ae.printStackTrace(System.out);
        } catch (MessagingException me) {
            me.printStackTrace(System.out);
        }
    }

    //@Resource(name="mail/notifyMailSession")
    public void sendMail(String from, String to, String subject, String messageText) {
        sendMail(from, to, subject, messageText, new HashMap());
    }

    public void sendMail(String from, String to, String subject, String messageText, Map extraHeaders) {
        try {
            Message msg = new MimeMessage(session);
            try {
                msg.setFrom(new InternetAddress(from));
            } catch (AddressException ae) {
                // set fake from address; instead, add it as part of the message
                msg.setFrom(new InternetAddress("invalid.email.address@mailinator.com"));
                messageText = "From: " + from + "\n\n" + messageText;
            }
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

    public void sendCreateDataverseNotification(String dataverseCreatorEmail, String dataverseName, String dataverseOwner) {
        String subject = "Dataverse: Your dataverse has been created";
        String messageText = "Hello, \nYour new dataverse named '" + dataverseName + "' was"
                + " created in the " + dataverseOwner + " Dataverse. Remember to release your dataverse.";
        sendDoNotReplyMail(dataverseCreatorEmail, subject, messageText);
    }
}
