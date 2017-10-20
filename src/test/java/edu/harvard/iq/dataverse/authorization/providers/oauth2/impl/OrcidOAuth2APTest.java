package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author michael
 */
public class OrcidOAuth2APTest extends OrcidOAuth2AP {
	private final String response_file="src/test/resources/xml/oauth2/orcid/v12_response.xml";
	private static String RESPONSE = null;
	private final String no_email_has_affiliation_file="src/test/resources/xml/oauth2/orcid/v12_no_email_has_aff.xml";
	private static String NO_EMAIL_HAS_AFFILIATION = null;
	private final String no_email_file="src/test/resources/xml/oauth2/orcid/v12_no_email.xml";
	private static String NO_EMAIL = null;
    public OrcidOAuth2APTest() {
        super("", "", "");
    }

	

    @Before
    public void setUp()
    {
	    RESPONSE = loadResponseXML( response_file );
	    NO_EMAIL_HAS_AFFILIATION = loadResponseXML( no_email_has_affiliation_file );
	    NO_EMAIL = loadResponseXML( no_email_file );
    }
    /**
     * load XML responses from filesystem (resources).
     * Why? To allow validating against the XSD prior to 1.2 -> 2.0 upgrade
     */
    private static String loadResponseXML(String fname)
    {
	    String txt = null;
	    try
	    {
		    java.io.File inp = new java.io.File( fname );
		    //java.io.InputStream inp = new java.io.FileInputStream( new java.io.File( fname ) );
		    txt = org.apache.commons.io.FileUtils.readFileToString( inp );
	    }
	    catch( java.io.IOException ie )
	    {
		    // no-op; assert that the needed strings are not null in tests
	    }
	    return txt;
    }

    @Test
    public void testParseUserResponse() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
	assertNotNull( RESPONSE );
        System.out.println("withEmailResponse = " + RESPONSE);
        final AbstractOAuth2AuthenticationProvider.ParsedUserResponse actual = sut.parseUserResponse(RESPONSE);

        assertEquals("0000-0003-2591-1698", actual.userIdInProvider);
        //assertEquals("bob", actual.username);
        assertEquals("bdoc", actual.username);
        assertEquals("Bob T.", actual.displayInfo.getFirstName());
        assertEquals("Doc", actual.displayInfo.getLastName());
        assertEquals("bdoc@mailinator.com", actual.displayInfo.getEmailAddress());
        assertEquals("", actual.displayInfo.getAffiliation());
        assertEquals("", actual.displayInfo.getPosition());
        assertEquals(Arrays.asList("bdoc@mailinator.com", "bdoc2@mailinator.com"), actual.emails);

    }

    @Test
    public void testParseUserResponseNoEmailHasAffiliation() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
	assertNotNull( NO_EMAIL_HAS_AFFILIATION );
        final AbstractOAuth2AuthenticationProvider.ParsedUserResponse actual = sut.parseUserResponse(NO_EMAIL_HAS_AFFILIATION);

        assertEquals("0000-0003-2591-1698", actual.userIdInProvider);
        assertEquals("Bob T.", actual.displayInfo.getFirstName());
        assertEquals("Doc", actual.displayInfo.getLastName());
        assertEquals("", actual.displayInfo.getEmailAddress());
        assertEquals("Miskatonic University", actual.displayInfo.getAffiliation());
        assertEquals("", actual.displayInfo.getPosition());
        List<String> emptyList = new ArrayList<>();
        assertEquals(emptyList, actual.emails);
        assertEquals("Bob.Doc", actual.username);

    }

    @Test
    public void testParseUserResponse_noEmails() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
	assertNotNull( NO_EMAIL );
        System.out.println("noEmailResponse = " + NO_EMAIL );
        final AbstractOAuth2AuthenticationProvider.ParsedUserResponse actual = sut.parseUserResponse(NO_EMAIL);

        assertEquals("0000-0003-2591-1698", actual.userIdInProvider);
        assertEquals("Bob.Doc", actual.username);
        assertEquals("Bob T.", actual.displayInfo.getFirstName());
        assertEquals("Doc", actual.displayInfo.getLastName());
        assertEquals("", actual.displayInfo.getEmailAddress());
        assertEquals("", actual.displayInfo.getAffiliation());
        assertEquals("", actual.displayInfo.getPosition());
        assertEquals(Arrays.asList("").toString(), actual.emails.toString());

    }

}
