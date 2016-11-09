package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.DataverseUserPage;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import java.util.Collection;
import java.util.logging.Logger;

public class AuthUtil {

    private static final Logger logger = Logger.getLogger(DataverseUserPage.class.getCanonicalName());

    public static boolean isNonLocalLoginEnabled(boolean shibEnabled, Collection<AuthenticationProvider> providers) {
        if (shibEnabled) {
            return true;
        } else {
            logger.fine("Shib is not enabled.");
        }
        if (providers != null) {
            for (AuthenticationProvider provider : providers) {
                if (provider instanceof AbstractOAuth2AuthenticationProvider) {
                    logger.fine("found an oauth provider (returning true): " + provider.getId());
                    return true;
                } else {
                    logger.fine("not an oauth provider: " + provider.getId());
                }
            }
        }
        return false;
    }

}
