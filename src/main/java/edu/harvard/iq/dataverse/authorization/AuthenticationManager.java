package edu.harvard.iq.dataverse.authorization;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The AuthenticationManager is responsible for registering and listing
 * AuthenticationProviders. There's only one, so you get it with the static
 * getter {@link #getInstance()} (singleton pattern).
 */
public class AuthenticationManager {

    private static final AuthenticationManager instance = new AuthenticationManager();

    /**
     * @return The one instance in the system (singleton pattern).
     */
    public static AuthenticationManager getInstance() {
        return instance;
    }

    /**
     * Local, Shibboleth, etc.
     */
    Collection<AuthenticationProvider> authenticationProviders = new ArrayList<>();

    private AuthenticationManager() {
    }

    public void registerProvider(AuthenticationProvider authenticationProvider) {
    }

    public Collection<AuthenticationProvider> getAuthenticationProviders() {
        return authenticationProviders;
    }

}
