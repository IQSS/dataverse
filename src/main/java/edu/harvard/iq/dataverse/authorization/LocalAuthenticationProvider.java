package edu.harvard.iq.dataverse.authorization;

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
