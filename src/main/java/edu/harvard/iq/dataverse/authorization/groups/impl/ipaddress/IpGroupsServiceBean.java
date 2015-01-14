package edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv4Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IPv6Address;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 *
 * @author michael
 */
@Named
@Stateless
public class IpGroupsServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
	protected EntityManager em;
	
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
    public IpGroup store( IpGroup grp ) {
        if ( grp.getId() == null ) {
            em.persist( grp );
            return grp;
        } else {
            return em.merge(grp);
        }
    }
    
    public IpGroup get( long id ) {
       return em.find( IpGroup.class, id);
    }
    
    public IpGroup getByAlias( String alias ) {
        try {
            return em.createNamedQuery("IpGroup.findByAlias", IpGroup.class)
              .setParameter("alias", alias)
              .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    public List<IpGroup> findAll() {
        return em.createNamedQuery("IpGroup.findAll").getResultList();
    }
    
    public Set<IpGroup> findAllIncludingIp( IpAddress ipa ) {
        if ( ipa instanceof IPv4Address ) {
            IPv4Address ip4 = (IPv4Address) ipa;
            List<IpGroup> groupList = em.createNamedQuery("IPv4Range.findGroupsContainingAddressAsLong", IpGroup.class)
                    .setParameter("addressAsLong", ip4.toLong()).getResultList();
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
        if ( roleAssigneeSvc.getAssignmentsFor(grp.getIdentifier()).isEmpty() ) {
            em.remove( grp );
            // TODO when adding explicit groups, need to check for group membership as well.
        } else {
            throw new IllegalArgumentException("Group " + grp.getAlias() + " has assignments and thus can't be deleted.");
        }
    }
}
