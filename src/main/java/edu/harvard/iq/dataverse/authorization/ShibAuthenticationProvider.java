package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;

public class ShibAuthenticationProvider implements AuthenticationProvider, GroupCreator {

    @Override
    public RoleAssignee getRoleAssignee(String identifier) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Group createGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
