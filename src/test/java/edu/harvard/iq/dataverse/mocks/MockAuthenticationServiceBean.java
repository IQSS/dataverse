package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

/**
 *
 * @author madunlap
 */
public class MockAuthenticationServiceBean extends AuthenticationServiceBean {
    
    @Override
    public AuthenticatedUser getAuthenticatedUser( String identifier ) {
        return new MockAuthenticatedUser();
    }
    
    @Override
    public AuthenticatedUser getAuthenticatedUserByEmail(String email) {
        return new MockAuthenticatedUser();
    }
}
