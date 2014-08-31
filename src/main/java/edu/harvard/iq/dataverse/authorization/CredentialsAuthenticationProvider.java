package edu.harvard.iq.dataverse.authorization;

import java.util.List;

/**
 * An authentication provider that can authenticate users based on a set
 * of credentials, given as a map of strings. This allows the Dataverse application
 * to authenticate the user using an in-app form.
 * 
 * @author michael
 */
public interface CredentialsAuthenticationProvider extends AuthenticationProvider {
    
    /**
     * Returns the list of credential required for login, normally username and password.
     * The strings will be used by the application as titles to the text fields, and then
     * as keys in the map passed to {@link #authenticate(java.util.Map)}.
     * 
     * @return the list of credentials required for login.
     */
    List<String> getRequiredCredentials();
    
}
