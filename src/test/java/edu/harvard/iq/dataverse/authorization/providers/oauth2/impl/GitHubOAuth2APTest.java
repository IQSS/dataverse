package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import org.junit.jupiter.api.Test;

public class GitHubOAuth2APTest extends GitHubOAuth2AP {

    private static final String GITHUB_RESPONSE = "{\n"
            + "  \"login\": \"pdurbin\",\n"
            + "  \"id\": 21006,\n"
            + "  \"avatar_url\": \"https://avatars.githubusercontent.com/u/21006?v=3\",\n"
            + "  \"gravatar_id\": \"\",\n"
            + "  \"url\": \"https://api.github.com/users/pdurbin\",\n"
            + "  \"html_url\": \"https://github.com/pdurbin\",\n"
            + "  \"followers_url\": \"https://api.github.com/users/pdurbin/followers\",\n"
            + "  \"following_url\": \"https://api.github.com/users/pdurbin/following{/other_user}\",\n"
            + "  \"gists_url\": \"https://api.github.com/users/pdurbin/gists{/gist_id}\",\n"
            + "  \"starred_url\": \"https://api.github.com/users/pdurbin/starred{/owner}{/repo}\",\n"
            + "  \"subscriptions_url\": \"https://api.github.com/users/pdurbin/subscriptions\",\n"
            + "  \"organizations_url\": \"https://api.github.com/users/pdurbin/orgs\",\n"
            + "  \"repos_url\": \"https://api.github.com/users/pdurbin/repos\",\n"
            + "  \"events_url\": \"https://api.github.com/users/pdurbin/events{/privacy}\",\n"
            + "  \"received_events_url\": \"https://api.github.com/users/pdurbin/received_events\",\n"
            + "  \"type\": \"User\",\n"
            + "  \"site_admin\": false,\n"
            + "  \"name\": \"Philip Durbin\",\n"
            + "  \"company\": \"Harvard\",\n"
            + "  \"blog\": \"http://greptilian.com\",\n"
            + "  \"location\": \"Boston\",\n"
            + "  \"email\": \"philipdurbin@gmail.com\",\n"
            + "  \"hireable\": null,\n"
            + "  \"bio\": null,\n"
            + "  \"public_repos\": 61,\n"
            + "  \"public_gists\": 16,\n"
            + "  \"followers\": 83,\n"
            + "  \"following\": 146,\n"
            + "  \"created_at\": \"2008-08-18T12:58:23Z\",\n"
            + "  \"updated_at\": \"2016-12-10T17:20:52Z\"\n"
            + "}";

    public GitHubOAuth2APTest() {
        super("clientId", "clientSecret");
    }

    @Test
    public void testParseUserResponse() {
        AbstractOAuth2AuthenticationProvider.ParsedUserResponse expResult = new AbstractOAuth2AuthenticationProvider.ParsedUserResponse(
                new AuthenticatedUserDisplayInfo("Philip", "Durbin", "philipdurbin@gmail.com", "Harvard", ""),
                "1938468",
                "jane_doe"
        );
        AbstractOAuth2AuthenticationProvider.ParsedUserResponse result = parseUserResponse(GITHUB_RESPONSE);

        assertEquals(expResult.displayInfo, result.displayInfo);
        assertEquals("21006", result.userIdInProvider);

    }
    
    public OAuth2UserRecord getExampleUserRecord() {
        ParsedUserResponse res = parseUserResponse(GITHUB_RESPONSE);
        return new OAuth2UserRecord(this.getId(), res.userIdInProvider, res.username, null, res.displayInfo, res.emails);
    }

}
