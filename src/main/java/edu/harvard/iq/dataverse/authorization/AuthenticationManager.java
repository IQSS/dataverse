package edu.harvard.iq.dataverse.authorization;

import java.util.ArrayList;
import java.util.Collection;

public interface AuthenticationManager {

    Collection<AuthenticationProvider> authenticationProviders = new ArrayList<>();

    public void registerProvider(AuthenticationProvider authenticationProvider);
}
