package edu.harvard.iq.dataverse.mocks;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.User;

public class MockRequestFactory {

    /**
     * @return A request with a guest user.
     */
    public static DataverseRequest makeRequest() {
        return makeRequest(GuestUser.get());
    }

    public static DataverseRequest makeRequest(User u) {
        return new DataverseRequest(u, IpAddress.valueOf("1.2.3.4"));
    }

}
