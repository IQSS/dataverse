package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.MailUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MailServiceBeanTest {

    @Mock
    DataverseServiceBean dataverseService;
    @Mock
    SettingsServiceBean settingsService;

    @Test
    void setContactDelegation_withName() {
        initBrandingUtilWithRootDataverse(null, null);

        InternetAddress fromAddress = MailUtil.parseSystemAddress("Foo Bar <foo@bar.org>");
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertEquals(
                "Foo Bar on behalf of user@example.edu",
                fromAddress.getPersonal()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    void setContactDelegation_withoutName_fromInstallationName() {
        initBrandingUtilWithRootDataverse("LibraScholar Dataverse", null);

        InternetAddress fromAddress = MailUtil.parseSystemAddress("dataverse@dataverse.org");
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertEquals(
                "LibraScholar Dataverse on behalf of user@example.edu",
                fromAddress.getPersonal()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    void setContactDelegation_withoutName_fromBranding() {
        initBrandingUtilWithRootDataverse(null, "LibraScholar");

        InternetAddress fromAddress = MailUtil.parseSystemAddress("dataverse@dataverse.org");
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertEquals(
                "LibraScholar on behalf of user@example.edu",
                fromAddress.getPersonal()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    void setContactDelegation_withoutName_fromBundle() {
        initBrandingUtilWithRootDataverse(null, null);

        InternetAddress fromAddress = MailUtil.parseSystemAddress("dataverse@dataverse.org");
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertEquals(
                "Dataverse Installation Admin on behalf of user@example.edu",
                fromAddress.getPersonal()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void initBrandingUtilWithRootDataverse(String installationName, String rootDataverseName) {
        Mockito.lenient()
            .when(settingsService.getValueForKey(SettingsServiceBean.Key.InstallationName))
            .thenReturn(installationName);
        Mockito.lenient()
            .when(dataverseService.getRootDataverseName())
            .thenReturn(rootDataverseName);
        BrandingUtil.injectServices(dataverseService, settingsService);
    }
}