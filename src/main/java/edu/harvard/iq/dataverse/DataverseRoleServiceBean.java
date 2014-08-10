package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.User;
import edu.harvard.iq.dataverse.authorization.UserRoleAssignments;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
		if ( assignment.getId() == null ) {
			em.persist(assignment);
			em.flush();
			return assignment;
		} else {
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
	
	public void revoke( Set<DataverseRole> roles, User user, DvObject defPoint ) {
		for ( DataverseRole role : roles ) {
			em.createNamedQuery("RoleAssignment.deleteByUserRoleIdDefinitionPointId")
					.setParameter("userId", user.getIdentifier())
					.setParameter("roleId", role.getId())
					.setParameter("definitionPointId", defPoint.getId())
					.executeUpdate();
			em.refresh(role);
		}
		em.refresh(user);
	}
	
	public void revoke( RoleAssignment ra ) {
		if ( ! em.contains(ra) ) {
			ra = em.merge(ra);
		}
		em.remove(ra);
	}
	
	public UserRoleAssignments roleAssignments( User user, Dataverse dv ) {
		UserRoleAssignments retVal = new UserRoleAssignments(user);
		while ( dv != null ) {
			retVal.add( directRoleAssignments(user, dv) );
			if ( dv.isPermissionRoot() ) break;
			dv = dv.getOwner();
		}
		return retVal;
	}
	
	public UserRoleAssignments assignmentsFor( final User u, final DvObject d ) {
		return d.accept( new DvObject.Visitor<UserRoleAssignments>() {

			@Override
			public UserRoleAssignments visit(Dataverse dv) {
				return roleAssignments(u, dv);
			}

			@Override
			public UserRoleAssignments visit(Dataset ds) {
				UserRoleAssignments asgn = ds.getOwner().accept(this);
				asgn.add( directRoleAssignments(u, ds) );
				return asgn;
			}

			@Override
			public UserRoleAssignments visit(DataFile df) {
				UserRoleAssignments asgn = df.getOwner().accept(this);
				asgn.add( directRoleAssignments(u, df) );
				return asgn;
			}
		});
	}
	
	public Set<RoleAssignment> rolesAssignments( Dataverse dv ) {
		Set<RoleAssignment> ras = new HashSet<>();
		while ( ! dv.isEffectivlyPermissionRoot() ) {
			ras.addAll( em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
					.setParameter("definitionPointId", dv.getId() ).getResultList() );
			dv = dv.getOwner();
		}
		
		ras.addAll( em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
					.setParameter("definitionPointId", dv.getId() ).getResultList() );
		
		return ras;
	}
	
	/**
	 * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
	 * No traversal on the containment hierarchy is done.
	 * @param user the user whose roles are given
	 * @param dvo the object where the roles are defined.
	 * @return Set of roles defined for the user in the given dataverse.
	 * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser, edu.harvard.iq.dataverse.Dataverse)
	 */
	public List<RoleAssignment> directRoleAssignments( User user, DvObject dvo ) {
		if ( user==null ) throw new IllegalArgumentException("User cannot be null");
		TypedQuery<RoleAssignment> query = em.createNamedQuery(
				"RoleAssignment.listByUserIdDefinitionPointId",
				RoleAssignment.class);
		query.setParameter("userId", user.getIdentifier());
		query.setParameter("definitionPointId", dvo.getId());
		return query.getResultList();
	}
	
	/**
	 * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
	 * No traversal on the containment hierarchy is done.
	 * @param user the user whose roles are given
	 * @param dvo the object where the roles are defined.
	 * @return Set of roles defined for the user in the given dataverse.
	 * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser, edu.harvard.iq.dataverse.Dataverse)
	 */
	public List<RoleAssignment> directRoleAssignments( DvObject dvo ) {
		TypedQuery<RoleAssignment> query = em.createNamedQuery(
				"RoleAssignment.listByDefinitionPointId",
				RoleAssignment.class);
		query.setParameter("definitionPointId", dvo.getId());
		return query.getResultList();
	}
	
	/**
	 * Get all the available roles in a given dataverse, mapped by the
	 * dataverse that defines them. Map entries are ordered by reversed hierarchy 
	 * (root is always last).
	 * @param dvId The id of dataverse whose available roles we query
	 * @return map of available roles.
	 */
	public LinkedHashMap<Dataverse,Set<DataverseRole>> availableRoles( Long dvId ) {
		LinkedHashMap<Dataverse,Set<DataverseRole>> roles = new LinkedHashMap<>();
		Dataverse dv = em.find(Dataverse.class, dvId);
		roles.put( dv, dv.getRoles() );
		while( !dv.isEffectivlyPermissionRoot() ) {
			dv = dv.getOwner();
			roles.put( dv, dv.getRoles() );
		}
		
		return roles;
	}
}
