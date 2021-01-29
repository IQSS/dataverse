package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.UserNotification;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

public class MailUtilTest {

    private String rootDataverseName;
    UserNotification userNotification = new UserNotification();

    @Before
    public void setUp() {
        rootDataverseName = "LibraScholar";
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
    public void testSubjectCreateAccount() {
        userNotification.setType(UserNotification.Type.CREATEACC);
        assertEquals("LibraScholar: Your account has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }

    @Test
    public void testSubjectAssignRole() {
        userNotification.setType(UserNotification.Type.ASSIGNROLE);
        assertEquals("LibraScholar: You have been assigned a role", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }

    @Test
    public void testSubjectCreateDataverse() {
        userNotification.setType(UserNotification.Type.CREATEDV);
        assertEquals("LibraScholar: Your dataverse has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectRevokeRole() {
        userNotification.setType(UserNotification.Type.REVOKEROLE);
        assertEquals("LibraScholar: Your role has been revoked", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectRequestFileAccess() {
        userNotification.setType(UserNotification.Type.REQUESTFILEACCESS);
        assertEquals("LibraScholar: Access has been requested for a restricted file", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectGrantFileAccess() {
        userNotification.setType(UserNotification.Type.GRANTFILEACCESS);
        assertEquals("LibraScholar: You have been granted access to a restricted file", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectRejectFileAccess() {
        userNotification.setType(UserNotification.Type.REJECTFILEACCESS);
        assertEquals("LibraScholar: Your request for access to a restricted file has been rejected", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }

    @Test
    public void testSubjectCreateDataset() {
        userNotification.setType(UserNotification.Type.CREATEDS);
        assertEquals("LibraScholar: Your dataset has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectSubmittedDS() {
        userNotification.setType(UserNotification.Type.SUBMITTEDDS);
        assertEquals("LibraScholar: Your dataset has been submitted for review", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectPublishedDS() {
        userNotification.setType(UserNotification.Type.PUBLISHEDDS);
        assertEquals("LibraScholar: Your dataset has been published", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectReturnedDS() {
        userNotification.setType(UserNotification.Type.RETURNEDDS);
        assertEquals("LibraScholar: Your dataset has been returned", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectChecksumFail() {
        userNotification.setType(UserNotification.Type.CHECKSUMFAIL);
        assertEquals("LibraScholar: Your upload failed checksum validation", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
    
    @Test
    public void testSubjectFileSystemImport() {
        userNotification.setType(UserNotification.Type.FILESYSTEMIMPORT);
        //TODO SEK add a dataset version to get the Dataset Title which is actually used in the subject now
        assertEquals("Dataset LibraScholar has been successfully uploaded and verified", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName , null));
    }
    
    @Test
    public void testSubjectChecksumImport() {
        userNotification.setType(UserNotification.Type.CHECKSUMIMPORT);
        assertEquals("LibraScholar: Your file checksum job has completed", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }

    @Test
    public void testSubjectConfirmEmail() {
        userNotification.setType(UserNotification.Type.CONFIRMEMAIL);
        assertEquals("LibraScholar: Verify your email address", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null));
    }
}
