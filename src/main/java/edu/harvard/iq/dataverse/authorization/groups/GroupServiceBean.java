package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.authorization.groups.impl.PersistedGroup;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class GroupServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public Group getGroup( String groupAlias ) {
        try {
            return em.createNamedQuery("PersistedGroup.findByAlias", PersistedGroup.class )
                     .setParameter("alias", groupAlias)
                    .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
}
