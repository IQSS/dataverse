package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import javax.json.Json;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GoogleOAuth2APTest extends GoogleOAuth2AP {

    public GoogleOAuth2APTest() {
        super("clientId", "clientSecret");
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
