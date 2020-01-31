package edu.harvard.iq.dataverse.dataverse.template;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author skraffmiller
 */
@Stateless
public class TemplateDao {

    private static final Logger logger = Logger.getLogger(DatasetDao.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Template find(Object pk) {
        return em.find(Template.class, pk);
    }

    public Template merge(Template template) {
        return em.merge(template);
    }

    public void flush() {
        em.flush();
    }

    public void remove(Template template) {
        em.remove(template);
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

    @SuppressWarnings("unchecked")
    public List<String> findDataverseNamesByDefaultTemplateId(long defaultTemplateId) {
        Query query = em.createNativeQuery("select dv.name from Dataverse as dv where dv.defaulttemplate_id = ? order by dv.name");
        query.setParameter(1, defaultTemplateId);

        return query.getResultList();
    }

    public void incrementUsageCount(Long templateId) {

        Template toUpdate = em.find(Template.class, templateId);
        Long usage = toUpdate.getUsageCount();
        usage++;
        toUpdate.setUsageCount(usage);
        em.merge(toUpdate);

    }
}
