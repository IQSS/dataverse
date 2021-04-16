package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * A bean providing the {@link ExplicitGroupProvider}s with container services,
 * such as database connectivity.
 * 
 * @author michael
 */
@Named
@Stateless
public class ExplicitGroupServiceBean {
    
    private static final Logger logger = Logger.getLogger(ExplicitGroupServiceBean.class.getName());
    @EJB
    private RoleAssigneeServiceBean roleAssigneeSvc;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
	
    ExplicitGroupProvider provider;
    
    /**
     * A PostgreSQL-specific query that returns a group and all the groups
     * that contain it, and their parents too (-> recourse up teh containment
     * hierarchy of the explicit groups). Takes the group id as a parameter.
     */
    private static final String FIND_ALL_PARENTS_QUERY_TEMPLATE = "WITH RECURSIVE\n" +
        "explicit_group_graph AS (\n" +
        "  SELECT\n" +
        "     eg.id as id,\n" +
        "     ee.explicitgroup_id as parent_group_id\n" +
        "  FROM explicitgroup eg \n" +
        "    LEFT JOIN explicitgroup_explicitgroup ee \n" +
        "      ON eg.id=ee.containedexplicitgroups_id\n" +
        "),\n" +
        "parents AS (\n" +
        "  SELECT * FROM explicit_group_graph\n" +
        "  WHERE \n" +
        "    id IN (@IDS)\n" +
        "  UNION ALL\n" +
        "  SELECT egg.*\n" +
        "  FROM explicit_group_graph egg, parents\n" +
        "  WHERE parents.parent_group_id = egg.id\n" +
        ") SELECT * from explicitgroup \n" +
        "WHERE id IN (SELECT distinct id FROM parents);";
    
    @PostConstruct
    void setup() {
        provider = new ExplicitGroupProvider(this, roleAssigneeSvc);
    }
    
    public ExplicitGroupProvider getProvider() {
        return provider;
    }
    
    public ExplicitGroup persist( ExplicitGroup g ) {
        if ( g.getId() == null ) {
            em.persist( g );
            return g;
        } else {
            // clean stale data once in a while
            if ( Math.random() >= 0.5 ) {
                Set<String> stale = new TreeSet<>();
                for ( String idtf : g.getContainedRoleAssignees()) {
                    if ( roleAssigneeSvc.getRoleAssignee(idtf) == null ) {
                        stale.add(idtf);
                    }
                }
                if ( ! stale.isEmpty() ) {
                    g.getContainedRoleAssignees().removeAll(stale);
                }
            }
            
            return em.merge( g );
        }    
    }
    
    public List<ExplicitGroup> findByOwner( Long dvObjectId ) {
        return provider.updateProvider(em.createNamedQuery( "ExplicitGroup.findByOwnerId", ExplicitGroup.class)
                 .setParameter("ownerId", dvObjectId )
                 .getResultList());
    }
    
    ExplicitGroup findByAlias(String groupAlias) {
        try  {
            return provider.updateProvider( em.createNamedQuery("ExplicitGroup.findByAlias", ExplicitGroup.class)
                    .setParameter("alias", groupAlias)
                    .getSingleResult());
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public ExplicitGroup findInOwner(Long ownerId, String groupAliasInOwner) {
        try  {
            return provider.updateProvider( 
                    em.createNamedQuery("ExplicitGroup.findByOwnerIdAndAlias", ExplicitGroup.class)
                        .setParameter("alias", groupAliasInOwner)
                        .setParameter("ownerId", ownerId)
                        .getSingleResult());
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public void removeGroup(ExplicitGroup explicitGroup) {
        em.remove( explicitGroup );
    }
    
    /**
     * Returns all the explicit groups that are available in the context of the passed DvObject.
     * @param d The DvObject where the groups are queried
     * @return All the explicit groups defined at {@code d} and its ancestors.
     */
    public Set<ExplicitGroup> findAvailableFor( DvObject d ) {
        Set<ExplicitGroup> egs = new HashSet<>();
        while ( d != null ) {
            egs.addAll( findByOwner(d.getId()) );
            d = d.getOwner();
        }
        return provider.updateProvider( egs );
    }
    
    /**
     * Finds all the explicit groups {@code ra} is <b>directly</b> a member of.
     * To find all these groups and the groups the contain them (recursively upwards),
     * consider using {@link #findGroups(edu.harvard.iq.dataverse.authorization.RoleAssignee)}
     * @param ra the role assignee whose membership list we seek
     * @return set of the explicit groups that contain {@code ra} directly.
     * @see #findGroups(edu.harvard.iq.dataverse.authorization.RoleAssignee)
     */
    public Set<ExplicitGroup> findDirectlyContainingGroups( RoleAssignee ra ) {
        if ( ra instanceof AuthenticatedUser ) {
            return provider.updateProvider(
                    new HashSet<>(
                            em.createNamedQuery("ExplicitGroup.findByAuthenticatedUserIdentifier", ExplicitGroup.class)
                              .setParameter("authenticatedUserIdentifier", ra.getIdentifier().substring(1))
                              .getResultList()
                  ));
        } else if ( ra instanceof ExplicitGroup ) {
            return provider.updateProvider(
                    new HashSet<>(
                            em.createNamedQuery("ExplicitGroup.findByContainedExplicitGroupId", ExplicitGroup.class)
                              .setParameter("containedExplicitGroupId", ((ExplicitGroup) ra).getId())
                              .getResultList()
                  ));
        } else {
            return provider.updateProvider(
                    new HashSet<>(
                            em.createNamedQuery("ExplicitGroup.findByRoleAssgineeIdentifier", ExplicitGroup.class)
                              .setParameter("roleAssigneeIdentifier", ra.getIdentifier())
                              .getResultList()
                  ));
        }
    }

    
    /**
     * Finds all the explicit groups {@code ra} is a member of.
     * @param ra the role assignee whose membership list we seek
     * @return set of the explicit groups that contain {@code ra}.
     */
    public Set<ExplicitGroup> findGroups( RoleAssignee ra ) {
        return findClosure(findDirectlyContainingGroups(ra));
    }
    
    /**
     * Finds all the groups {@code ra} is a member of, in the context of {@code o}.
     * This includes both direct and indirect memberships.
     * @param ra The role assignee whose memberships we seek.
     * @param o The {@link DvObject} whose context we search.
     * @return All the groups in {@code o}'s context that {@code ra} is a member of.
     */
    public Set<ExplicitGroup> findGroups( RoleAssignee ra, DvObject o ) {
        return findGroups(ra).stream()
                .filter( g -> g.owner.isAncestorOf(o) )
                .collect( Collectors.toSet() );
    }
    
    /**
     * Finds all the groups {@code ra} directly belongs to in the context of {@code o}. In effect,
     * collects all the groups {@code ra} belongs to and that are defined at {@code o}
     * or one of its ancestors.
     * 
     * <B>Does not take group containment into account.</B> Use
     * 
     * @param ra The role assignee that belongs to the groups
     * @param o the DvObject that defines the context of the search.
     * @return All the groups ra belongs to in the context of o.
     */
    public Set<ExplicitGroup> findDirectGroups( RoleAssignee ra, DvObject o ) {
        if ( o == null ) {
            return Collections.emptySet();
        }
        List<ExplicitGroup> groupList = new LinkedList<>();
        
        if ( ra instanceof ExplicitGroup ) {
            for ( DvObject cur = o; cur != null; cur=cur.getOwner() ) {
                groupList.addAll( em.createNamedQuery("ExplicitGroup.findByOwnerAndSubExGroupId", ExplicitGroup.class)
                  .setParameter("ownerId", cur.getId())
                  .setParameter("subExGroupId", ((ExplicitGroup)ra).getId())
                  .getResultList() );
            }
            
        } else if ( ra instanceof AuthenticatedUser ) {
            for ( DvObject cur = o; cur != null; cur=cur.getOwner() ) {
                groupList.addAll( em.createNamedQuery("ExplicitGroup.findByOwnerAndAuthUserId", ExplicitGroup.class)
                  .setParameter("ownerId", cur.getId())
                  .setParameter("authUserId", ((AuthenticatedUser)ra).getId())
                  .getResultList() );
            }
            
        } else {
            for ( DvObject cur = o; cur != null; cur=cur.getOwner() ) {
                groupList.addAll( em.createNamedQuery("ExplicitGroup.findByOwnerAndRAIdtf", ExplicitGroup.class)
                  .setParameter("ownerId", cur.getId())
                  .setParameter("raIdtf", ra.getIdentifier())
                  .getResultList() );
            }
        }
        
        return provider.updateProvider( new HashSet<>(groupList) );
    }
    
    /**
     * 
     * Finds all the groups that contain the groups in {@code seed} (including {@code seed}), and the
     * groups that contain these groups, an so on.
     * 
     * @param seed the initial set of groups.
     * @return Transitive closure (based on group  containment) of the groups in {@code seed}.
     */
    protected Set<ExplicitGroup> findClosure( Set<ExplicitGroup> seed ) {
        
        if ( seed.isEmpty() ) return Collections.emptySet();
        
        String ids = seed.stream().map(eg->Long.toString(eg.getId())).collect( joining(",") );
        
        // PSQL driver has issues with arrays and collections as parameters, so we're using 
        // string manipulation to create the query here. Not ideal, but seems to be
        // the only solution at the Java Persistence level (i.e. without downcasting to org.postgresql.*)
        String sqlCode = FIND_ALL_PARENTS_QUERY_TEMPLATE.replace("@IDS", ids);
        return new HashSet<>(em.createNativeQuery(sqlCode, ExplicitGroup.class)
            .getResultList());
    }
    
    /**
     * 
     * Fully strips the assignee of membership in all the explicit groups.
     * 
     * @param assignee User or Group 
     */
    public void revokeAllGroupsForAssignee(RoleAssignee assignee) {
        if (assignee instanceof AuthenticatedUser) {
            em.createNativeQuery("DELETE FROM explicitgroup_authenticateduser WHERE containedauthenticatedusers_id=" + ((AuthenticatedUser) assignee).getId()).executeUpdate();
        } else if (assignee instanceof ExplicitGroup) {
            em.createNativeQuery("DELETE FROM explicitgroup_explicitgroup WHERE containedexplicitgroups_id=" + ((ExplicitGroup) assignee).getId()).executeUpdate();
        }
    }
    
    
}
