package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates OAuth2 authentication providers based on rows from the database.
 *
 * @author michael
 */
public class OAuth2AuthenticationProviderFactory implements AuthenticationProviderFactory {

    private static interface ProviderBuilder {

        AbstractOAuth2AuthenticationProvider build(AuthenticationProviderRow aRow, Map<String, String> factoryData);

    }

    private final Map<String, ProviderBuilder> builders = new HashMap<>();

    public OAuth2AuthenticationProviderFactory() {
        builders.put("github", (row, data) -> readRow(row, new GitHubOAuth2AP(data.get("clientId"), data.get("clientSecret"))));
        builders.put("google", (row, data) -> readRow(row, new GoogleOAuth2AP(data.get("clientId"), data.get("clientSecret"))));
        builders.put("orcid", (row, data)  -> readRow(row, new OrcidOAuth2AP(data.get("clientId"), data.get("clientSecret"), data.get("userEndpoint"))));
    }

    @Override
    public String getAlias() {
        return "oauth2";
    }

    @Override
    public AuthenticationProvider buildProvider(AuthenticationProviderRow aRow) throws AuthorizationSetupException {
        Map<String,String> factoryData = parseFactoryData(aRow.getFactoryData());
        final String type = factoryData.get("type");
        if ( type == null ) {
            throw new AuthorizationSetupException("Authentication provider row with id " + aRow.getId() 
                    + " describes an OAuth2 provider but does not provide a type. Available types are " + builders.keySet() );
        }
        ProviderBuilder pb = builders.get(type);
        if ( pb == null ) {
            throw new AuthorizationSetupException("Authentication provider row with id " + aRow.getId() 
                    + " describes an OAuth2 provider of type " + type +". This type is not supported."
                    + " Available types are " + builders.keySet() );
        }
        return pb.build(aRow, factoryData);
    }

    @Override
    public String getInfo() {
        return "Factory for OAuth2 identity providers.";
    }

    /**
     * Expected map format.: {@code name: value|name: value|...}
     *
     * @param factoryData
     * @return A map of the factory data.
     */
    protected Map<String, String> parseFactoryData(String factoryData) {
        return Arrays.asList(factoryData.split("\\|")).stream()
                .map(s -> s.split(":", 2))
                .filter(p -> p.length == 2)
                .collect(Collectors.toMap(kv -> kv[0].trim(), kv -> kv[1].trim()));
    }

    protected AbstractOAuth2AuthenticationProvider readRow(AuthenticationProviderRow row, AbstractOAuth2AuthenticationProvider prv) {
        prv.setId(row.getId());
        prv.setTitle(row.getTitle());
        prv.setSubTitle(row.getSubtitle());

        return prv;
    }
}
