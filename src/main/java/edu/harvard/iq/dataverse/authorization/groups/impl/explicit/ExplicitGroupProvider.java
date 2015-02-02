package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Collection;
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
    
    /**
     * Returns all the groups the user belongs to in the context of 
     * {@code o}. This includes groups defined on {@code o}'s parents as well.
     * 
     * @param u The user
     * @param o The DvObject over which the groups are defined.
     * @return The groups the user belongs to in the context of {@code o}.
     */
    @Override
    public Set<ExplicitGroup> groupsFor(User u, DvObject o) {
        return explicitGroupSvc.findGroups(u, o);
    }

    public Set<ExplicitGroup> groupsFor( ExplicitGroup eg, DvObject o ) {
        return explicitGroupSvc.findGroups(eg, o);
    }
    
    @Override
    public ExplicitGroup get(String groupAlias) {
        return explicitGroupSvc.findByAlias( groupAlias );
    }

    @Override
    public Set<ExplicitGroup> findAll() {
        return explicitGroupSvc.findAll();
    }
    
    public ExplicitGroup makeGroup() {
        return new ExplicitGroup(this);
    }
    
    /**
     * Finds the role asgineed whose identifier is given. While this is basically
     * a delegation to {@link RoleAssigneeServiceBean}, we need it as a way of
     * dependency injection for {@link ExplicitGroup}s, which need to access the 
     * server context but are POJOs rather than enterprise beans.
     * 
     * @param roleAssigneeIdtf The identifier of the role assignee.
     * @return The role assignee whose ID is passed.
     */
    RoleAssignee findRoleAssignee( String roleAssigneeIdtf ) {
        return roleAssigneeSvc.getRoleAssignee(roleAssigneeIdtf);
    }
    
    ExplicitGroup updateProvider( ExplicitGroup eg ) {
        eg.setProvider(this);
        return eg;
    }
    
    <T extends Collection<ExplicitGroup>> T updateProvider( T egs ) {
        for ( ExplicitGroup eg : egs ) {
            updateProvider(eg);
        }
        return egs;
    }
}

