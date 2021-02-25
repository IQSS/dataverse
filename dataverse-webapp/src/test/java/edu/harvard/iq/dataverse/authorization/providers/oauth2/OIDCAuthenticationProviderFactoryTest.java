package edu.harvard.iq.dataverse.authorization.providers.oauth2;


import edu.harvard.iq.dataverse.persistence.user.AuthenticationProviderRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OIDCAuthenticationProviderFactoryTest {

    @Test
    @DisplayName("Should create provider form the given row")
    void buildProvider() {
        // given
        AuthenticationProviderRow row = new AuthenticationProviderRow();
        row.setId("oidc-provider-id");
        row.setTitle("oidc-provider-title");
        row.setSubtitle("oidc-provider-subtitle");
        row.setFactoryData("clientId:client-id|clientSecret:client-secret|issuer:issuer-url");

        // when
        OIDCAuthenticationProvider provider = (OIDCAuthenticationProvider) new OIDCAuthenticationProviderFactory().buildProvider(row);

        // then
        assertThat(provider.getId()).isEqualTo("oidc-provider-id");
        assertThat(provider.getTitle()).isEqualTo("oidc-provider-title");
        assertThat(provider.getSubTitle()).isEqualTo("oidc-provider-subtitle");
        assertThat(provider.getClientSecret()).isEqualTo("client-secret");
    }
}