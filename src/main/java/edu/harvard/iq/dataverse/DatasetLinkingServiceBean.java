/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetLinkingServiceBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(DatasetLinkingServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    


    public List<Dataset> findLinkedDatasets(Long dataverseId) {
        List<Dataset> datasets = new ArrayList<>();
        TypedQuery<DatasetLinkingDataverse> typedQuery = em.createQuery("SELECT OBJECT(o) FROM DatasetLinkingDataverse AS o WHERE o.linkingDataverse.id = :dataverseId", DatasetLinkingDataverse.class);
        typedQuery.setParameter("dataverseId", dataverseId);
        List<DatasetLinkingDataverse> datasetLinkingDataverses = typedQuery.getResultList();
        for (DatasetLinkingDataverse datasetLinkingDataverse : datasetLinkingDataverses) {
            datasets.add(datasetLinkingDataverse.getDataset());
        }
        return datasets;
    }

    public List<Dataverse> findLinkingDataverses(Long datasetId) {
        List<Dataverse> retList = new ArrayList<>();
        Query query = em.createQuery("select object(o) from DatasetLinkingDataverse as o where o.dataset.id =:datasetId order by o.id");
        query.setParameter("datasetId", datasetId);
        for (Object o : query.getResultList()) {
            DatasetLinkingDataverse converted = (DatasetLinkingDataverse) o;
            retList.add(converted.getLinkingDataverse());
        }
        return retList;
    }
    
    public void save(DatasetLinkingDataverse datasetLinkingDataverse) {
        if (datasetLinkingDataverse.getId() == null) {
            em.persist(datasetLinkingDataverse);
        } else {
            em.merge(datasetLinkingDataverse);
        }
    }
    
    public DatasetLinkingDataverse findDatasetLinkingDataverse(Long datasetId, Long linkingDataverseId) {
        try {
            return (DatasetLinkingDataverse) em.createNamedQuery("DatasetLinkingDataverse.findByDatasetIdAndLinkingDataverseId")
                .setParameter("datasetId", datasetId)
                .setParameter("linkingDataverseId", linkingDataverseId)
                .getSingleResult();            
        } catch (javax.persistence.NoResultException e) {
            logger.fine("no datasetLinkingDataverse found for datasetId " + datasetId + " and linkingDataverseId " + linkingDataverseId);        
            return null;
        }
    }
    
    
    public boolean alreadyLinked(Dataverse dataverse, Dataset dataset) {
        return findDatasetLinkingDataverse(dataset.getId(), dataverse.getId()) != null;        
    }

}
