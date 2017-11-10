package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
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
 * @author pameyer
 */
public class OrcidOAuth2APTest extends OrcidOAuth2AP {
	private static final String RESPONSE_FILE="src/test/resources/xml/oauth2/orcid/v20_response.xml";
	private static final String NO_EMAIL_HAS_AFFILIATION_FILE="src/test/resources/xml/oauth2/orcid/v20_no_email_has_aff.xml";
	private static final String NO_EMAIL_FILE="src/test/resources/xml/oauth2/orcid/v20_no_email.xml";
	private static final String ACTIVITIES_FILE="src/test/resources/xml/oauth2/orcid/v20_activities.xml";
	private static final String RESPONSE;
	private static final String NO_EMAIL_HAS_AFFILIATION;
	private static final String NO_EMAIL;
	private static final String ACTIVITIES;
    
    public OrcidOAuth2APTest() {
        super("", "", "");
    }

    static {
	    RESPONSE = loadResponseXML( RESPONSE_FILE );
	    NO_EMAIL_HAS_AFFILIATION = loadResponseXML( NO_EMAIL_HAS_AFFILIATION_FILE );
	    NO_EMAIL = loadResponseXML( NO_EMAIL_FILE );
        ACTIVITIES = loadResponseXML( ACTIVITIES_FILE );
    }
    /**
     * load XML responses from filesystem (resources).
     * Why? To allow validating against the XSD prior to 1.2 -> 2.0 upgrade
     */
    private static String loadResponseXML(String fname) {
	    String txt = null;
	    try {
		    java.io.File inp = new java.io.File( fname );
		    txt = org.apache.commons.io.FileUtils.readFileToString( inp );
	    } catch( java.io.IOException ie ) {
		    // no-op; assert that the needed strings are not null in tests
	    }
	    return txt;
    }

    @Test
    public void testParseUserResponse() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        assertNotNull( RESPONSE );
        final AbstractOAuth2AuthenticationProvider.ParsedUserResponse actual = sut.parseUserResponse(RESPONSE);

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

        assertEquals("Bob.Doc", actual.username);
        assertEquals("Bob T.", actual.displayInfo.getFirstName());
        assertEquals("Doc", actual.displayInfo.getLastName());
        assertEquals("", actual.displayInfo.getEmailAddress());
        assertEquals("", actual.displayInfo.getAffiliation());
        assertEquals("", actual.displayInfo.getPosition());
        assertEquals(Arrays.asList("").toString(), actual.emails.toString());
    }
    
    @Test
    public void testExtractOrcid() throws OAuth2Exception {
        // sample response from https://members.orcid.org/api/tutorial/read-orcid-records
        String response = "{\"access_token\":\"f5af9f51-07e6-4332-8f1a-c0c11c1e3728\",\"token_type\":\"bearer\",\n" +
                            "\"refresh_token\":\"f725f747-3a65-49f6-a231-3e8944ce464d\",\"expires_in\":631138518,\n" +
                            "\"scope\":\"/read-limited\",\"name\":\"Sofia Garcia\",\"orcid\":\"0000-0001-2345-6789\"}";
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        assertEquals("0000-0001-2345-6789", sut.extractOrcidNumber(response));
    }
    
    @Test( expected=OAuth2Exception.class )
    public void testExtractOrcidBad() throws OAuth2Exception {
        // sample response from https://members.orcid.org/api/tutorial/read-orcid-records
        String response = "{\"access_token\":\"f5af9f51-07e6-4332-8f1a-c0c11c1e3728\",\"token_type\":\"bearer\",\n" +
                            "\"refresh_token\":\"f725f747-3a65-49f6-a231-3e8944ce464d\",\"expires_in\":631138518,\n" +
                            "\"scope\":\"/read-limited\",\"name\":\"Sofia Garcia\"}";
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        sut.extractOrcidNumber(response);
    }
    
    @Test
    public void testParseActivitiesResponse() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        assertNotNull( ACTIVITIES );
        final AuthenticatedUserDisplayInfo actual = sut.parseActivitiesResponse(ACTIVITIES);

        assertEquals("My Organization Name", actual.getAffiliation());
        assertEquals("role, department", actual.getPosition());
    }
    
    @Test
    public void testParseActivitiesResponseNoOrgName() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        assertNotNull( ACTIVITIES );
        
        String responseWithNoOrg = ACTIVITIES.replaceAll("\n","").replaceAll("<employment:organization>.*</employment:organization>", "");
        
        final AuthenticatedUserDisplayInfo actual = sut.parseActivitiesResponse(responseWithNoOrg);

        assertEquals(null, actual.getAffiliation());
        assertEquals("role, department", actual.getPosition());
    }
    
    @Test
    public void testParseActivitiesResponseNoRole() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        assertNotNull( ACTIVITIES );
        
        String responseWithNoOrg = ACTIVITIES.replaceAll("\n","").replaceAll("<employment:role-title>.*</employment:role-title>", "");
        
        final AuthenticatedUserDisplayInfo actual = sut.parseActivitiesResponse(responseWithNoOrg);

        assertEquals("My Organization Name", actual.getAffiliation());
        assertEquals("department", actual.getPosition());
    }
}
