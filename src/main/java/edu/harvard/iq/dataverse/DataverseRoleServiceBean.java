package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.UserRoleAssignments;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class DataverseRoleServiceBean {
	private static final Logger logger = Logger.getLogger(DataverseRoleServiceBean.class.getName());
	
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
	
	public RoleAssignment save( RoleAssignment assignment ) {
		logger.info("Saving: " + assignment );
		if ( assignment.getId() == null ) {
			logger.info("persisting" );
			em.persist(assignment);
			em.flush();
			return assignment;
		} else {
			logger.info("merging" );
			return em.merge( assignment );
		}
	}
	
	public DataverseRole find( Long id ) {
		return em.find( DataverseRole.class, id );
	}
	
	public List<DataverseRole> findAll() {
		return em.createNamedQuery("DataverseRole.listAll", DataverseRole.class).getResultList();
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
	
	
	public void revoke( Set<DataverseRole> roles, DataverseUser user ) {
		for ( DataverseRole role : roles ) {
			em.createNamedQuery("RoleAssignment.deleteByUserIdRoleId")
					.setParameter("userId", user.getId())
					.setParameter("roleId", role.getId())
					.executeUpdate();
			em.refresh(role);
		}
		em.refresh(user);
	}
	
	public UserRoleAssignments roleAssignments( DataverseUser user, Dataverse dv ) {
		UserRoleAssignments retVal = new UserRoleAssignments(user);
		while ( dv != null ) {
			retVal.add( directRoleAssignments(user, dv) );
			if ( dv.isPermissionRoot() ) break;
			dv = dv.getOwner();
		}
		return retVal;
	}
	
	/**
	 * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
	 * No traversal on the containment hierarchy is done.
	 * @param user the user whose roles are given
	 * @param dvo the object where the roles are defined.
	 * @return Set of roles defined for the user in the given dataverse.
	 * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser, edu.harvard.iq.dataverse.Dataverse)
	 */
	public List<RoleAssignment> directRoleAssignments( DataverseUser user, DvObject dvo ) {
		if ( user==null ) throw new IllegalArgumentException("User cannot be null");
		TypedQuery<RoleAssignment> query = em.createQuery(
				"SELECT r FROM RoleAssignment r WHERE r.user.id=:userId AND r.role.owner.id=:dvoId",
				RoleAssignment.class);
		query.setParameter("userId", user.getId());
		query.setParameter("dvoId", dvo.getId());
		return query.getResultList();
	}
}
