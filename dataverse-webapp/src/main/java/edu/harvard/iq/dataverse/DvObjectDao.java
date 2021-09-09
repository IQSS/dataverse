package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.apache.commons.lang.StringUtils.isNumeric;

@Stateless
public class DvObjectDao {

    private DataverseDao dataverseDao;
    private DatasetDao datasetDao;

    @Inject
    public DvObjectDao(DataverseDao dataverseDao, DatasetDao datasetDao) {
        this.dataverseDao = dataverseDao;
        this.datasetDao = datasetDao;
    }

    public DvObjectDao() {
    }

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    /**
     * Tries to find a DvObject. If the passed id can be interpreted as a number,
     * it tries to get the DvObject by its id. Else, it tries to get a {@link Dataverse}
     * with that alias. If that fails, tries to get a {@link Dataset} with that global id.
     *
     * @param id a value identifying the DvObject, either numeric of textual.
     * @return A DvObject, or {@code null}
     */
    public DvObject findDvo(String id) {
        if (isNumeric(id)) {
            return this.findDvo(Long.valueOf(id));
        } else {
            Dataverse d = dataverseDao.findByAlias(id);
            return (d != null) ? d : datasetDao.findByGlobalId(id);
        }
    }

    private DvObject findDvo(Long id) {
        return em.createNamedQuery("DvObject.findById", DvObject.class)
                 .setParameter("id", id)
                 .getSingleResult();
    }
}
