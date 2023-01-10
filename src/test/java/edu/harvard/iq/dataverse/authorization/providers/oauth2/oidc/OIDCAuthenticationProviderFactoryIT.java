package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("testcontainers")
@Testcontainers
class OIDCAuthenticationProviderFactoryIT {
    
    static final String clientId = "oidc-client";
    static final String clientSecret = "ss6gE8mODCDfqesQaSG3gwUwZqZt547E";
    static final String realm = "oidc-realm";
    
    @Container
    static KeycloakContainer keycloakContainer = new KeycloakContainer().withRealmImportFile("keycloak/oidc-realm.json");
    
    // simple method to retrieve the issuer URL, referenced to by @JvmSetting annotations
    private static String getAuthUrl() {
        return keycloakContainer.getAuthServerUrl() + "realms/" + realm;
    }
    
    @Test
    @JvmSetting(key = JvmSettings.OIDC_CLIENT_ID, value = clientId)
    @JvmSetting(key = JvmSettings.OIDC_CLIENT_SECRET, value = clientSecret)
    @JvmSetting(key = JvmSettings.OIDC_AUTH_SERVER_URL, method = "getAuthUrl")
    void testCreateProvider() throws Exception {
        OIDCAuthProvider oidcAuthProvider = (OIDCAuthProvider) OIDCAuthenticationProviderFactory.buildFromSettings();
        assertTrue(oidcAuthProvider.getMetadata().getTokenEndpointURI().toString().startsWith(keycloakContainer.getAuthServerUrl()));
    }
}