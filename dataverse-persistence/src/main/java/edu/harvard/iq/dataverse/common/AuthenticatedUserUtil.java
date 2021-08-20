package edu.harvard.iq.dataverse.common;

public class AuthenticatedUserUtil {

    /**
     * Given the AuthenticationProvider id, return the friendly name
     * of the AuthenticationProvider as defined in the bundle
     * <p>
     * If no name is defined, return the id itself
     *
     * @param authProviderId
     * @return
     */
    public static String getAuthenticationProviderFriendlyName(String authProviderId) {
        if (authProviderId == null) {
            return BundleUtil.getStringFromBundle("authenticationProvider.name.null");
        }

        String friendlyName = BundleUtil.getStringFromBundle("authenticationProvider.name." + authProviderId);
        if (friendlyName.isEmpty()) {
            return authProviderId;
        }
        return friendlyName;
    }
}
