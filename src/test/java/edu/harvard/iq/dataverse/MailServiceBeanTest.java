package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.util.MailUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MailServiceBeanTest {
    
    
    /**
     * We need to reset the BrandingUtil mocks for every test, as we rely on them being set to default.
     */
    @BeforeEach
    private void setup() {
        BrandingUtilTest.setupMocks();
    }
    @AfterAll
    private static void tearDown() {
        BrandingUtilTest.tearDownMocks();
    }
    
    @ParameterizedTest
    @CsvSource(value  = {
        // with name in admin mail address
        "Foo Bar <foo@bar.org>, NULL, NULL, Foo Bar",
        // without name, but installation branding name set
        "dataverse@dataverse.org, NULL, LibraScholar Dataverse, LibraScholar Dataverse",
        // without name, but root dataverse name available
        "dataverse@dataverse.org, NotLibraScholar, NULL, NotLibraScholar",
        // without name, without root dataverse name, without installation name -> default to bundle string.
        "dataverse@dataverse.org, NULL, NULL, Dataverse Installation Admin"
    }, nullValues = {"NULL"})
    void setContactDelegation(String fromMail, String rootDataverseName, String installationName, String expectedStartsWith) {
        BrandingUtilTest.setRootDataverseName(rootDataverseName);
        BrandingUtilTest.setInstallationName(installationName);
        
        InternetAddress fromAddress = MailUtil.parseSystemAddress(fromMail);
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertTrue(fromAddress.getPersonal().startsWith(expectedStartsWith));
            assertTrue(fromAddress.getPersonal().endsWith(" on behalf of user@example.edu"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}