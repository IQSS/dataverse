package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MailServiceBeanTest {
    
    @Nested
    class Delegation {
        /**
         * We need to reset the BrandingUtil mocks for every test, as we rely on them being set to default.
         */
        @BeforeEach
        void setup() {
            BrandingUtilTest.setupMocks();
        }
        @AfterAll
        static void tearDown() {
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
        void setContactDelegation(String fromMail, String rootDataverseName, String installationName, String expectedStartsWith) throws AddressException {
            BrandingUtilTest.setRootDataverseName(rootDataverseName);
            BrandingUtilTest.setInstallationName(installationName);
            
            InternetAddress fromAddress = new InternetAddress(fromMail);
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
    
    @Nested
    @LocalJvmSettings
    class LookupMailAddresses {
        
        @Mock
        SettingsServiceBean settingsServiceBean;
        
        @InjectMocks
        MailServiceBean mailServiceBean = new MailServiceBean();
        
        private static final String email = "test@example.org";
        
        @Test
        void lookupSystemWithoutAnySetting() {
            assertTrue(mailServiceBean.getSystemAddress().isEmpty());
        }
        
        @Test
        void lookupSystemWithDBOnly() {
            Mockito.when(settingsServiceBean.getValueForKey(Key.SystemEmail)).thenReturn(email);
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        void lookupSystemWithMPConfig() {
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        void lookupSystemWhereMPConfigTakesPrecedenceOverDB() {
            Mockito.lenient().when(settingsServiceBean.getValueForKey(Key.SystemEmail)).thenReturn("foobar@example.org");
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
        }
        
        @Test
        void lookupSupportWithoutAnySetting() {
            assertTrue(mailServiceBean.getSupportAddress().isEmpty());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        void lookupSupportNotSetButWithSystemPresent() {
            assertEquals(email, mailServiceBean.getSupportAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SUPPORT_EMAIL, value = email)
        void lookupSupportWithoutSystemSet() {
            assertTrue(mailServiceBean.getSystemAddress().isEmpty());
            assertEquals(email, mailServiceBean.getSupportAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        @JvmSetting(key = JvmSettings.SUPPORT_EMAIL, value = "support@example.org")
        void lookupSupportSetWithSystemPresent() {
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
            assertEquals("support@example.org", mailServiceBean.getSupportAddress().get().getAddress());
        }
    }
    
    @Nested
    @LocalJvmSettings
    class SendSystemMail {
        @Mock
        SettingsServiceBean settingsServiceBean;
        @InjectMocks
        MailServiceBean mailServiceBean = new MailServiceBean();
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = "")
        void skipIfNoSystemAddress() {
            assertFalse(mailServiceBean.sendSystemEmail("target@example.org", "Test", "Test", false));
        }
    }
    
}