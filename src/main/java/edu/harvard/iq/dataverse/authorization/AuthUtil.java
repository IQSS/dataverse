package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.DataverseUserPage;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    /**
     * @todo Cleanup and refactor (DRY) if we decide to use this.
     */
    public static String getNamesOfRemoteAuthProvidersWithSeparators(boolean shibEnabled, Collection<AuthenticationProvider> authenticationProviders) {
        List<AuthenticationProvider> finalList = new ArrayList<>();
        if (shibEnabled) {
            logger.fine("Shib enabled, adding.");
            finalList.add(new ShibAuthenticationProvider());
        }
        if (authenticationProviders != null) {
            for (AuthenticationProvider provider : authenticationProviders) {
                if (provider instanceof AbstractOAuth2AuthenticationProvider) {
                    logger.fine("found an oauth provider (returning true): " + provider.getId());
                    finalList.add(provider);
                }
            }
        }
        int numNonRemoteProviders = finalList.size();
        logger.fine("Number of non-remote auth providers: " + numNonRemoteProviders);
        switch (numNonRemoteProviders) {
            case 0:
                return null;
            case 1:
                return finalList.get(0).getInfo().getTitle();
            case 2:
                return finalList.get(0).getInfo().getTitle() + " or " + finalList.get(1).getInfo().getTitle();
            case 3:
                return finalList.get(0).getInfo().getTitle() + ", " + finalList.get(1).getInfo().getTitle() + ", or " + finalList.get(2).getInfo().getTitle();
            case 4:
                return finalList.get(0).getInfo().getTitle() + ", " + finalList.get(1).getInfo().getTitle() + ", " + finalList.get(2).getInfo().getTitle() + ", or " + finalList.get(3).getInfo().getTitle();
            case 5:
                return finalList.get(0).getInfo().getTitle() + ", " + finalList.get(1).getInfo().getTitle() + ", " + finalList.get(2).getInfo().getTitle() + ", " + finalList.get(3).getInfo().getTitle() + ", or " + finalList.get(4).getInfo().getTitle();
            default:
                return null;
        }
    }

}
