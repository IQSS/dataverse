package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import java.util.List;

public class LocalAuthenticationProvider implements AuthenticationProvider, UserLister, GroupCreator {

    @Override
    public RoleAssignee getRoleAssignee(String identifier) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<User> listUsers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Group createGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
