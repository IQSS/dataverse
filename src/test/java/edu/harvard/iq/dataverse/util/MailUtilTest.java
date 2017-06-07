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
        assertEquals("support@dataverse.org", MailUtil.parseSystemAddress("support@dataverse.org").getAddress());
        assertEquals("support@dataverse.org", MailUtil.parseSystemAddress("Dataverse Support <support@dataverse.org>").getAddress());
        assertEquals("Dataverse Support", MailUtil.parseSystemAddress("Dataverse Support <support@dataverse.org>").getPersonal());
        assertEquals("support@dataverse.org", MailUtil.parseSystemAddress("\"Dataverse Support\" <support@dataverse.org>").getAddress());
        assertEquals("Dataverse Support", MailUtil.parseSystemAddress("\"Dataverse Support\" <support@dataverse.org>").getPersonal());
        assertEquals(null, MailUtil.parseSystemAddress(null));
        assertEquals(null, MailUtil.parseSystemAddress(""));
        assertEquals(null, MailUtil.parseSystemAddress("Dataverse Support support@dataverse.org"));
        assertEquals(null, MailUtil.parseSystemAddress("\"Dataverse Support <support@dataverse.org>"));
        assertEquals(null, MailUtil.parseSystemAddress("support1@dataverse.org, support2@dataverse.org"));
    }

    @Test
    public void testSubjectCreateAccount() {
        userNotification.setType(UserNotification.Type.CREATEACC);
        assertEquals("LibraScholar: Your account has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName));
    }

    @Test
    public void testSubjectAssignRole() {
        userNotification.setType(UserNotification.Type.ASSIGNROLE);
        // FIXME: Instead of "Dataverse:" it should be "LibraScholar:"
        assertEquals("Dataverse: You have been assigned a role", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName));
    }

    @Test
    public void testSubjectCreateDataverse() {
        userNotification.setType(UserNotification.Type.CREATEDV);
        assertEquals("LibraScholar: Your dataverse has been created", MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName));
    }
}
