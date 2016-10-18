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
    
    String getId();
    
    AuthenticationProviderDisplayInfo getInfo();

}
