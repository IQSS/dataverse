package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

/**
 * Objects that can authenticate users. The authentication process yields a unique 
 * identifier for the user in the user directory this provider represents.
 * 
 * Providers can have optional functionalities, such as updating user data and verifying email addresses. This abilities
 * can be queried using the {@code isXXXAllowed()} methods. If an implementation returns {@code true}
 * from one of these methods, it has to implement the matching methods.
 * 
 * {@code AuthenticationPrvider}s are normally registered at startup in {@link AuthenticationServiceBean#startup()}.
 * 
 * @author michael
 */
public interface AuthenticationProvider {
    
    String getId();
    
    AuthenticationProviderDisplayInfo getInfo();
    
    default boolean isPasswordUpdateAllowed() { return false; };
    default boolean isUserInfoUpdateAllowed() { return false; };
    default boolean isUserDeletionAllowed() { return false; };
    default boolean isOAuthProvider() { return false; };
    /** @todo Consider moving some or all of these to AuthenticationProviderDisplayInfo.*/
    /** The identifier is only displayed in the UI if it's meaningful, such as an ORCID iD.*/
    default boolean isDisplayIdentifier() { return false; };
    /** ORCID calls their persistent id an "ORCID iD".*/
    default String getPersistentIdName() { return null; };
    /** ORCID has special language to describe their ID: http://members.orcid.org/logos-web-graphics */
    default String getPersistentIdDescription() { return null; };
    /** An ORCID example would be the "http://orcid.org/" part of http://orcid.org/0000-0002-7874-374X*/
    default String getPersistentIdUrlPrefix() { return null; };
    default String getLogo() { return null; };
    
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
}
