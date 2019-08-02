package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Creates and manages explicit groups. Also provides services they might need.
 *
 * @author michael
 */
public class ExplicitGroupProvider implements GroupProvider<ExplicitGroup> {

    private static final Logger logger = Logger.getLogger(ExplicitGroupProvider.class.getName());

    private final ExplicitGroupServiceBean explicitGroupSvc;
    private final RoleAssigneeServiceBean roleAssigneeSvc;
    private final Map<Class<? extends Group>, GroupProvider<? extends Group>> otherGroupProviders; 

    public ExplicitGroupProvider(ExplicitGroupServiceBean anExplicitGroupSvc, RoleAssigneeServiceBean aRoleAssigneeSvc,
            List<GroupProvider<?>> otherGroupProviders) {
        this.explicitGroupSvc = anExplicitGroupSvc;
        this.roleAssigneeSvc = aRoleAssigneeSvc;
        this.otherGroupProviders = new HashMap<>();
        otherGroupProviders.forEach(provider -> this.otherGroupProviders.put(provider.providerFor(), provider));
    }

    @Override
    public String getGroupProviderAlias() {
        return ExplicitGroup.GROUP_TYPE;
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
     * @param o   The DvObject over which the groups are defined.
     * @return The groups the user belongs to in the context of {@code o}.
     */
    @Override
    public Set<ExplicitGroup> groupsFor(DataverseRequest req, DvObject o) {
        return explicitGroupSvc.findGroups(req.getUser(), o);
    }

    @Override
    public Set<ExplicitGroup> groupsFor(RoleAssignee ra, DvObject o) {
        return explicitGroupSvc.findGroups(ra, o);
    }

    @Override
    public Set<ExplicitGroup> groupsFor(RoleAssignee ra) {
        return explicitGroupSvc.findGroups(ra);
    }

    @Override
    public Set<ExplicitGroup> groupsFor(DataverseRequest req) {
        return explicitGroupSvc.findGroups(req.getUser());
    }

    @Override
    public ExplicitGroup get(String groupAlias) {
        return explicitGroupSvc.findByAlias(groupAlias);
    }

    /**
     * As explicit groups are defined per dataverse, we cannot return any of them here.
     *
     * @return empty set.
     */
    @Override
    public Set<ExplicitGroup> findGlobalGroups() {
        return Collections.emptySet();
    }

    public ExplicitGroup makeGroup() {
        return new ExplicitGroup();
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
    RoleAssignee findRoleAssignee(String roleAssigneeIdtf) {
        return roleAssigneeSvc.getRoleAssignee(roleAssigneeIdtf);
    }
    
    
    @Override
    public boolean contains(DataverseRequest req, ExplicitGroup explicitGroup) {
        return containsDirectly(req, explicitGroup) || containsIndirectly(req, explicitGroup);
    }
    
    
    /**
     * @param req
     * @return {@code true} iff the request is contained in the group or in an included non-explicit group.
     */
    private boolean containsDirectly(DataverseRequest req, ExplicitGroup explicitGroup) {
        User ra = req.getUser();
        if (ra instanceof AuthenticatedUser) {
            AuthenticatedUser au = (AuthenticatedUser) ra;
            if (explicitGroup.getContainedAuthenticatedUsers().contains(au)) {
                return true;
            }
        }

        if (explicitGroup.getContainedRoleAssignees().contains(ra.getIdentifier())) {
            return true;
        }

        for (String craIdtf : explicitGroup.getContainedRoleAssignees()) {
            // Need to retrieve the actual role assingee, and let it's logic decide.
            RoleAssignee cra = findRoleAssignee(craIdtf);
            if (cra != null) {
                if (cra instanceof Group) {
                    Group cgrp = (Group) cra;
                    if (otherGroupProviderContains(req, cgrp, cgrp.getClass())) {
                        return true;
                    }
                } // if cra is a user, we would have returned after the .contains() test.
            }
        }
        // If we get here, the request is not in this group.
        return false;
    }

    /**
     * @param req
     * @return {@code true} iff the request if contained in an explicit group that's a member of this group.
     */
    private boolean containsIndirectly(DataverseRequest req, ExplicitGroup explicitGroup) {
        for (ExplicitGroup ceg : explicitGroup.getContainedExplicitGroups()) {
            if (this.contains(req, ceg)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if {@link DataverseRequest} is part of the given {@link Group}
     * according to other group providers
     * <p>
     * otherGroupProviders is map with values: <code>{<group_class>: <provider_for_group_with_that_class>}</code>
     * so unchecked casting should be ok
     */
    @SuppressWarnings("unchecked")
    private <T extends Group> boolean otherGroupProviderContains(DataverseRequest req, Group group, Class<T> groupClass) {
        GroupProvider<T> providerForGroup = (GroupProvider<T>) otherGroupProviders.get(groupClass);
        if(providerForGroup == null) {
            throw new RuntimeException("No group provider for group type: " + groupClass.getName());
        }
        return providerForGroup.contains(req, (T)group);
    }

    @Override
    public Class<ExplicitGroup> providerFor() {
        return ExplicitGroup.class;
    }
}

