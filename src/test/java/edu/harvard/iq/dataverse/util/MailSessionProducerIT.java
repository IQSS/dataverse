package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.RestAssured;
import jakarta.mail.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An integration test using a fake SMTP MTA to check for outgoing mails.
 * LIMITATION: This test cannot possibly check if the production and injection of the session via CDI
 *             works, as it is not running within a servlet container. This would require usage of Arquillian
 *             or and end-to-end API test with a deployed application.
 */
@Testcontainers
@ExtendWith(MockitoExtension.class)
class MailSessionProducerIT {
    
    private static final Integer PORT_SMTP = 1025;
    private static final Integer PORT_HTTP = 1080;
    
    Integer smtpPort;
    String smtpHost;
    
    @Mock
    SettingsServiceBean settingsServiceBean;
    @Mock
    DataverseServiceBean dataverseServiceBean;
    
    @Container
    static GenericContainer<?> maildev = new GenericContainer<>("maildev/maildev:2.1.0")
        .withExposedPorts(PORT_HTTP, PORT_SMTP)
        .waitingFor(Wait.forHttp("/"));

    @BeforeEach
    void setUp() {
        smtpHost = maildev.getHost();
        smtpPort = maildev.getMappedPort(PORT_SMTP);
        Integer httpPort = maildev.getMappedPort(PORT_HTTP);
        
        RestAssured.baseURI = "http://" + smtpHost;
        RestAssured.port = httpPort;
        
        // Setup mocks
        Mockito.when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.SystemEmail)).thenReturn("noreply@example.org");
        BrandingUtil.injectServices(dataverseServiceBean, settingsServiceBean);
        
        // TODO: Once we merge PR 9273 (https://github.com/IQSS/dataverse/pull/9273),
        //       we can use methods to inject the settings in @JvmSetting
        System.setProperty(JvmSettings.MAIL_MTA_HOST.getScopedKey(), smtpHost);
        System.setProperty(JvmSettings.MAIL_MTA_SETTING.insert("port"), smtpPort.toString());
    }
    
    @AfterEach
    void tearDown() {
        System.clearProperty(JvmSettings.MAIL_MTA_HOST.getScopedKey());
        System.clearProperty(JvmSettings.MAIL_MTA_SETTING.insert("port"));
    }
    
    @Test
    //@JvmSetting(key = JvmSettings.MAIL_DEBUG, value = "true")
    void createSessionWithoutAuth() {
        given().when().get("/email")
            .then()
            .statusCode(200)
            .body("size()", is(0));
        
        // given
        Session session = new MailSessionProducer().getSession();
        MailServiceBean mailer = new MailServiceBean(session, settingsServiceBean);
        
        // when
        boolean sent = mailer.sendSystemEmail("test@example.org", "Test", "Test", false);
        
        // then
        assertTrue(sent);
        //RestAssured.get("/email").body().prettyPrint();
        given().when().get("/email")
            .then()
            .statusCode(200)
            .body("size()", is(1))
            .body("[0].subject", equalTo("Test"));
    }
    
}