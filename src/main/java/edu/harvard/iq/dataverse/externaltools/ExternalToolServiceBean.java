package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class ExternalToolServiceBean {

    private static final Logger logger = Logger.getLogger(ExternalToolServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<ExternalTool> findAll() {
        TypedQuery<ExternalTool> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ExternalTool AS o ORDER BY o.id", ExternalTool.class);
        return typedQuery.getResultList();
    }

    public List<ExternalTool> findAll(DataFile file, ApiToken apiToken) {
        List<ExternalTool> externalTools = findAll();
        externalTools.forEach((externalTool) -> {
            externalTool.setDataFile(file);
            externalTool.setApiToken(apiToken);
        });
        return externalTools;
    }

    public ExternalTool save(ExternalTool externalTool) {
        em.persist(externalTool);
        // FIXME: Remove this flush.
        em.flush();
        return em.merge(externalTool);
    }

}
