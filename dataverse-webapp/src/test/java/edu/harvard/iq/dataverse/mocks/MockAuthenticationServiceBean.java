package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.qualifiers.TestBean;

/**
 * @author madunlap
 */
@TestBean
public class MockAuthenticationServiceBean extends AuthenticationServiceBean {

    @Override
    public AuthenticatedUser getAuthenticatedUser(String identifier) {
        return new MockAuthenticatedUser();
    }

    @Override
    public AuthenticatedUser getAuthenticatedUserByEmail(String email) {
        return new MockAuthenticatedUser();
    }
}
