package edu.harvard.iq.dataverse.mail;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.Tuple;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.simplejavamail.mailer.Mailer;

import javax.mail.internet.InternetAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MailServiceTest {

    private MailService mailService;

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private DataverseServiceBean dataverseService;

    @Mock
    private MailMessageCreator mailMessageCreator;

    @Mock
    private Mailer mailer;

    @BeforeEach
    public void prepare() {
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.SystemEmail)).thenReturn("test@email.com");
        Mockito.when(mailMessageCreator.getMessageAndSubject(any(EmailNotificationDto.class),
                                                             any(AuthenticatedUser.class)))
                .thenReturn(Tuple.of("Nice Message", "Nice Subject"));

        Mockito.when(mailMessageCreator.createMailFooterMessage(anyString(), anyString(), any(InternetAddress.class)))
                .thenReturn("Nice Footer");

        Dataverse testDataverse = new Dataverse();
        testDataverse.setName("ROOT");
        Mockito.when(dataverseService.findRootDataverse()).thenReturn(testDataverse);

        Mockito.doNothing().when(mailer).sendMail(any());

        mailService = new MailService(dataverseService, settingsService, mailMessageCreator);
        mailService.setMailSender(mailer);
    }

    @Test
    public void sendNotificationEmail() {
        //given
        EmailNotificationDto testEmailNotificationDto = createTestEmailNotificationDto();

        //when
        Boolean emailSent = mailService.sendNotificationEmail(testEmailNotificationDto, new AuthenticatedUser());

        //then
        Assert.assertTrue(emailSent);
    }

    @Test
    public void sendSystemEmail() {
        //when
        boolean emailSent = mailService.sendMail("test@email.com", "Nice Subject", "Nice message");

        //then
        Assert.assertTrue(emailSent);
    }

    @Test
    public void sendNotificationEmail_WithException() {
        //given
        makeSmtpThrowException();
        EmailNotificationDto testEmailNotificationDto = createTestEmailNotificationDto();

        //when
        Boolean emailSent = mailService.sendNotificationEmail(testEmailNotificationDto, new AuthenticatedUser());

        //then
        Assert.assertFalse(emailSent);
    }

    @Test
    public void sendSystemEmail_WithException() {
        //given
        makeSmtpThrowException();

        //when
        boolean emailSent = mailService.sendMail("test@email.com", "Nice Subject", "Nice message");

        //then
        Assert.assertFalse(emailSent);
    }

    @Test
    public void sendMail() {
        //when
        boolean emailSent = mailService.sendMail("replay@email.com", "test@email.com", "Nice Subject", "Nice message");

        //then
        Assert.assertTrue(emailSent);
    }

    @Test
    public void sendMail_WithException() {
        //given
        makeSmtpThrowException();

        //when
        boolean emailSent = mailService.sendMail("replay@email.com", "test@email.com", "Nice Subject", "Nice message");

        //then
        Assert.assertFalse(emailSent);
    }

    private EmailNotificationDto createTestEmailNotificationDto() {
        return new EmailNotificationDto(1L,
                                        "useremail@test.com",
                                        NotificationType.CREATEDV,
                                        1L,
                                        NotificationObjectType.DATAVERSE,
                                        new AuthenticatedUser());
    }

    private void makeSmtpThrowException() {
        Mockito.doThrow(RuntimeException.class).when(mailer).sendMail(any());
        mailService.setMailSender(mailer);
    }
}