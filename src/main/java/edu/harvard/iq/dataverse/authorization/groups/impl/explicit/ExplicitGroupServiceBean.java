package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
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
 * A bean providing the {@link ExplicitGroupProvider}s with container services,
 * such as database connectivity.
 * 
 * @author michael
 */
@Named
@Stateless
public class ExplicitGroupServiceBean {
    
    @EJB
    private RoleAssigneeServiceBean roleAssigneeSvc;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
	protected EntityManager em;
	
    public ExplicitGroupProvider getProvider() {
        return new ExplicitGroupProvider(this, roleAssigneeSvc);
    }
    
    public ExplicitGroup persist( ExplicitGroup g ) {
        if ( g.getId() == null ) {
            em.persist( g );
            return g;
        } else {
            return em.merge( g );
        }    
    }
    
    public List<ExplicitGroup> findByOwner( Long dvObjectId ) {
        List<ExplicitGroup> groups =  em.createNamedQuery( "ExplicitGroup.findByOwnerId" )
                 .setParameter("ownerId", dvObjectId )
                 .getResultList();
        for ( ExplicitGroup eg : groups ) {
            eg.setProvider( getProvider() );
        }
        return groups;
    }
    
    ExplicitGroup findByAlias(String groupAlias) {
        try  {
            ExplicitGroup eg = em.createNamedQuery("ExplicitGroup.findByAlias", ExplicitGroup.class)
                    .setParameter("alias", groupAlias)
                    .getSingleResult();
            eg.setProvider( getProvider() );
            return eg;
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public ExplicitGroup findInOwner(Long id, String groupAliasInOwner) {
        try  {
            ExplicitGroup eg = em.createNamedQuery("ExplicitGroup.findByOwnerIdAndAlias", ExplicitGroup.class)
                    .setParameter("alias", groupAliasInOwner)
                    .setParameter("ownerId", id)
                    .getSingleResult();
            eg.setProvider( getProvider() );
            return eg;
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public void removeGroup(ExplicitGroup explicitGroup) {
        em.remove( explicitGroup );
    }
    
    public Set<ExplicitGroup> findAll() {
        return new HashSet<>(em.createNamedQuery("ExplicitGroup.findAll", ExplicitGroup.class).getResultList() );
    }
}
