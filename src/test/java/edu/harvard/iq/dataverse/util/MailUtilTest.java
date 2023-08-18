package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class MailUtilTest {

    UserNotification userNotification = new UserNotification();

    @Mock
    DataverseServiceBean dataverseSvc;
    @Mock
    SettingsServiceBean settingsSvc;
    
    @BeforeEach
    public void setUp() {
        userNotification = new UserNotification();

    }

    @Test
    public void testParseSystemAddress() {
        assertEquals("support@librascholar.edu", MailUtil.parseSystemAddress("support@librascholar.edu").getAddress());
        assertEquals("support@librascholar.edu", MailUtil.parseSystemAddress("LibraScholar Support Team <support@librascholar.edu>").getAddress());
        assertEquals("LibraScholar Support Team", MailUtil.parseSystemAddress("LibraScholar Support Team <support@librascholar.edu>").getPersonal());
        assertEquals("support@librascholar.edu", MailUtil.parseSystemAddress("\"LibraScholar Support Team\" <support@librascholar.edu>").getAddress());
        assertEquals("LibraScholar Support Team", MailUtil.parseSystemAddress("\"LibraScholar Support Team\" <support@librascholar.edu>").getPersonal());
        assertEquals(null, MailUtil.parseSystemAddress(null));
        assertEquals(null, MailUtil.parseSystemAddress(""));
        assertEquals(null, MailUtil.parseSystemAddress("LibraScholar Support Team support@librascholar.edu"));
        assertEquals(null, MailUtil.parseSystemAddress("\"LibraScholar Support Team <support@librascholar.edu>"));
        assertEquals(null, MailUtil.parseSystemAddress("support1@dataverse.org, support@librascholar.edu"));
    }

    @Test
    @Order(1)
    public void testSubjectCreateAccount() {
        Mockito.when(settingsSvc.getValueForKey(SettingsServiceBean.Key.InstallationName)).thenReturn(null);
        //And configure the mock DataverseService to pretend the root collection name is as shown
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn("LibraScholar");
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);

        userNotification.setType(UserNotification.Type.CREATEACC);
        assertEquals("LibraScholar: Your account has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }

    @Test
    public void testSubjectAssignRole() {
        userNotification.setType(UserNotification.Type.ASSIGNROLE);
        assertEquals("LibraScholar: You have been assigned a role", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }

    @Test
    public void testSubjectCreateDataverse() {
        userNotification.setType(UserNotification.Type.CREATEDV);
        assertEquals("LibraScholar: Your dataverse has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectRevokeRole() {
        userNotification.setType(UserNotification.Type.REVOKEROLE);
        assertEquals("LibraScholar: Your role has been revoked", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectRequestFileAccess() {
        userNotification.setType(UserNotification.Type.REQUESTFILEACCESS);
        assertEquals("LibraScholar: Access has been requested for a restricted file", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectGrantFileAccess() {
        userNotification.setType(UserNotification.Type.GRANTFILEACCESS);
        assertEquals("LibraScholar: You have been granted access to a restricted file", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectRejectFileAccess() {
        userNotification.setType(UserNotification.Type.REJECTFILEACCESS);
        assertEquals("LibraScholar: Your request for access to a restricted file has been rejected", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }

    @Test
    public void testSubjectCreateDataset() {
        userNotification.setType(UserNotification.Type.CREATEDS);
        assertEquals("LibraScholar: Dataset \"\" has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectSubmittedDS() {
        userNotification.setType(UserNotification.Type.SUBMITTEDDS);
        assertEquals("LibraScholar: Dataset \"\" has been submitted for review", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectPublishedDS() {
        userNotification.setType(UserNotification.Type.PUBLISHEDDS);
        assertEquals("LibraScholar: Dataset \"\" has been published", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectReturnedDS() {
        userNotification.setType(UserNotification.Type.RETURNEDDS);
        assertEquals("LibraScholar: Dataset \"\" has been returned", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectChecksumFail() {
        userNotification.setType(UserNotification.Type.CHECKSUMFAIL);
        assertEquals("LibraScholar: Your upload failed checksum validation", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectFileSystemImport() {
        userNotification.setType(UserNotification.Type.FILESYSTEMIMPORT);
        //TODO SEK add a dataset version to get the Dataset Title which is actually used in the subject now
        assertEquals("Dataset LibraScholar has been successfully uploaded and verified", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
    
    @Test
    public void testSubjectChecksumImport() {
        userNotification.setType(UserNotification.Type.CHECKSUMIMPORT);
        assertEquals("LibraScholar: Your file checksum job has completed", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }

    @Test
    public void testSubjectConfirmEmail() {
        userNotification.setType(UserNotification.Type.CONFIRMEMAIL);
        assertEquals("LibraScholar: Verify your email address", MailUtil.getSubjectTextBasedOnNotification(userNotification, null));
    }
}
