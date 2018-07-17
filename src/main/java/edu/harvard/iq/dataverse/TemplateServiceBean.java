package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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

    public Template save(Template template) {
        /*
                if (template.getId() == null) {
            em.persist(template);
            return template;
        } else {
            return em.merge(template);
        } */
        return em.merge(template);
    }

    public Template findByDeafultTemplateOwnerId(Long ownerId) {
        TypedQuery<Template> query = em.createQuery("select object(o.defaultTemplate) from Dataverse as o where o.owner.id =:ownerId order by o.name", Template.class);
        query.setParameter("ownerId", ownerId);
        return query.getSingleResult();
    }

    
    public List<Dataverse> findDataversesByDefaultTemplateId(Long defaultTemplateId) {
        TypedQuery<Dataverse> query = em.createQuery("select object(o) from Dataverse as o where o.defaultTemplate.id =:defaultTemplateId order by o.name", Dataverse.class);
        query.setParameter("defaultTemplateId", defaultTemplateId);
        return query.getResultList();
    }
    
    public void incrementUsageCount(Long templateId) {
        // Use object instead of Native Queries to update count
        //also catch/log exceptions so that create dataset will succeed
        //Issue 4847 SEK 7/16/2018
        try {
            Template toUpdate = em.find(Template.class, templateId);
            Long usage = toUpdate.getUsageCount();
            usage++;
            toUpdate.setUsageCount(usage);
            em.merge(toUpdate);
        } catch (Exception e) {
            logger.log(Level.INFO, "Increment usage count failed on template id {0}. ", templateId);
        }
    }
}
