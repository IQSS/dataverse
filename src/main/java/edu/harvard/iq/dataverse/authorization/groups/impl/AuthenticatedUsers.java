package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import javax.servlet.ServletRequest;

public class AuthenticatedUsers extends AbstractGroup {

    public AuthenticatedUsers() {
        setAlias("int:authenticated-users");
        setName("Authenticated Users");
        setDescription("All users, except for guests");
    }
    
    @Override
    public boolean contains(RoleAssignee anAssignee, ServletRequest aRequest) {
        return (anAssignee instanceof AuthenticatedUser);
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
    public String getDisplayInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
