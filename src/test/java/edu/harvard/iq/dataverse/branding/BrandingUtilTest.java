package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class BrandingUtilTest {

    @Mock
    DataverseServiceBean dataverseSvc;
    @Mock
    SettingsServiceBean settingsSvc;
    
    @Test
    @Order(1)
    public void testGetInstallationBrandName() {
        System.out.println("testGetInstallationBrandName");
        
        Mockito.when(settingsSvc.getValueForKey(SettingsServiceBean.Key.InstallationName)).thenReturn(null);
        
        //And configure the mock DataverseService to pretend the root collection name is as shown
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn("LibraScholar");
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        
        assertEquals("LibraScholar", BrandingUtil.getInstallationBrandName()); //Defaults to root collection name
        
        Mockito.when(settingsSvc.getValueForKey(SettingsServiceBean.Key.InstallationName)).thenReturn("NotLibraScholar");
        
        assertEquals("NotLibraScholar", BrandingUtil.getInstallationBrandName()); //uses setting
    }

    @Test
    public void testGetSupportTeamName() throws AddressException, UnsupportedEncodingException {
        System.out.println("testGetSupportTeamName");
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn(null);
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        assertEquals("Support", BrandingUtil.getSupportTeamName(null));
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn("");
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        assertEquals("Support", BrandingUtil.getSupportTeamName(null));
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn("LibraScholar");
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        assertEquals("LibraScholar Support", BrandingUtil.getSupportTeamName(null));
        assertEquals("LibraScholar Support", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu")));
        assertEquals("LibraScholar Support Team", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team")));
        assertEquals("", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu", ""))); // misconfiguration to set to empty string
    }

    @Test
    public void testGetSupportEmailAddress() throws AddressException, UnsupportedEncodingException {
        System.out.println("testGetSupportEmailAddress");
        assertEquals(null, BrandingUtil.getSupportTeamEmailAddress(null));
        assertEquals("support@librascholar.edu", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu")));
        assertEquals("support@librascholar.edu", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team")));
        assertEquals("support@librascholar.edu", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu", ""))); // misconfiguration to set to empty string but doesn't matter
        assertEquals(null, BrandingUtil.getSupportTeamEmailAddress(new InternetAddress(null, "LibraScholar Support Team"))); // misconfiguration to set to null
        assertEquals("", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("", "LibraScholar Support Team"))); // misconfiguration to set to empty string
    }

    @Test
    public void testWelcomeInAppNotification() {
        System.out.println("testWelcomeInAppNotification");
        String message = BundleUtil.getStringFromBundle("notification.welcome",
                Arrays.asList(
                		"LibraScholar",
                        "<a href=\"http://guides.dataverse.org/en/4.3/user/index.html\">User Guide</a>",
                        "<a mailto:\"LibraScholar<support@librascholar.edu>\">contact us</a>"
                ));
        System.out.println("message: " + message);
        assertEquals("Welcome to LibraScholar! Get started by adding or finding data. "
                + "Have questions? Check out our <a href=\"http://guides.dataverse.org/en/4.3/user/index.html\">User Guide</a> or <a mailto:\"LibraScholar<support@librascholar.edu>\">contact us</a>.",
                message);
    }

    @Test
    public void testWelcomeEmail() {
        System.out.println("testWelcomeEmail");
        String message = BundleUtil.getStringFromBundle("notification.email.welcome",
                Arrays.asList(
                		"LibraScholar",
                		"documentation",
                        "http://guides.librascholar.edu/en/4.3",
                        "LibraScholar Support",
                        "support@librascholar.edu"
                ));
        System.out.println("message: " + message);
        //QDR - suppress Welcome Email
        assertEquals("",
                message);
    }

    @Test
    public void testEmailClosing() {
        System.out.println("testEmailClosing");
        String message = BundleUtil.getStringFromBundle("notification.email.closing",
                Arrays.asList(
                        "support@librascholar.edu",
                        "LibraScholar Support Team"
                ));
        System.out.println("message: " + message);
        assertEquals("\n\nPlease be in touch with any questions or concerns support@librascholar.edu.\n\nThank you,\nThe LibraScholar Support Team",
                message);
    }

    @Test
    public void testEmailSubject() {
        System.out.println("testEmailSubject");
        String message = BundleUtil.getStringFromBundle("notification.email.create.account.subject",
                Arrays.asList(
                        "LibraScholar"
                ));
        System.out.println("message: " + message);
        assertEquals("",
                message);
    }

    @Test
    public void testGetContactHeader() {
        System.out.println("testGetContactHeader");
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn(null);
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        assertEquals("Contact Support", BrandingUtil.getContactHeader(null));
    }

}
