package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.RoleAssignmentSet;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
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
public class DataverseRoleServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
      
    @EJB RoleAssigneeServiceBean roleAssigneeService;
    @EJB IndexServiceBean indexService;   
    @EJB SolrIndexServiceBean solrIndexService;

	public DataverseRole save( DataverseRole aRole ) {
		if ( aRole.getId() == null ) {
			em.persist(aRole);
                    /**
                     * @todo Why would getId be null? Should we call
                     * indexDefinitionPoint here too?
                     */
			return aRole;
		} else {
                    DataverseRole merged = em.merge(aRole);
                    /**
                     * @todo update permissionModificationTime here.
                     */
                    IndexResponse indexDefinitionPountResult = indexDefinitionPoint(merged.getOwner());
                    logger.info("aRole getId was not null. Indexing result: " + indexDefinitionPountResult);
                    return merged;
		}
	}
	
	public RoleAssignment save( RoleAssignment assignment ) {
		if ( assignment.getId() == null ) {
			em.persist(assignment);
			em.flush();
		} else {
			assignment = em.merge( assignment );
		}
            /**
             * @todo update permissionModificationTime here.
             */
            IndexResponse indexDefinitionPountResult = indexDefinitionPoint(assignment.getDefinitionPoint());
            logger.info("output from indexing operations: " + indexDefinitionPountResult);
                return assignment;
	}

    private IndexResponse indexDefinitionPoint(DvObject definitionPoint) {
        /**
         * @todo Do something with the index response. Was Solr down? Is
         * everything ok?
         */
        IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(definitionPoint);
        return indexResponse;
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
        
	
	public List<DataverseRole> findBuiltinRoles() {
		return em.createNamedQuery("DataverseRole.findBuiltinRoles", DataverseRole.class)
				.getResultList();
	}
        
	
	public DataverseRole findBuiltinRoleByAlias(String alias) {
		return em.createNamedQuery("DataverseRole.findBuiltinRoleByAlias", DataverseRole.class)
                                .setParameter("alias", alias)
				.getSingleResult();
        }
	
	public void revoke( Set<DataverseRole> roles, RoleAssignee assignee, DvObject defPoint ) {
		for ( DataverseRole role : roles ) {
			em.createNamedQuery("RoleAssignment.deleteByAssigneeIdentifier_RoleIdDefinition_PointId")
					.setParameter("assigneeIdentifier", assignee.getIdentifier())
					.setParameter("roleId", role.getId())
					.setParameter("definitionPointId", defPoint.getId())
					.executeUpdate();
			em.refresh(role);
		}
		em.refresh(assignee);
	}
	
	public void revoke( RoleAssignment ra ) {
		if ( ! em.contains(ra) ) {
			ra = em.merge(ra);
		}
		em.remove(ra);
            /**
             * @todo update permissionModificationTime here.
             */
            IndexResponse indexDefinitionPointResult = indexDefinitionPoint(ra.getDefinitionPoint());
            logger.info("indexing operation results: " + indexDefinitionPointResult);
	}
	
	public RoleAssignmentSet roleAssignments( User user, Dataverse dv ) {
		RoleAssignmentSet retVal = new RoleAssignmentSet(user);
		while ( dv != null ) {
			retVal.add( directRoleAssignments(user, dv) );
			if ( dv.isPermissionRoot() ) break;
			dv = dv.getOwner();
		}
		return retVal;
	}
	
    public List<RoleAssignment> roleAssignments( Long roleId ) {
        return em.createNamedQuery("RoleAssignment.listByRoleId", RoleAssignment.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }
    
	public RoleAssignmentSet assignmentsFor( final User u, final DvObject d ) {
		return d.accept( new DvObject.Visitor<RoleAssignmentSet>() {

			@Override
			public RoleAssignmentSet visit(Dataverse dv) {
				return roleAssignments(u, dv);
			}

			@Override
			public RoleAssignmentSet visit(Dataset ds) {
				RoleAssignmentSet asgn = ds.getOwner().accept(this);
				asgn.add( directRoleAssignments(u, ds) );
				return asgn;
			}

			@Override
			public RoleAssignmentSet visit(DataFile df) {
				RoleAssignmentSet asgn = df.getOwner().accept(this);
				asgn.add( directRoleAssignments(u, df) );
				return asgn;
			}
		});
	}
	
	public Set<RoleAssignment> rolesAssignments( DvObject dv ) {
		Set<RoleAssignment> ras = new HashSet<>();
		while ( !dv.isEffectivelyPermissionRoot() ) {
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
	 * @param roas the user whose roles are given
	 * @param dvo the object where the roles are defined.
	 * @return Set of roles defined for the user in the given dataverse.
	 * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser, edu.harvard.iq.dataverse.Dataverse)
	 */
	public List<RoleAssignment> directRoleAssignments( RoleAssignee roas, DvObject dvo ) {
		if ( roas==null ) throw new IllegalArgumentException("RoleAssignee cannot be null");
		TypedQuery<RoleAssignment> query = em.createNamedQuery(
				"RoleAssignment.listByAssigneeIdentifier_DefinitionPointId",
				RoleAssignment.class);
		query.setParameter("assigneeIdentifier", roas.getIdentifier());
		query.setParameter("definitionPointId", dvo.getId());
		return query.getResultList();
	}
	
	/**
	 * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
	 * No traversal on the containment hierarchy is done.
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
	public Set<DataverseRole> availableRoles( Long dvId ) {              
                Dataverse dv = em.find(Dataverse.class, dvId);
                Set<DataverseRole> roles = dv.getRoles(); 
                roles.addAll(findBuiltinRoles());

		while ( !dv.isEffectivelyPermissionRoot() ) {
			dv = dv.getOwner();
			roles.addAll( dv.getRoles() );
		}
		
		return roles;
	}                
}
