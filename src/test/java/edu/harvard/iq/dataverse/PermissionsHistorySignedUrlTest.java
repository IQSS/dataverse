package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.signing.FixedSigningSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The permissions-history CSV links must be signed with the server secret + the user's token. */
class PermissionsHistorySignedUrlTest {

    private static final String SECRET = "test-only-signing-secret-0123456789";

    private AuthenticationServiceBean authenticationService;
    private DataverseSession session;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        user = new AuthenticatedUser();
        user.setUserIdentifier("alice");
        session = mock(DataverseSession.class);
        when(session.getUser()).thenReturn(user);
        authenticationService = mock(AuthenticationServiceBean.class);
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("alice-token");
        apiToken.setExpireTime(new java.sql.Timestamp(System.currentTimeMillis() + 60_000));
        when(authenticationService.findApiTokenByUser(user)).thenReturn(apiToken);
    }

    private Dataset dataset() {
        Dataset dataset = new Dataset();
        dataset.setId(42L);
        dataset.setGlobalId(new GlobalId(AbstractDOIProvider.DOI_PROTOCOL, "10.5072", "FK2/ABC", "/", AbstractDOIProvider.DOI_RESOLVER_URL, null));
        return dataset;
    }

    @Test
    void filePermissionsPageSignsTheCsvLink() {
        ManageFilePermissionsPage page = new ManageFilePermissionsPage();
        page.session = session;
        page.authenticationService = authenticationService;
        page.signingSecretService = FixedSigningSecret.withSecret(SECRET);
        page.setDataset(dataset());

        String signedUrl = page.getSignedUrlForRAHistoryCsv();

        assertNotNull(signedUrl);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl, "alice", "GET", SECRET + "alice-token"), signedUrl);
    }

    @Test
    void permissionsPageSignsTheCsvLink() {
        ManagePermissionsPage page = new ManagePermissionsPage();
        page.session = session;
        page.authenticationService = authenticationService;
        page.signingSecretService = FixedSigningSecret.withSecret(SECRET);
        page.setDvObject(dataset());

        String signedUrl = page.getSignedUrlForRAHistoryCsv();

        assertNotNull(signedUrl);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl, "alice", "GET", SECRET + "alice-token"), signedUrl);
    }

    @Test
    void noLinkWithoutAValidApiToken() {
        when(authenticationService.findApiTokenByUser(user)).thenReturn(null);
        ManageFilePermissionsPage page = new ManageFilePermissionsPage();
        page.session = session;
        page.authenticationService = authenticationService;
        page.signingSecretService = FixedSigningSecret.withSecret(SECRET);
        page.setDataset(dataset());

        assertNull(page.getSignedUrlForRAHistoryCsv());
    }
}
