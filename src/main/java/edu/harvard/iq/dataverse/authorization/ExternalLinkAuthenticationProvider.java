package edu.harvard.iq.dataverse.authorization;

import java.net.URL;

/**
 * An authentication provider that authenticates users by sending them to another website (e.g Shibboleth).
 * @author michael
 */
public interface ExternalLinkAuthenticationProvider extends AuthenticationProvider {
    
    public URL getAuthenticationUrl();
    
}
