package edu.harvard.iq.dataverse.branding;

import java.io.UnsupportedEncodingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.junit.Test;
import static org.junit.Assert.*;

public class BrandingUtilTest {

    @Test
    public void testGetSupportTeamName() throws AddressException, UnsupportedEncodingException {
        System.out.println("getSupportTeamName");
        assertEquals("Dataverse Support", BrandingUtil.getSupportTeamName(null, null));
        assertEquals("Dataverse Support", BrandingUtil.getSupportTeamName(null, ""));
        assertEquals("LibraScholar Support", BrandingUtil.getSupportTeamName(null, "LibraScholar"));
        assertEquals("LibraScholar Support", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu"), "LibraScholar"));
        assertEquals("LibraScholar Support Team", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team"), "LibraScholar"));
        assertEquals("", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu", ""), "LibraScholar")); // misconfiguration to set to empty string
    }

    @Test
    public void testGetContactHeader() {
        System.out.println("getContactHeader");
        assertEquals("Contact Dataverse Support", BrandingUtil.getContactHeader(null, null));
    }

}
