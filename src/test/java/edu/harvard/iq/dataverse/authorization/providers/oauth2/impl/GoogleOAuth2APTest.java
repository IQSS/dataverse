package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import jakarta.json.Json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class GoogleOAuth2APTest extends GoogleOAuth2AP {

    private static final String GOOGLE_RESPONSE = "{\n"
            + " \"id\": \"107770072576338242009\",\n"
            + " \"email\": \"philipdurbin@gmail.com\",\n"
            + " \"verified_email\": true,\n"
            + " \"name\": \"Philip Durbin\",\n"
            + " \"given_name\": \"Philip\",\n"
            + " \"family_name\": \"Durbin\",\n"
            + " \"link\": \"https://plus.google.com/+PhilipDurbin\",\n"
            + " \"picture\": \"https://lh3.googleusercontent.com/-ybpG0j3J-jI/AAAAAAAAAAI/AAAAAAAAALA/XarE2v_wiic/photo.jpg\",\n"
            + " \"gender\": \"male\",\n"
            + " \"locale\": \"en\"\n"
            + "}";

    public GoogleOAuth2APTest() {
        super("clientId", "clientSecret");
    }

    @Test
    public void testParseUserResponseRealData() {

        AbstractOAuth2AuthenticationProvider.ParsedUserResponse expResult = new AbstractOAuth2AuthenticationProvider.ParsedUserResponse(
                new AuthenticatedUserDisplayInfo("Philip", "Durbin", "philipdurbin@gmail.com", "", ""), null, null);

        AbstractOAuth2AuthenticationProvider.ParsedUserResponse result = parseUserResponse(GOOGLE_RESPONSE);
        assertEquals(expResult.displayInfo, result.displayInfo);
        assertEquals("107770072576338242009", result.userIdInProvider);
        assertEquals("philipdurbin", result.username);
        assertEquals("Philip", result.displayInfo.getFirstName());
        assertEquals("Durbin", result.displayInfo.getLastName());
        assertEquals("philipdurbin@gmail.com", result.displayInfo.getEmailAddress());
        assertEquals("", result.displayInfo.getPosition());
        assertEquals("", result.displayInfo.getAffiliation());

    }

    @Test
    public void testParseUserResponse() {
        AbstractOAuth2AuthenticationProvider.ParsedUserResponse result
                = parseUserResponse(Json.createObjectBuilder()
                        .add("id", "123456")
                        .add("given_name", "Jane")
                        .add("family_name", "Doe")
                        .add("email", "jane@janedoe.com")
                        .build().toString());

        assertEquals("123456", result.userIdInProvider);
        assertEquals("Jane", result.displayInfo.getFirstName());
        assertEquals("Doe", result.displayInfo.getLastName());
        assertEquals("jane@janedoe.com", result.displayInfo.getEmailAddress());
        assertEquals("jane", result.username);

    }

}
