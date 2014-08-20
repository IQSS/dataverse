
package edu.harvard.iq.dataverse;

import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class TemplateServiceBean {
    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Template find(Object pk) {
        return em.find(Template.class, pk);
    }
    
    	public Template save( Template template ) {
		if ( template.getId() == null ) {
			em.persist(template);
			return template;
		} else {
			return em.merge( template );
		}
	}

    
}
