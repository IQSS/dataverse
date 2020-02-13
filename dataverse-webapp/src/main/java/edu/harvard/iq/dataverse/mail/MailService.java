/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mail;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.email.EmailPopulatingBuilder;
import org.simplejavamail.email.Recipient;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for mail sending.
 */
@Stateless
public class MailService implements java.io.Serializable {

    private DataverseDao dataverseDao;
    private SettingsServiceBean settingsService;
    private MailMessageCreator mailMessageCreator;

    private Mailer mailSender;
    private ExecutorService executorService;


    @Resource(name = "mail/notifyMailSession")
    private Session session;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement */
    public MailService() {
    }

    @Inject
    public MailService(DataverseDao dataverseDao, SettingsServiceBean settingsService, MailMessageCreator mailMessageCreator) {
        this.dataverseDao = dataverseDao;
        this.settingsService = settingsService;
        this.mailMessageCreator = mailMessageCreator;
    }

    @PostConstruct
    public void prepareMailSession() {
        mailSender = MailerBuilder
                .usingSession(session)
                .withDebugLogging(true)
                .buildMailer();

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // -------------------- LOGIC --------------------

    /**
     * Gathers template messages for given notification and sends email.
     *
     * @return true if email was sent or false if some error occurred and email could not be sent.
     */
    public Boolean sendNotificationEmail(EmailNotificationDto notification) {

        String userEmail = notification.getUserEmail();
        String systemEmail = settingsService.getValueForKey(Key.SystemEmail);

        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notification, systemEmail);

        if (messageAndSubject._1().isEmpty() || messageAndSubject._2().isEmpty()) {
            return false;
        }

        String footerMessage = getFooterMailMessage(notification.getNotificationReceiver().getNotificationsLanguage());

        return sendMail(userEmail, new EmailContent(messageAndSubject._2(), messageAndSubject._1(), footerMessage));
    }

    /**
     * Gathers template messages for given notification and sends email.
     *
     * @return true if email was sent or false if some error occurred and email could not be sent.
     */
    public Boolean sendNotificationEmail(EmailNotificationDto notification, AuthenticatedUser requestor) {

        String userEmail = notification.getUserEmail();

        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notification, requestor);

        if (messageAndSubject._1().isEmpty() && messageAndSubject._2().isEmpty()) {
            return false;
        }

        String footerMessage = getFooterMailMessage(notification.getNotificationReceiver().getNotificationsLanguage());

        return sendMail(userEmail, new EmailContent(messageAndSubject._2(), messageAndSubject._1(), footerMessage));
    }

    public CompletableFuture<Boolean> sendMailAsync(String recipientsEmails, EmailContent emailContent) {

        return CompletableFuture.supplyAsync(() -> sendMail(recipientsEmails, emailContent), executorService);
    }

    public CompletableFuture<Boolean> sendMailAsync(String replyEmail, String recipientsEmails, String subject, String messageText) {
        return CompletableFuture.supplyAsync(() -> sendMail(replyEmail, recipientsEmails, subject, messageText),
                                             executorService);
    }

    /**
     * Sends email(s).
     *
     * @param recipientsEmails - comma separated emails.
     */
    public boolean sendMail(String recipientsEmails, EmailContent emailContent) {

        Email email = newMailWithOverseerIfExists()
                .from(getSystemAddress())
                .withRecipients(mailMessageCreator.createRecipients(recipientsEmails, StringUtils.EMPTY))
                .withSubject(emailContent.getSubject())
                .appendText(emailContent.getMessageText() + emailContent.getFooter())
                .buildEmail();

        return Try.run(() -> mailSender.sendMail(email))
                .map(emailSent -> true)
                .onFailure(Throwable::printStackTrace)
                .getOrElse(false);
    }

    /**
     * Sends email(s) with replay email.
     *
     * @param recipientsEmails - comma separated emails.
     */
    public boolean sendMail(String replyEmail, String recipientsEmails, String subject, String messageText) {

        Email email = newMailWithOverseerIfExists()
                .from(getSystemAddress())
                .withRecipients(mailMessageCreator.createRecipients(recipientsEmails, StringUtils.EMPTY))
                .withSubject(subject)
                .withReplyTo(replyEmail)
                .appendText(messageText)
                .buildEmail();

        return Try.run(() -> mailSender.sendMail(email))
                .map(emailSent -> true)
                .onFailure(Throwable::printStackTrace)
                .getOrElse(false);
    }

    public String getFooterMailMessage(Locale footerLocale) {
        return mailMessageCreator.createMailFooterMessage(footerLocale,
                                                          dataverseDao.findRootDataverse().getName(),
                                                          getSystemAddress());
    }

    // -------------------- PRIVATE --------------------

    private EmailPopulatingBuilder newMailWithOverseerIfExists() {
        EmailPopulatingBuilder builder = EmailBuilder.startingBlank();
        Option<Recipient> overseer = createOverseerRecipient();
        return overseer.isDefined()
                ? builder.bcc(overseer.get())
                : builder;
    }

    private Option<Recipient> createOverseerRecipient() {
        String overseerEmail = settingsService.getValueForKey(Key.MailOverseerAddress);
        return StringUtils.isNotBlank(overseerEmail)
                ? Option.some(new Recipient(null, overseerEmail, Message.RecipientType.BCC))
                : Option.none();
    }

    private InternetAddress getSystemAddress() {
        String systemEmail = settingsService.getValueForKey(Key.SystemEmail);

        return Try.of(() -> new InternetAddress(systemEmail))
                .getOrElseThrow(throwable -> new IllegalArgumentException(
                        "Email will not be sent due to invalid email: " + systemEmail));
    }

    // -------------------- SETTERS --------------------

    void setMailSender(Mailer mailSender) {
        this.mailSender = mailSender;
    }
}
