package edu.harvard.iq.dataverse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class DataverseRoleServiceBean {
	
	@PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
	
	public DataverseRole save( DataverseRole aRole ) {
		if ( aRole.getId() == null ) {
			em.persist(aRole);
			return aRole;
		} else {
			return em.merge( aRole );
		}
	}
	
	public DataverseRole find( Long id ) {
		return em.find( DataverseRole.class, id );
	}
	
	public void delete( Long id ) {
		em.createNamedQuery("DataverseRole.deleteById", DataverseRole.class)
				.setParameter("id", id)
				.executeUpdate();
	}
	
	public List<DataverseRole> findByOwnerId( Long ownerId ) {
		return em.createNamedQuery("DataverseRole.findByOwnerId", DataverseRole.class)
				.setParameter("ownerId", ownerId)
				.getResultList();
	}
	
	public void grant( Set<DataverseRole> roles, DataverseUser user ) {
		for ( DataverseRole role : roles ) {
			em.merge( new UserDataverseAssignedRole(role, user) );
		}
	}
	
	public void revoke( Set<DataverseRole> roles, DataverseUser user ) {
		for ( DataverseRole role : roles ) {
			em.createNamedQuery("UserDataverseAssignedRole.deleteByUserIdRoleId")
					.setParameter("userId", user.getId())
					.setParameter("roleId", role.getId())
					.executeUpdate();
			em.refresh(role);
		}
		em.refresh(user);
	}
	
	public Set<DataverseRole> effectiveRoles( DataverseUser user, Dataverse dv ) {
		Set<DataverseRole> roles = new HashSet<>();
		while ( dv != null ) {
			roles.addAll( definedRoles(user, dv) );
			if ( dv.isPermissionRoot() ) break;
			dv = dv.getOwner();
		}
		return roles;
	}
	
	/**
	 * Retrieves the roles defined for {@code user}, directly on {@code dv}.
	 * No traversal on the containment hierarchy is done.
	 * @param user the user whose roles are given
	 * @param dv the dataverse defining the roles
	 * @return Set of roles defined for the user in the given dataverse.
	 * @see #effectiveRoles(edu.harvard.iq.dataverse.DataverseUser, edu.harvard.iq.dataverse.Dataverse)
	 */
	public Set<DataverseRole> definedRoles( DataverseUser user, Dataverse dv ) {
		return new HashSet<>(em.createNamedQuery("UserDataverseAssignedRole.findRoleByUserIdDataverseId", DataverseRole.class)
				.setParameter("userId", user.getId())
				.setParameter("dataverseId", dv.getId())
				.getResultList());
	}
}
