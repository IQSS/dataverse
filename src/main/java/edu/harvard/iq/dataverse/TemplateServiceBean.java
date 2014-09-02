package edu.harvard.iq.dataverse;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

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
        if (template.getId() == null) {
            em.persist(template);
            return template;
        } else {
            return em.merge(template);
        }
    }

    public Template findByDeafultTemplateOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o.defaultTemplate) from Dataverse as o where o.owner.id =:ownerId order by o.name");
        query.setParameter("ownerId", ownerId);
        return (Template) query.getSingleResult();
    }

    public List<Template> findByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }

}
