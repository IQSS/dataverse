package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import javax.servlet.ServletRequest;

public class AuthenticatedUsers extends AbstractGroup {

    public AuthenticatedUsers() {
        setAlias("int:authenticated-users");
        setName("Authenticated Users");
        setDescription("All users, except for guests");
    }
    
    @Override
    public boolean contains(User aUser, ServletRequest aRequest) {
        return (aUser instanceof AuthenticatedUser);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public GroupCreator getCreator() {
        return null;
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo("All Authenticated Users", null);
    }

}
