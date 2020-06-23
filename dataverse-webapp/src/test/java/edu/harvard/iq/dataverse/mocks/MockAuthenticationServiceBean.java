package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.qualifiers.TestBean;

import java.time.Clock;

/**
 * @author madunlap
 */
@TestBean
public class MockAuthenticationServiceBean extends AuthenticationServiceBean {

    public MockAuthenticationServiceBean() {
        super();
    }

    public MockAuthenticationServiceBean(Clock clock) {
        super(clock);
    }

    @Override
    public AuthenticatedUser getAuthenticatedUser(String identifier) {
        return new MockAuthenticatedUser();
    }

    @Override
    public AuthenticatedUser getAuthenticatedUserByEmail(String email) {
        return new MockAuthenticatedUser();
    }

    @Override
    public ApiToken findApiTokenByUser(AuthenticatedUser au) {
        return generateApiToken(au);
    }
}
