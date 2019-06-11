package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Collection;
import java.util.Collections;
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
     * Returns all the groups role assignee belongs to in the context of 
     * {@code o} and {@code req}. This includes groups defined on {@code o}'s parents as well,
     * but not on the groups descendants - only groups directly containing the users are
     * included in the list.
     * 
     * @param req The request
     * @param o The DvObject over which the groups are defined.
     * @return The groups the user belongs to in the context of {@code o}.
     */
    @Override
    public Set<ExplicitGroup> groupsFor(DataverseRequest req, DvObject o) {
        return updateProvider(explicitGroupSvc.findGroups(req.getUser(), o));
    }
    
    @Override
    public Set<ExplicitGroup> groupsFor(RoleAssignee ra, DvObject o) {
        return updateProvider(explicitGroupSvc.findGroups(ra, o));
    }
    
    @Override
    public Set<ExplicitGroup> groupsFor(RoleAssignee ra) {
        return updateProvider(explicitGroupSvc.findGroups(ra));
    }

    @Override
    public Set<ExplicitGroup> groupsFor(DataverseRequest req) {
        return updateProvider(explicitGroupSvc.findGroups(req.getUser()));
    }
    
    @Override
    public ExplicitGroup get(String groupAlias) {
        return updateProvider(explicitGroupSvc.findByAlias(groupAlias));
    }

    /**
     * As explicit groups are defined per dataverse, we cannot return any of them here.
     * @return empty set.
     */
    @Override
    public Set<ExplicitGroup> findGlobalGroups() {
        return Collections.emptySet();
    }
    
    public ExplicitGroup makeGroup() {
        return new ExplicitGroup(this);
    }
    
    /**
     * Finds the role assignee whose identifier is given. While this is basically
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
    
    /**
     * Sets the provider of the passed explicit group to {@code this}.
     * @param eg the collection
     * @return the passed group, updated.
     */
    ExplicitGroup updateProvider( ExplicitGroup eg ) {
        if (eg == null) {
            return null; 
        }
        eg.setProvider(this);
        return eg;
    }
    
    /**
     * Sets the provider of the explicit groups to {@code this}.
     * @param <T> Collection's type
     * @param egs the collection
     * @return the collection, with all the groups updated.
     */
    <T extends Collection<ExplicitGroup>> T updateProvider( T egs ) {
        for ( ExplicitGroup eg : egs ) {
            updateProvider(eg);
        }
        return egs;
    }
}

