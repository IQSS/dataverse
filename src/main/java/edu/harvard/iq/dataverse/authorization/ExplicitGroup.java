package edu.harvard.iq.dataverse.authorization;

import java.util.List;

public class ExplicitGroup implements Group {

    private List<RoleAssignee> roleAssignees;

    public void add(RoleAssignee roleAssignee) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void remove(RoleAssignee roleAssignee) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<RoleAssignee> getRoleAssignees() {
        return roleAssignees;
    }

}
