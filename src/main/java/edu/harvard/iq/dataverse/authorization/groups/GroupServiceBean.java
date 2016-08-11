package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.BuiltInGroupsProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupsServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class GroupServiceBean {
    private static final Logger logger = Logger.getLogger(GroupServiceBean.class.getName());
    
    @EJB
    IpGroupsServiceBean ipGroupsService;
    @EJB
    ShibGroupServiceBean shibGroupService;
    @EJB
    ExplicitGroupServiceBean explicitGroupService;
    
    private final Map<String, GroupProvider> groupProviders = new HashMap<>();
    
    private IpGroupProvider ipGroupProvider;
    private ShibGroupProvider shibGroupProvider;
    private ExplicitGroupProvider explicitGroupProvider;
    
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
    @PostConstruct
    public void setup() {
        addGroupProvider( BuiltInGroupsProvider.get() );
        addGroupProvider( ipGroupProvider = new IpGroupProvider(ipGroupsService) );
        addGroupProvider( shibGroupProvider = new ShibGroupProvider(shibGroupService) );
        addGroupProvider( explicitGroupProvider = explicitGroupService.getProvider() );
    }

    public Group getGroup( String groupAlias ) {
        String[] comps = groupAlias.split( Group.PATH_SEPARATOR, 2 );
        GroupProvider gp = groupProviders.get( comps[0] );
        if ( gp == null ) {
            logger.log(Level.WARNING, "Cannot find group provider with alias {0}", comps[0]);
            return null;
        }
        return gp.get( comps[1] );
    }

    public IpGroupProvider getIpGroupProvider() {
        return ipGroupProvider;
    }

    public ShibGroupProvider getShibGroupProvider() {
        return shibGroupProvider;
    }
    
    /**
     * Finds all the groups {@code req} is part of in {@code dvo}'s context.
     * Recurses upwards in {@link ExplicitGroup}s, as needed.
     * @param req The request whose group memberships we seek.
     * @param dvo The {@link DvObject} we determining the context fo the membership.
     * @return The groups {@code req} is part of under {@code dvo}.
     */
    public Set<Group> groupsFor( DataverseRequest req, DvObject dvo ) {
        return groupTransitiveClosure(
                groupProviders.values().stream()
                              .flatMap(gp->(Stream<Group>)gp.groupsFor(req, dvo).stream())
                              .collect(toSet()),
                dvo);
    }
    
    /**
     * All the groups a Role assignee belongs to. Does not take request-level groups
     * (such as IPGroups) into account.
     * @param ra
     * @param dvo
     * @return 
     */
    public Set<Group> groupsFor( RoleAssignee ra, DvObject dvo ) {
        return groupTransitiveClosure(
                groupProviders.values().stream()
                              .flatMap(gp->(Stream<Group>)gp.groupsFor(ra, dvo).stream())
                              .collect( toSet() ),
                dvo);
    }

    /**
     * groupsFor( RoleAssignee ra, DvObject dvo ) doesn't really get *all* the
     * groups a Role assignee belongs to as advertised but this method comes
     * closer.
     *
     * @todo Determine if this method works with IP Groups.
     *
     * @param au An AuthenticatedUser.
     * @return As many groups as we can find for the AuthenticatedUser.
     * 
     * @deprecated use {@link #groupsFor(edu.harvard.iq.dataverse.engine.command.DataverseRequest)}
     */
    @Deprecated
    public Set<Group> groupsFor(AuthenticatedUser au) {
        Set<Group> groups = new HashSet<>();
        groups.addAll(groupsFor(au, null));
        String identifier = au.getIdentifier();
        if (identifier != null) {
            try {
                groups.addAll( explicitGroupService.findGroups(au) );
            } catch (IndexOutOfBoundsException ex) {
                logger.log(Level.INFO, "Couldn''t trim first character (@ sign) from identifier: {0}", identifier);
            }
        }

        return groups;
    }

    public Set<Group> groupsFor( DataverseRequest dr ) {
        Set<Group> groups = new HashSet<>();
        
        // get the global groups
        groups.addAll( groupsFor(dr,null) );
        
        // add the explicit groups
        groups.addAll( explicitGroupService.findGroups(dr.getUser()) );
        
        return groups;
    }
    
    /**
     * Given a set of groups and a DV object, return all the groups that are
     * reachable from the set. Effectively, if the initial set has an {@link ExplicitGroup},
     * recursively add all the groups it contains.
     * 
     * @param groups
     * @param dvo
     * @return All the groups included in the groups in {@code groups}.
     */
    private Set<Group> groupTransitiveClosure(Set<Group> groups, DvObject dvo) {
        // now, get the explicit group transitive closure.
        Set<ExplicitGroup> perimeter = new HashSet<>();
        Set<ExplicitGroup> visited = new HashSet<>();
        
        groups.stream()
              .filter((g) -> ( g instanceof ExplicitGroup ))
              .forEachOrdered((g) -> perimeter.add((ExplicitGroup) g));
        visited.addAll(perimeter);
        
        while ( ! perimeter.isEmpty() ) {
            ExplicitGroup g = perimeter.iterator().next();
            perimeter.remove(g);
            groups.add(g);
            
            Set<ExplicitGroup> discovered = explicitGroupProvider.groupsFor(g, dvo);
            discovered.removeAll(visited); // Ideally the conjunction is always empty, as we don't allow cycles.
            // Still, coding defensively here, in case someone gets too
            // smart on the SQL console.
            
            perimeter.addAll(discovered);
            visited.addAll(discovered);
        }
        
        return groups;
    }
    
    public Set<Group> findGlobalGroups() {
        Set<Group> groups = new HashSet<>();
        groupProviders.values().forEach( 
                gp-> groups.addAll( gp.findGlobalGroups() ));
        return groups;
    }
    
    private void addGroupProvider( GroupProvider gp ) {
        groupProviders.put( gp.getGroupProviderAlias(), gp );
    }
}
