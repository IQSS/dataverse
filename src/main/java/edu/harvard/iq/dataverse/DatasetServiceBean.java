/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
public class DatasetServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
 
    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }
    
    public List<Dataset> findByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.id");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id").getResultList();
    }

    public void generateFileSystemName(DataFile dataFile) {
        String fileSystemName = null;
        Long result = (Long) em.createNativeQuery("select nextval('filesystemname_seq')").getSingleResult();
        dataFile.setFileSystemName(result.toString());

    }

    /**
     * @todo write this method for real. Don't just iterate through every single
     * dataset! See https://redmine.hmdc.harvard.edu/issues/3988
     */
    public Dataset findByGlobalId(String globalId) {
        Dataset foundDataset = null;
        if (globalId != null) {
            Query query = em.createQuery("select object(o) from Dataset as o order by o.id");
            List<Dataset> datasets = query.getResultList();
            for (Dataset dataset : datasets) {
                if (globalId.equals(dataset.getGlobalId())) {
                    foundDataset = dataset;
                }
            }
        }
        return foundDataset;
    }

}
