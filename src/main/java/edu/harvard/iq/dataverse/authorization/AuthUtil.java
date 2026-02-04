package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.DataverseUserPage;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.Collection;
import java.util.logging.Logger;

public class AuthUtil {

    private static final Logger logger = Logger.getLogger(DataverseUserPage.class.getCanonicalName());

    public static boolean isNonLocalSignupEnabled(Collection<AuthenticationProvider> providers, SystemConfig systemConfig) {
        if (providers != null) {
            
            for (AuthenticationProvider provider : providers) {
                if (provider instanceof AbstractOAuth2AuthenticationProvider || provider instanceof ShibAuthenticationProvider) {
                    logger.fine("found an remote auth provider (returning true): " + provider.getId());
                    if(!systemConfig.isSignupDisabledForRemoteAuthProvider(provider.getId())) {
                        return true;
                    }
                } else {
                    logger.fine("not a remote auth provider: " + provider.getId());
                }
            }
        }
        return false;
    }

    public static String getDisplayName(String firstName, String lastName) {
        if (firstName == null || firstName.isEmpty()) {
            if (lastName != null && !lastName.isEmpty()) {
                return lastName.trim();
            }
        }
        if (lastName == null || lastName.isEmpty()) {
            if (firstName != null && !firstName.isEmpty()) {
                return firstName.trim();
            }
        }
        if (firstName != null && !firstName.isEmpty() || lastName != null && !lastName.isEmpty()) {
            return firstName.trim() + " " + lastName.trim();
        }
        return null;
    }
}
