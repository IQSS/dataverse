package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

import java.util.*;
import java.util.logging.Logger;

/**
 * Creates and manages mail domain based groups
 */
public class MailDomainGroupProvider implements GroupProvider<MailDomainGroup> {
    
    private static final Logger logger = Logger.getLogger(MailDomainGroupProvider.class.getName());
    
    private final MailDomainGroupServiceBean emailGroupSvc;

    public MailDomainGroupProvider(MailDomainGroupServiceBean emailGroupSvc) {
        this.emailGroupSvc = emailGroupSvc;
    }
       
    @Override
    public String getGroupProviderAlias() {
        return "maildomain";
    }

    @Override
    public String getGroupProviderInfo() {
        return "Groups users by their email address domain.";
    }
    
    /**
     * This method is meant for group management. This is a global, unmanageable thing, so this will just result in an empty set.
     * @return empty set
     */
    @Override
    public Set<MailDomainGroup> groupsFor(RoleAssignee ra, DvObject o) {
        return Collections.emptySet();
    }
    
    /**
     * This method is meant for group management. This is a global, unmanageable thing, so this will just result in an empty set.
     * @return empty set
     */
    @Override
    public Set<MailDomainGroup> groupsFor(RoleAssignee ra) {
        return Collections.emptySet();
    }
    
    /**
     * Lookup for a request. We don't need the context, so just move to groupsFor(DataverseRequest)
     * @param req The request whose group memberships we evaluate.
     * @param dvo the DvObject which is the context for the groups. May be {@code null}.
     * @return A set of groups, if any.
     */
    @Override
    public Set<MailDomainGroup> groupsFor(DataverseRequest req, DvObject dvo ) {
        return groupsFor(req);
    }
    
    /**
     * Lookup the user group on each request by using the email address domain.
     * @param req
     * @return
     */
    @Override
    public Set<MailDomainGroup> groupsFor(DataverseRequest req) {
        AuthenticatedUser user = req.getAuthenticatedUser();
        if ( user != null ) {
            return updateProvider(emailGroupSvc.findAllWithDomain(user) );
        } else {
            return Collections.emptySet();
        }
    }
    
    @Override
    public MailDomainGroup get(String groupAlias) {
        return updateProvider(emailGroupSvc.findByAlias(groupAlias).orElse(null));
    }

    /**
     * Find all email groups available
     * @return empty set.
     */
    @Override
    public Set<MailDomainGroup> findGlobalGroups() {
        return updateProvider( new HashSet<>(emailGroupSvc.findAll()) );
    }
    
    /**
     * Update an existing instance (if found) or create a new (if groupName = null).
     * @param groupName String with the group alias of the group to update or empty if new entity
     * @param grp The group to update or add
     * @return The saved entity, including updated group provider attribute
     */
    public MailDomainGroup saveOrUpdate(Optional<String> groupName, MailDomainGroup grp) {
        grp.setGroupProvider(this);
        return emailGroupSvc.saveOrUpdate(groupName, grp);
    }
    
    /**
     * Sets the provider of the passed explicit group to {@code this}.
     * @param eg the collection
     * @return the passed group, updated.
     */
    MailDomainGroup updateProvider(MailDomainGroup eg ) {
        if (eg == null) {
            return null; 
        }
        eg.setGroupProvider(this);
        return eg;
    }
    
    /**
     * Sets the provider of the explicit groups to {@code this}.
     * @param <T> Collection's type
     * @param mdgs the collection
     * @return the collection, with all the groups updated.
     */
    <T extends Collection<MailDomainGroup>> T updateProvider(T mdgs ) {
        for ( MailDomainGroup mdg : mdgs ) {
            updateProvider(mdg);
        }
        return mdgs;
    }
}

