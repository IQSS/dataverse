package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

/**
 *
 * @author madunlap
 */
public class MockAuthenticatedUser extends AuthenticatedUser{
    @Override
    public AuthenticatedUserDisplayInfo getDisplayInfo() {
        return new AuthenticatedUserDisplayInfo("FirstMock", "LastMock", "EmailMock@email.com", "AffilMock", "PositionMock");
    }
}
