package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

/**
 * Objects that can authenticate users - for credentials, they provide persistent user id that can be used to
 * lookup an {@link AuthenticatedUser} using {@link AuthenticatedUserLookup} objects.
 * 
 * {@code AuthenticationPrvider}s are normally registered at startup in {@link AuthenticationServiceBean#startup()}.
 * 
 * @author michael
 */
public interface AuthenticationProvider {
    
    public String getId();
    
    public AuthenticationProviderDisplayInfo getInfo();
    
    /**
     * The main method of this interface - provide a consistent user id, within
     * the scope of this provider, for a user based on the request content.
     * @param request All information needed to decide whether the user can be authenticated.
     * @return response with the result of the authentication process.
     */
    public AuthenticationResponse authenticate( AuthenticationRequest request );

}
