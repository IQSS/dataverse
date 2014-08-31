package edu.harvard.iq.dataverse.authorization;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * The AuthenticationManager is responsible for registering and listing
 * AuthenticationProviders. There's a single instance per application. 
 * 
 * Register the providers in the {@link #startup()} method.
 */
@Singleton
@Startup
public class AuthenticationServiceBean {

    /**
     * Where all registered authentication providers live.
     */
    final Collection<AuthenticationProvider> authenticationProviders = new ArrayList<>();


    @PostConstruct
    public void startup() {
        // FIXME register the builtin provider.
    }
    
    public void registerProvider(AuthenticationProvider authenticationProvider) {
    }

    public Collection<AuthenticationProvider> getAuthenticationProviders() {
        return authenticationProviders;
    }

}
