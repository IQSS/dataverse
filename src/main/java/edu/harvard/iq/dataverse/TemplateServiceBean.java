package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
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
    @Inject
    EntityManagerBean emBean;

    public Template find(Object pk) {
        return emBean.getEntityManager().find(Template.class, pk);
    }

    public Template save(Template template) {
        /*
                if (template.getId() == null) {
            emBean.getMasterEM().persist(template);
            return template;
        } else {
            return emBean.getMasterEM().merge(template);
        } */
        return emBean.getMasterEM().merge(template);
    }

    public Template findByDeafultTemplateOwnerId(Long ownerId) {
        TypedQuery<Template> query = emBean.getMasterEM().createQuery("select object(o.defaultTemplate) from Dataverse as o where o.owner.id =:ownerId order by o.name", Template.class);
        query.setParameter("ownerId", ownerId);
        return query.getSingleResult();
    }

    
    public List<Dataverse> findDataversesByDefaultTemplateId(Long defaultTemplateId) {
        TypedQuery<Dataverse> query = emBean.getMasterEM().createQuery("select object(o) from Dataverse as o where o.defaultTemplate.id =:defaultTemplateId order by o.name", Dataverse.class);
        query.setParameter("defaultTemplateId", defaultTemplateId);
        return query.getResultList();
    }
    
    public void incrementUsageCount(Long templateId) {
       
        Template toUpdate = emBean.getEntityManager().find(Template.class, templateId);
        Long usage = toUpdate.getUsageCount();
        usage++;
        toUpdate.setUsageCount(usage);
        emBean.getMasterEM().merge(toUpdate);

    }
}
