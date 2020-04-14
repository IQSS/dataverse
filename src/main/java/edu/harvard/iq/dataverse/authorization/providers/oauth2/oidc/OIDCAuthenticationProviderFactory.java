package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2AuthenticationProviderFactory;

import java.util.Map;

public class OIDCAuthenticationProviderFactory implements AuthenticationProviderFactory {
    
    /**
     * The alias of a factory. Has to be unique in the system.
     * @return The alias of the factory.
     */
    @Override
    public String getAlias() {
        return "oidc";
    }
    
    /**
     * @return A human readable display string describing this factory.
     */
    @Override
    public String getInfo() {
        return "Factory for Open ID Connect providers";
    }
    
    /**
     * Instantiates an {@link AuthenticationProvider} based on the row passed.
     * @param aRow The row on which the created provider is based.
     * @return The provider
     * @throws AuthorizationSetupException If {@code aRow} contains malformed data.
     */
    @Override
    public AuthenticationProvider buildProvider( AuthenticationProviderRow aRow ) throws AuthorizationSetupException {
        Map<String, String> factoryData = OAuth2AuthenticationProviderFactory.parseFactoryData(aRow.getFactoryData());
        
        OIDCAuthProvider oidc = new OIDCAuthProvider(factoryData.get("clientId"), factoryData.get("clientSecret"), factoryData.get("issuer"));
        oidc.setId(aRow.getId());
        oidc.setTitle(aRow.getTitle());
        oidc.setSubTitle(aRow.getSubtitle());
        
        return oidc;
    }
}
