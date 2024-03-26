package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.MailSessionProducer;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.testing.Tags;
import io.restassured.RestAssured;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An integration test using a fake SMTP MTA to check for outgoing mails.
 * LIMITATION: This test cannot possibly check if the production and injection of the session via CDI
 *             works, as it is not running within a servlet container. This would require usage of Arquillian
 *             or and end-to-end API test with a deployed application.
 */

@Tag(Tags.INTEGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
@JvmSetting(key = JvmSettings.MAIL_MTA_SETTING, method = "tcSmtpHost", varArgs = "host")
@JvmSetting(key = JvmSettings.MAIL_MTA_SETTING, method = "tcSmtpPort", varArgs = "port")
class MailServiceBeanIT {
    
    private static final Integer PORT_SMTP = 1025;
    private static final Integer PORT_HTTP = 1080;
    
    static MailServiceBean mailer;
    static Session session;
    static SettingsServiceBean settingsServiceBean = Mockito.mock(SettingsServiceBean.class);
    static DataverseServiceBean dataverseServiceBean = Mockito.mock(DataverseServiceBean.class);
    
    @BeforeAll
    static void setUp() {
        // Setup mocks behavior, inject as deps
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.SystemEmail)).thenReturn("noreply@example.org");
        BrandingUtil.injectServices(dataverseServiceBean, settingsServiceBean);
        
        // Must happen here, as we need Testcontainers to start the container first...
        session = new MailSessionProducer().getSession();
        mailer = new MailServiceBean(session, settingsServiceBean);
    }
    
    /*
        Cannot use maildev/maildev here. Also MailCatcher doesn't provide official support for SMTPUTF8.
        Also maildev does advertise the feature and everything is fine over the wire, both JSON API and Web UI
        of maildev have an encoding problem - UTF-8 mail addresses following RFC 6530/6531/6532 are botched.
        Neither MailCatcher nor MailHog have this problem, yet the API of MailCatcher is much simpler
        to use during testing, which is why we are going with it.
     */
    @Container
    static GenericContainer<?> maildev = new GenericContainer<>("dockage/mailcatcher")
        .withExposedPorts(PORT_HTTP, PORT_SMTP)
        .waitingFor(Wait.forHttp("/"));
    
    static String tcSmtpHost() {
        return maildev.getHost();
    }
    
    static String tcSmtpPort() {
        return maildev.getMappedPort(PORT_SMTP).toString();
    }
    
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://" + tcSmtpHost();
        RestAssured.port = maildev.getMappedPort(PORT_HTTP);
    }
    
    static List<String> mailTo() {
        return List.of(
            "pete@mailinator.com", // one example using ASCII only, make sure it works
            "michélle.pereboom@example.com",
            "begüm.vriezen@example.com",
            "lótus.gonçalves@example.com",
            "lótus.gonçalves@éxample.com",
            "begüm.vriezen@example.cologne",
            "رونیکا.محمدخان@example.com",
            "lótus.gonçalves@example.cóm"
        );
    }
    
    @ParameterizedTest
    @MethodSource("mailTo")
    @JvmSetting(key = JvmSettings.MAIL_MTA_SUPPORT_UTF8, value = "true")
    void sendEmailIncludingUTF8(String mailAddress) {
        given().when().get("/messages")
            .then()
            .statusCode(200);
        
        // given
        Session session = new MailSessionProducer().getSession();
        MailServiceBean mailer = new MailServiceBean(session, settingsServiceBean);
        
        // when
        boolean sent = mailer.sendSystemEmail(mailAddress, "Test", "Test üüü", false);
        
        // then
        assertTrue(sent);
        //RestAssured.get("/messages").body().prettyPrint();
        given().when().get("/messages")
            .then()
            .statusCode(200)
            .body("last().recipients.first()", equalTo("<" + mailAddress + ">"));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = "test@example.org")
    @JvmSetting(key = JvmSettings.MAIL_MTA_SUPPORT_UTF8, value = "false")
    void mailRejectedWhenUTF8AddressButNoSupport() throws AddressException {
        // given
        Session session = new MailSessionProducer().getSession();
        MailServiceBean mailer = new MailServiceBean(session, settingsServiceBean);
        String to = "michélle.pereboom@example.com";
        
        assertFalse(mailer.sendSystemEmail(to, "Test", "Test", false));
    }
    
}