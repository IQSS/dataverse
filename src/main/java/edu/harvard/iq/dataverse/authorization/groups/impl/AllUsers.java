package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import edu.harvard.iq.dataverse.authorization.users.User;
import javax.servlet.ServletRequest;

public class AllUsers extends AbstractGroup {

    public AllUsers() {
        setAlias("int:all-users");
        setName("All Users");
        setDescription("All system users, including guests");
    }
        
    @Override
    public boolean contains(User aUser, ServletRequest aRequest) {
        return true;
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
        return new RoleAssigneeDisplayInfo("All Users", null);
    }

}
