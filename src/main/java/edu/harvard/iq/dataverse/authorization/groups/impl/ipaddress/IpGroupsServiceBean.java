package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv6Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * Provides CRUD tools to efficiently manage IP groups in a Java EE container.
 * 
 * @author michael
 */
@Named
@Stateless
public class IpGroupsServiceBean {
    
    private static final Logger logger = Logger.getLogger(IpGroupsServiceBean.class.getName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
	protected EntityManager em;
    
    @EJB
    ActionLogServiceBean actionLogSvc;
	
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
    /**
     * Stores (inserts/updates) the passed IP group.
     * @param grp The group to store.
     * @return Managed version of the group. The provider might be un-set.
     */
    public IpGroup store( IpGroup grp ) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "ipCreate");
        if ( grp.getGroupProvider() != null ) {
            alr.setInfo( grp.getIdentifier());
        } else {
            alr.setInfo( grp.getDisplayName() );
        }
        alr.setInfo( alr.getInfo() + "// " + grp.getRanges() );
        
        if ( grp.getId() == null ) {
            if ( grp.getPersistedGroupAlias() != null ) {
                IpGroup existing = getByGroupName( grp.getPersistedGroupAlias() );
                if ( existing == null ) {
                    // new group
                    em.persist( grp );
                    actionLogSvc.log( alr );
                    return grp;
                    
                } else {
                    existing.setDescription(grp.getDescription());
                    existing.setDisplayName(grp.getDisplayName());
                    existing.setIpv4Ranges(grp.getIpv4Ranges());
                    existing.setIpv6Ranges(grp.getIpv6Ranges());
                    actionLogSvc.log( alr.setActionSubType("ipUpdate") );
                    return existing;
                }
            } else {
                actionLogSvc.log( alr );
                em.persist( grp );
                return grp;
            }
        } else {
            actionLogSvc.log( alr.setActionSubType("ipUpdate") );
            return em.merge(grp);
        }
    }
    
    public IpGroup get( long id ) {
       return em.find( IpGroup.class, id);
    }
    
    public IpGroup getByGroupName( String alias ) {
        try {
            return em.createNamedQuery("IpGroup.findByPersistedGroupAlias", IpGroup.class)
              .setParameter("persistedGroupAlias", alias)
              .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    public List<IpGroup> findAll() {
        return em.createNamedQuery("IpGroup.findAll", IpGroup.class).getResultList();
    }
    
    public Set<IpGroup> findAllIncludingIp( IpAddress ipa ) {
        if ( ipa instanceof IPv4Address ) {
            IPv4Address ip4 = (IPv4Address) ipa;
            List<IpGroup> groupList = em.createNamedQuery("IPv4Range.findGroupsContainingAddressAsLong", IpGroup.class)
                    .setParameter("addressAsLong", ip4.toBigInteger()).getResultList();
            return new HashSet<>(groupList);

        } else if ( ipa instanceof IPv6Address ) {
            IPv6Address ip6 = (IPv6Address) ipa;
            long[] ip6arr = ip6.toLongArray();
            List<IpGroup> groupList = em.createNamedQuery("IPv6Range.findGroupsContainingABCD", IpGroup.class)
                    .setParameter("a", ip6arr[0])
                    .setParameter("b", ip6arr[1])
                    .setParameter("c", ip6arr[2])
                    .setParameter("d", ip6arr[3])
                    .getResultList();
            return new HashSet<>(groupList);

        } else {
            throw new IllegalArgumentException( "Unknown IpAddress type: " + ipa.getClass() + " (for IpAddress:" + ipa + ")" );
        }
    }
    
    /**
     * Deletes the group - if it has no assignments.
     * @param grp the group to be deleted
     * @throws IllegalArgumentException if the group has assignments
     * @see RoleAssigneeServiceBean#getAssignmentsFor(java.lang.String) 
     */
    public void deleteGroup( IpGroup grp ) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.GlobalGroups, "ipDelete");
        alr.setInfo( grp.getIdentifier() );
        if ( roleAssigneeSvc.getAssignmentsFor(grp.getIdentifier()).isEmpty() ) {
            em.remove( grp );
            actionLogSvc.log(alr);
            
        } else {
            String failReason = "Group " + grp.getAlias() + " has assignments and thus can't be deleted.";
            alr.setActionResult(ActionLogRecord.Result.BadRequest);
            alr.setInfo( alr.getInfo() + "// " + failReason);
            actionLogSvc.log(alr);
            throw new IllegalArgumentException(failReason);
        }
    }
}
