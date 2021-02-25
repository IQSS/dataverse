package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.persistence.user.AuthenticationProviderRow;

import java.util.Map;

public class OIDCAuthenticationProviderFactory implements AuthenticationProviderFactory {

    // -------------------- LOGIC --------------------

    @Override
    public String getAlias() {
        return "OIDC";
    }

    @Override
    public String getInfo() {
        return "Factory for creating OpenID Connect providers";
    }

    @Override
    public AuthenticationProvider buildProvider(AuthenticationProviderRow row) {
        Map<String, String> factoryData = OAuth2AuthenticationProviderFactory.parseFactoryData(row.getFactoryData());

        OIDCAuthenticationProvider provider = new OIDCAuthenticationProvider(
                factoryData.get("clientId"), factoryData.get("clientSecret"), factoryData.get("issuer"));
        provider.setId(row.getId());
        provider.setTitle(row.getTitle());
        provider.setSubTitle(row.getSubtitle());

        return provider;
    }
}
