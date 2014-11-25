package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import javax.servlet.ServletRequest;

public class AuthenticatedUsers extends AbstractGroup {

    private static final AuthenticatedUsers instance = new AuthenticatedUsers();
    
    public AuthenticatedUsers() {
        setAlias("int:authenticated-users");
        setName("Authenticated Users");
        setDescription("All users, except for guests");
    }
    
    public static AuthenticatedUsers get() { return instance; }
    
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
        return":AuthenticatedUsers";
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo("All Authenticated Users", null);
    }

}
