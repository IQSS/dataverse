package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Creates and manages explicit groups. Also provides services they might need.
 * @author michael
 */
public class ExplicitGroupProvider implements GroupProvider {
    
    private static final Logger logger = Logger.getLogger(ExplicitGroupProvider.class.getName());
    
    private final ExplicitGroupServiceBean explicitGroupSvc;
    private final RoleAssigneeServiceBean roleAssigneeSvc;

    public ExplicitGroupProvider(ExplicitGroupServiceBean anExplicitGroupSvc, RoleAssigneeServiceBean aRoleAssigneeSvc ) {
        explicitGroupSvc = anExplicitGroupSvc;
        roleAssigneeSvc = aRoleAssigneeSvc;
    }
       
    @Override
    public String getGroupProviderAlias() {
        return "explicit";
    }

    @Override
    public String getGroupProviderInfo() {
        return "Creates groups that contain users and other groups.";
    }

    @Override
    public Set groupsFor(User u) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group get(String groupAlias) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set findAll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Finds the role asgineed whose identifier is given. While this is basically
     * a delegation to {@link RoleAssigneeServiceBean}, we need it as a way of
     * dependency injection for {@link ExplicitGroup}s, which need to access the 
     * server context but are POJOs rahter than enterprise beans.
     * 
     * @param roleAssigneeIdtf The identifier of the role assignee.
     * @return The role assignee whose ID is passed.
     */
    RoleAssignee findRoleAssignee( String roleAssigneeIdtf ) {
        return roleAssigneeSvc.getRoleAssignee(roleAssigneeIdtf);
    }
}

