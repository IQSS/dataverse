package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

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
    @EJB
    DataverseServiceBean dataverseService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Template find(Object pk) {
        return em.find(Template.class, pk);
    }

    public Template save(Template template) {
        return em.merge(template);
    }
    
    public List<Template> findByOwnerId(Long ownerId) {              
        return em.createNamedQuery("Template.findByOwnerId", Template.class).setParameter("ownerId", ownerId).getResultList();
    }
    
    public List<Template> findAll() {
        return em.createNamedQuery("Template.findAll", Template.class).getResultList();
    }
    
    public List<Dataverse> findDataversesByDefaultTemplateId(Long defaultTemplateId) {
        TypedQuery<Dataverse> query = em.createQuery("select object(o) from Dataverse as o where o.defaultTemplate.id =:defaultTemplateId order by o.name", Dataverse.class);
        query.setParameter("defaultTemplateId", defaultTemplateId);
        return query.getResultList();
    }

    public void incrementUsageCount(Long templateId) {

        Template toUpdate = em.find(Template.class, templateId);
        Long usage = toUpdate.getUsageCount();
        usage++;
        toUpdate.setUsageCount(usage);
        em.merge(toUpdate);

    }

    public boolean isTemplateValid(Dataverse dataverse, Template template) {
        if (template != null) {
            List<Template> dataverseTemplates = dataverse.getTemplates();
            if (dataverseTemplates != null && dataverseTemplates.contains(template)) {
                return true;
            }
            if (!dataverse.isTemplateRoot() && dataverse.getOwner() != null) {
                List<Template> ownerTemplates = dataverseService.find(dataverse.getOwner().getId()).getParentTemplates();
                if (ownerTemplates != null && ownerTemplates.contains(template)) {
                    return true;
                }
            }
        }
        return false;
    }
}
