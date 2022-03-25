package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;

import java.io.IOException;

public interface OAuth2AuthenticationProvider extends AuthenticationProvider {

    /**
     * Returns the title that will be shown on login button on login page.
     */
    String getTitle();

    /**
     * Returns OAuth2 clientSecret.
     */
    String getClientSecret();

    /**
     * Creates URL for starting authorization process.
     * @param state safely generated string for securing data exchange
     * @param redirectUrl URL to follow after authorization success
     */
    String createAuthorizationUrl(String state, String redirectUrl);

    /**
     * Obtains user data from authorization provider.
     * @param code code received from authorization provider
     * @param state safely generated string for securing data exchange
     * @param redirectUrl URL to follow after authorization success
     */
    ExternalIdpUserRecord getUserRecord(String code, String state, String redirectUrl) throws IOException, OAuth2Exception;

    /**
     * Additional initialization of provider that should be executed before use.
     */
    default void initialize() throws AuthorizationSetupException { }
}
