package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;

/**
 * @author madunlap
 */
public class MockAuthenticatedUser extends AuthenticatedUser {
    @Override
    public AuthenticatedUserDisplayInfo getDisplayInfo() {
        return new AuthenticatedUserDisplayInfo("FirstMock", "LastMock", "EmailMock@email.com", "AffilMock", "PositionMock");
    }
}
