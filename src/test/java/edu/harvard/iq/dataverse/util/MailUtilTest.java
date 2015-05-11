package edu.harvard.iq.dataverse.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MailUtilTest {

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

}
