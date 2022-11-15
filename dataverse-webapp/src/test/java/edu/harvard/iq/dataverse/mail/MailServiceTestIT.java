package edu.harvard.iq.dataverse.mail;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.awaitility.Awaitility;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class MailServiceTestIT extends WebappArquillianDeployment {
    private static final String subject = "Lorem ipsum";
    private static final EmailContent content = new EmailContent(subject, "", "");
    private static final String userMailAddress = "dv-user@mailinator.com";

    private String overseerEmail;

    @Inject
    private MailService mailService;

    @Inject
    private SettingsServiceBean settingsService;

    @Before
    public void setUp() {
        this.overseerEmail = settingsService.getValueForKey(Key.MailOverseerAddress);
        smtpServer.mailBox().clear();
    }

    @Test
    public void sendMail_whenOverseerActive() {
        // given
        Assume.assumeThat(overseerEmail, not(emptyOrNullString()));

        // when
        mailService.sendMail(userMailAddress, null, content);
        Awaitility.await()
                .atMost(10L, TimeUnit.SECONDS)
                .until(() -> !smtpServer.mailBox().isEmpty());

        // then
        List<Tuple2<String, String>> mailBox = smtpServer.mailBox()
                .stream()
                .map(m -> Tuple.of(m.getTo(), m.getSubject()))
                .collect(Collectors.toList());
        assertThat("Two mails should be present in mailbox", smtpServer.mailBox().size(), is(2));
        assertThat("Should contain a mail for chosen user and a copy for overseer",
                mailBox, containsInAnyOrder(Tuple.of(overseerEmail, subject), Tuple.of(userMailAddress, subject)));
    }

    @Test
    public void sendNotificationEmail__sendCopyOnDatasetReturn() {
        // given
        String content = "ad6f789b677c8976e0";
        String editorMail = "dv-editor@mailinator.com";

        Map<String, String> params = new HashMap<>();
        params.put(NotificationParameter.REPLY_TO.key(), editorMail);
        params.put(NotificationParameter.SEND_COPY.key(), "true");
        params.put(NotificationParameter.MESSAGE.key(), content);
        EmailNotificationDto notificationDto = new EmailNotificationDto(1L, userMailAddress,
                NotificationType.RETURNEDDS, 41L, NotificationObjectType.DATASET_VERSION,
                MocksFactory.makeAuthenticatedUser("User", "First"), params);

        // when
        mailService.sendNotificationEmail(notificationDto);

        // then
        List<Tuple2<String, String>> mailBox = smtpServer.mailBox()
                .stream()
                .map(m -> Tuple.of(m.getTo(), m.getEmailStr()))
                .filter(m -> !overseerEmail.equals(m._1()))
                .collect(Collectors.toList());
        assertThat("Two mails should be present in mailbox (after filtering out overseer copies)",
                mailBox.size(), is(2));
        assertThat("User and editor should have received a mail",
                mailBox.stream().map(Tuple2::_1).collect(Collectors.toList()),
                containsInAnyOrder(userMailAddress, editorMail));
        assertThat("Both mails should contain the chosen content",
                mailBox.stream().allMatch(m -> m._2().contains(content)), is(true));
    }
}