package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class GitHubOAuth2IdpTest extends GitHubOAuth2AP {
    
    private static final String GITHUB_RESPONSE = "{\n" +
"    \"avatar_url\": \"https://avatars.githubusercontent.com/u/9999999?v=3\",\n" +
"    \"bio\": null,\n" +
"    \"blog\": \"janedoe.com\",\n" +
"    \"collaborators\": 700,\n" +
"    \"company\": \"ACME Sprokets, Inc.\",\n" +
"    \"created_at\": \"2016-07-01T14:14:09Z\",\n" +
"    \"disk_usage\": 78664,\n" +
"    \"email\": \"jane@janedoe.com\",\n" +
"    \"events_url\": \"https://api.github.com/users/jane_doe/events{/privacy}\",\n" +
"    \"followers\": 10,\n" +
"    \"followers_url\": \"https://api.github.com/users/jane_doe/followers\",\n" +
"    \"following\": 1,\n" +
"    \"following_url\": \"https://api.github.com/users/jane_doe/following{/other_user}\",\n" +
"    \"gists_url\": \"https://api.github.com/users/jane_doe/gists{/gist_id}\",\n" +
"    \"gravatar_id\": \"\",\n" +
"    \"hireable\": null,\n" +
"    \"html_url\": \"https://github.com/jane_doe\",\n" +
"    \"id\": 1938468,\n" +
"    \"location\": \"Sea of Holes\",\n" +
"    \"login\": \"jane_doe\",\n" +
"    \"name\": \"Jane\",\n" +
"    \"organizations_url\": \"https://api.github.com/users/jane_doe/orgs\",\n" +
"    \"owned_private_repos\": 4,\n" +
"    \"plan\": {\n" +
"        \"collaborators\": 0,\n" +
"        \"name\": \"personal\",\n" +
"        \"private_repos\": 9999,\n" +
"        \"space\": 976562499\n" +
"    },\n" +
"    \"private_gists\": 0,\n" +
"    \"public_gists\": 2,\n" +
"    \"public_repos\": 14,\n" +
"    \"received_events_url\": \"https://api.github.com/users/jane_doe/received_events\",\n" +
"    \"repos_url\": \"https://api.github.com/users/jane_doe/repos\",\n" +
"    \"site_admin\": false,\n" +
"    \"starred_url\": \"https://api.github.com/users/jane_doe/starred{/owner}{/repo}\",\n" +
"    \"subscriptions_url\": \"https://api.github.com/users/jane_doe/subscriptions\",\n" +
"    \"total_private_repos\": 5,\n" +
"    \"type\": \"User\",\n" +
"    \"updated_at\": \"2016-09-07T07:34:54Z\",\n" +
"    \"url\": \"https://api.github.com/users/jane_doe\"\n" +
"}";

    public GitHubOAuth2IdpTest() {
        super("clientId", "clientSecret");
    }
    
    /**
     * Test of parseUserResponse method, of class GitHubOAuth2Idp.
     */
    @Test
    public void testParseUserResponse() {
        AbstractOAuth2AuthenticationProvider.ParsedUserResponse expResult = new AbstractOAuth2AuthenticationProvider.ParsedUserResponse(
                new AuthenticatedUserDisplayInfo("Jane", "", "jane@janedoe.com", "ACME Sprokets, Inc.", ""),
                "1938468",
                "jane_doe"
        );
        AbstractOAuth2AuthenticationProvider.ParsedUserResponse result = parseUserResponse(GITHUB_RESPONSE);
        
        assertEquals(expResult.displayInfo, result.displayInfo);
        assertEquals("1938468", result.userIdInProvider);
        
        
    }
    
}
