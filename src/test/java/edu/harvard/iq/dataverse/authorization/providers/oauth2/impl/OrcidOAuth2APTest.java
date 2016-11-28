package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class OrcidOAuth2APTest extends OrcidOAuth2AP {

    private static final String RESPONSE
            = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<orcid-message xmlns=\"http://www.orcid.org/ns/orcid\">\n"
            + "    <message-version>1.2</message-version>\n"
            + "    <orcid-profile type=\"user\">\n"
            + "        <orcid-identifier>\n"
            + "            <uri>http://sandbox.orcid.org/0000-0002-3283-0661</uri>\n"
            + "            <path>0000-0002-3283-0661</path>\n"
            + "            <host>sandbox.orcid.org</host>\n"
            + "        </orcid-identifier>\n"
            + "        <orcid-preferences>\n"
            + "            <locale>en</locale>\n"
            + "        </orcid-preferences>\n"
            + "        <orcid-history>\n"
            + "            <creation-method>Member-referred</creation-method>\n"
            + "            <submission-date>2016-10-12T21:59:25.760Z</submission-date>\n"
            + "            <last-modified-date>2016-10-16T21:11:33.032Z</last-modified-date>\n"
            + "            <claimed>true</claimed>\n"
            + "            <verified-email>false</verified-email>\n"
            + "            <verified-primary-email>false</verified-primary-email>\n"
            + "        </orcid-history>\n"
            + "        <orcid-bio>\n"
            + "            <personal-details>\n"
            + "                <given-names>Pete K.</given-names>\n"
            + "                <family-name>Dataversky</family-name>\n"
            + "            </personal-details>\n"
            + "            <contact-details>"
            + "                <email primary=\"false\" current=\"true\" verified=\"false\" visibility=\"limited\" source=\"0000-0002-3283-0661\">pete2@mailinator.com</email>"
            + "                <email primary=\"true\" current=\"true\" verified=\"false\" visibility=\"public\" source=\"0000-0002-3283-0661\">pete@mailinator.com</email>"
            + "            </contact-details>"
            + "        </orcid-bio>\n"
            + "    </orcid-profile>\n"
            + "</orcid-message>";

    public OrcidOAuth2APTest() {
        super("", "", "");
    }

    @Test
    public void testParseUserResponse() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        System.out.println("withEmailResponse = " + RESPONSE);
        final AbstractOAuth2AuthenticationProvider.ParsedUserResponse actual = sut.parseUserResponse(RESPONSE);

        assertEquals("0000-0002-3283-0661", actual.userIdInProvider);
        assertEquals("pete", actual.username);
        assertEquals("Pete K.", actual.displayInfo.getFirstName());
        assertEquals("Dataversky", actual.displayInfo.getLastName());
        assertEquals("pete@mailinator.com", actual.displayInfo.getEmailAddress());
        assertEquals("", actual.displayInfo.getAffiliation());
        assertEquals("", actual.displayInfo.getPosition());
        assertEquals(Arrays.asList("pete@mailinator.com", "pete2@mailinator.com"), actual.emails);

    }

    @Test
    public void testParseUserResponse_noEmails() {
        OrcidOAuth2AP sut = new OrcidOAuth2AP("clientId", "clientSecret", "userEndpoint");
        String noEmailResponse = RESPONSE.replaceAll("<contact-details>.*</contact-details>", "");
        System.out.println("noEmailResponse = " + noEmailResponse);
        final AbstractOAuth2AuthenticationProvider.ParsedUserResponse actual = sut.parseUserResponse(noEmailResponse);

        assertEquals("0000-0002-3283-0661", actual.userIdInProvider);
        assertEquals("Pete.Dataversky", actual.username);
        assertEquals("Pete K.", actual.displayInfo.getFirstName());
        assertEquals("Dataversky", actual.displayInfo.getLastName());
        assertEquals("", actual.displayInfo.getEmailAddress());
        assertEquals("", actual.displayInfo.getAffiliation());
        assertEquals("", actual.displayInfo.getPosition());
        assertEquals(Arrays.asList("").toString(), actual.emails.toString());

    }

}
