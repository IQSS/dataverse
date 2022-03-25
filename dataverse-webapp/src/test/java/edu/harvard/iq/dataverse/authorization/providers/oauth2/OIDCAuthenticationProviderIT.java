package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCAuthenticationProviderIT {

    private static final int MOCK_SERVER_PORT = 7117;
    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String ISSUER_URL = "http://localhost:" + MOCK_SERVER_PORT;

    private OIDCAuthenticationProvider provider;

    private WireMockServer server;

    @Mock
    OIDCValidator validator;

    @BeforeEach
    void setUp() throws AuthorizationSetupException, OAuth2Exception {
        setUpMockServer();

        provider = new OIDCAuthenticationProvider(CLIENT_ID, CLIENT_SECRET, ISSUER_URL, validator);
        provider.initialize();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should create authorization URL")
    void createAuthorizationUrl() {

        // given
        String state = String.valueOf(new Random().nextInt());
        String redirectUri = "redirect-uri";

        // when
        String authorizationUrl = provider.createAuthorizationUrl(state, redirectUri);

        // then
        assertThat(authorizationUrl).isEqualTo(String.format(
                "http://localhost:%d/protocol/openid-connect/auth" +
                "?response_type=code&redirect_uri=%s" +
                "&state=%s&client_id=client-id&scope=openid+profile+email", MOCK_SERVER_PORT, redirectUri, state));
    }

    @Test
    @DisplayName("Should use received token to retrieve user data")
    void getUserRecord() throws OAuth2Exception {

        // given
        String code = String.valueOf(new Random().nextInt());
        String redirectUri = "redirect-uri";

        IDTokenClaimsSet claims = mock(IDTokenClaimsSet.class);
        Subject subject = new Subject("subject");
        when(claims.getSubject()).thenReturn(subject);
        when(validator.validateIDToken(any())).thenReturn(claims);

        WireMock.stubFor(WireMock.post("/protocol/openid-connect/token")
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                                "\"access_token\": \"access-token\"," +
                                "\"token_type\": \"Bearer\"," +
                                "\"refresh_token\": \"refresh-token\"," +
                                "\"expires_in\": 3600" +
                                "}")));

        WireMock.stubFor(WireMock.get("/protocol/openid-connect/userinfo")
                .withHeader("Authorization", WireMock.equalTo("Bearer access-token"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                                "\"sub\": \"248289761001\"," +
                                "\"name\": \"Zenon Nonez\"," +
                                "\"given_name\": \"Zenon\"," +
                                "\"family_name\": \"Nonez\"," +
                                "\"preferred_username\": \"zenon.nonez\"," +
                                "\"email\": \"zenon.nonez@icm.edu.pl\"" +
                                "}")));

        // when
        ExternalIdpUserRecord userData = provider.getUserRecord(code, null, redirectUri);

        // then
        assertThat(userData.getUsername()).isEqualTo("zenon.nonez");
        AuthenticatedUserDisplayInfo displayInfo = userData.getDisplayInfo();
        assertThat(displayInfo.getFirstName()).isEqualTo("Zenon");
        assertThat(displayInfo.getLastName()).isEqualTo("Nonez");
        assertThat(displayInfo.getEmailAddress()).isEqualTo("zenon.nonez@icm.edu.pl");
    }

    // -------------------- PRIVATE --------------------

    private void setUpMockServer() {
        server = new WireMockServer(MOCK_SERVER_PORT);
        server.start();
        WireMock.configureFor(MOCK_SERVER_PORT);
        setUpConfigurationEndpoint();
    }

    private void setUpConfigurationEndpoint() {
        String configurationJson = readConfigurationJson();

        WireMock.stubFor(WireMock.get("/.well-known/openid-configuration")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(configurationJson)));
    }

    private String readConfigurationJson() {
        try {
            return IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("json/oidc/oidc_conf.json"), Charset.defaultCharset())
                    .replaceAll("____", String.valueOf(MOCK_SERVER_PORT));
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}