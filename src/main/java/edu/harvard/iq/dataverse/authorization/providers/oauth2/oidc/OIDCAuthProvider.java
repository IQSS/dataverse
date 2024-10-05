package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.scribejava.core.builder.api.DefaultApi20;

import edu.harvard.iq.dataverse.api.OpenIDConfigBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;

/**
 * TODO: this should not EXTEND, but IMPLEMENT the contract to be used in
 * {@link edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean}
 */
public class OIDCAuthProvider extends AbstractOAuth2AuthenticationProvider {
    protected String id = "oidc";
    protected String title = "Open ID Connect";

    final String aClientId;
    final String aClientSecret;
    final String issuerEndpointURL;

    /**
     * To be absolutely sure this may not be abused to DDoS us and not let unused
     * verifiers rot, use an evicting cache implementation and not a standard map.
     */
    private final Cache<String, UserRecordIdentifier> verifierCache = Caffeine.newBuilder()
            .maximumSize(JvmSettings.OIDC_BEARER_CACHE_MAXSIZE.lookup(Integer.class))
            .expireAfterWrite(
                    Duration.of(JvmSettings.OIDC_BEARER_CACHE_MAXAGE.lookup(Integer.class), ChronoUnit.SECONDS))
            .build();

    public OIDCAuthProvider(String aClientId, String aClientSecret, String issuerEndpointURL) {
        this.aClientId = aClientId;
        this.aClientSecret = aClientSecret;
        this.issuerEndpointURL = issuerEndpointURL;
    }

    public String getIssuerEndpointURL() {
        return this.issuerEndpointURL;
    }

    public void setConfig(final OpenIDConfigBean openIdConfigBean) {
        openIdConfigBean.setTarget("JSF");
        openIdConfigBean.setClientId(aClientId);
        openIdConfigBean.setClientSecret(aClientSecret);
        openIdConfigBean.setProviderURI(issuerEndpointURL);
    }

    /**
     * Although this is defined in
     * {@link edu.harvard.iq.dataverse.authorization.AuthenticationProvider},
     * this needs to be present due to bugs in ELResolver (has been modified for
     * Spring).
     * TODO: for the future it might be interesting to make this configurable via
     * the provider JSON (it's used for ORCID!)
     * 
     * @see <a href="https://issues.jboss.org/browse/JBEE-159">JBoss Issue 159</a>
     * @see <a href="https://github.com/eclipse-ee4j/el-ri/issues/43">Jakarta EE Bug
     *      43</a>
     * @return false
     */
    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    /**
     * TODO: remove when refactoring package and
     * {@link AbstractOAuth2AuthenticationProvider}
     */
    @Override
    public DefaultApi20 getApiInstance() {
        throw new UnsupportedOperationException("OIDC provider cannot provide a ScribeJava API instance object");
    }

    /**
     * TODO: remove when refactoring package and
     * {@link AbstractOAuth2AuthenticationProvider}
     */
    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        throw new UnsupportedOperationException("OIDC provider uses the SDK to parse the response.");
    }

    /**
     * Trades an access token for an email (if found).
     *
     * @param accessToken The access token
     * @return Returns an email if found
     */
    public UserRecordIdentifier getUserRecordIdentifier(String accessToken) {
        return this.verifierCache.getIfPresent(accessToken);
    }

    /**
     * Stores an email in cache for an access token.
     *
     * @param accessToken The access token
     * @param email       The email
     */
    public void storeBearerToken(String accessToken, UserRecordIdentifier userRecordIdentifier) {
        this.verifierCache.put(accessToken, userRecordIdentifier);
    }
}
