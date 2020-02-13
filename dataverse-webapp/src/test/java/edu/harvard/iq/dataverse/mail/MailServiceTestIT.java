package edu.harvard.iq.dataverse.mail;


import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(Arquillian.class)
public class MailServiceTestIT extends WebappArquillianDeployment {
    private static final String subject = "Lorem ipsum";
    private static final EmailContent content = new EmailContent(subject, "", "");
    private static final String userMailAddress = "dataverse-user@gmail.com";

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
        Assume.assumeThat("Property [MailOverseerAddress] should be set to valid mail in domain mailinator.com or gmail.com",
                overseerEmail, not(emptyOrNullString()));

        // when
        mailService.sendMail(userMailAddress, content);
        Awaitility.await()
                .atMost(10L, TimeUnit.SECONDS)
                .until(() -> !smtpServer.mailBox().isEmpty());

        // then
        List<Tuple2<String, String>> mailBox = smtpServer.mailBox()
                .stream()
                .map(m -> Tuple.of(m.getTo(), m.getSubject()))
                .collect(Collectors.toList());
        Assert.assertThat("Two mails should be present in mailbox", smtpServer.mailBox().size(), is(2));
        Assert.assertThat("Should contain a mail for chosen user and a copy for overseer",
                mailBox, containsInAnyOrder(Tuple.of(overseerEmail, subject), Tuple.of(userMailAddress, subject)));
    }
}