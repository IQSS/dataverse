package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;

public class AuthenticatedUsers extends PersistedGroup {

    private static final AuthenticatedUsers instance = new AuthenticatedUsers();
    
    private AuthenticatedUsers() {
        setAlias(":authenticated-users");
        setName("Authenticated Users");
        setDescription("All users, except for guests");
    }
    
    public static AuthenticatedUsers get() { return instance; }
    
    @Override
    public boolean contains(User aUser) {
        return (aUser instanceof AuthenticatedUser);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public GroupProvider getGroupProvider() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return":AuthenticatedUsers";
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo("Anyone with a Dataverse account", null);
    }

}
