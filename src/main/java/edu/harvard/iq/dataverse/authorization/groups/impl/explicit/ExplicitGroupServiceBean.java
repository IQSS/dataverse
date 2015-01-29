package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
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

}
