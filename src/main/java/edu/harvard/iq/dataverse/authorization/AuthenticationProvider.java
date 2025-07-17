package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 * Objects that can authenticate users. The authentication process yields a unique 
 * identifier for the user in the user directory this provider represents.
 * 
 * Providers can have optional functionalities, such as updating user data and verifying email addresses. This abilities
 * can be queried using the {@code isXXXAllowed()} methods. If an implementation returns {@code true}
 * from one of these methods, it has to implement the matching methods.
 * 
 * Note: If you are adding an implementation of this interface, please add a "friendly" name to the bundles.
 *  Example:  ShibAuthenticationProvider implements this interface.
 *            (a)  ShibAuthenticationProvider.getID() returns "ship"
 *            (b)  Construct bundle name using String id "shib" from (a):
 *                "authenticationProvider.name." + "ship" ->
 *            (c)  Bundle.properties entry: "authenticationProvider.name.shib=Shibboleth"
 *          
 * {@code AuthenticationPrvider}s are normally registered at startup in {@link AuthenticationServiceBean#startup()}.
 * 
 * @author michael
 */
public interface AuthenticationProvider {
    
    String getId();
        
    AuthenticationProviderDisplayInfo getInfo();
    
    default int getOrder() { return 1; }
    default boolean isPasswordUpdateAllowed() { return false; };
    default boolean isUserInfoUpdateAllowed() { return false; };
    default boolean isUserDeletionAllowed() { return false; };
    default boolean isOAuthProvider() { return false; };
    
    
    
    /**
     * Some providers (e.g organizational ones) provide verified email addresses.
     * @return {@code true} if we can treat email addresses coming from this provider as verified, {@code false} otherwise.
     */
    default boolean isEmailVerified() { return false; };
    
    

    /**
     * Updates the password of the user whose id is passed. 
     * @param userIdInProvider User id in the provider. NOT the {@link AuthenticatedUser#id}, which is internal to the installation.
     * @param newPassword password, in clear text
     * @throws UnsupportedOperationException if the provider does not support updating passwords.
     * @see #isPasswordUpdateAllowed() 
     */
    default void updatePassword( String userIdInProvider, String newPassword ) {
        throw new UnsupportedOperationException(this.toString() + " does not implement password updates");
    };
    
    /**
     * Verifies that the passed password matches the user's. Note that this method is has tri-state return
     * value ({@link Boolean} rather than a {@code boolean}). A {@code null} returned means that the user
     * was not found.
     * @param userIdInProvider User id in the provider. NOT the {@link AuthenticatedUser#id}, which is internal to the installation.
     * @param password The password string we test
     * @return {@code True} if the passwords match; {@code False} if they don't; {@code null} if the user was not found.
     * @throws UnsupportedOperationException if the provider does not support updating passwords.
     * @see #isPasswordUpdateAllowed()
     */
    default Boolean verifyPassword( String userIdInProvider, String password ) {
        throw new UnsupportedOperationException(this.toString() + " does not implement password updates");
    };
    
    /**
     * Updates the password of the user whose id is passed. 
     * @param userIdInProvider User id in the provider. NOT the {@link AuthenticatedUser#id}, which is internal to the installation.
     * @param updatedUserData
     * @throws UnsupportedOperationException
     * @see #isUserInfoUpdateAllowed() 
     */
    default void updateUserInfo( String userIdInProvider, AuthenticatedUserDisplayInfo updatedUserData ) {
        throw new UnsupportedOperationException(this.toString() + " does not implement account detail updates");
    };
    
    default void deleteUser( String userIdInProvider ) {
        throw new UnsupportedOperationException(this.toString() + " does not implement account deletions");
    }
 
    
    /**
     * Given the AuthenticationProvider id, return the friendly name 
     * of the AuthenticationProvider as defined in the bundle
     * 
     * If no name is defined, return the id itself
     * 
     * @param authProviderId
     * @return 
     */
    public static String getFriendlyName(String authProviderId){
        if (authProviderId == null){
            return BundleUtil.getStringFromBundle("authenticationProvider.name.null");
        }
        
        String friendlyName = BundleUtil.getStringFromBundle("authenticationProvider.name." + authProviderId);
        if (friendlyName == null){
            return authProviderId;
        }
        return friendlyName;
    }

    /**
     * Given the AuthenticationProvider id, 
     * return the friendly name using the static method
     */
    default String getFriendlyName(){
        // call static method
        return BundleUtil.getStringFromBundle("authentication.human_readable." + this.getId());
    }
        
}

