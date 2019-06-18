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

    class Credential {
        private final String key;

        /**
         * When {@code true}, the login form will use the secret/password widget rather than the regular text field.
         */
        private final boolean secret;

        public Credential(String key, boolean secret) {
            this.key = key;
            this.secret = secret;
        }

        public Credential(String key) {
            this(key, false);
        }

        public String getKey() {
            return key;
        }

        public boolean isSecret() {
            return secret;
        }
    }

    /**
     * Returns the list of credential required for login, normally username and password.
     * The strings will be used by the application as titles to the text fields, and then
     * as keys in the map passed to {@link #authenticate(java.util.Map)}.
     *
     * @return the list of credentials required for login.
     */
    List<Credential> getRequiredCredentials();

    /**
     * The main method of this interface - provide a consistent user id, within
     * the scope of this provider, for a user based on the request content.
     *
     * @param request All information needed to decide whether the user can be authenticated.
     * @return response with the result of the authentication process.
     * TODO BACKLOG push this method down to CredentialsAuthenticationProvider.
     */
    AuthenticationResponse authenticate(AuthenticationRequest request);
}
