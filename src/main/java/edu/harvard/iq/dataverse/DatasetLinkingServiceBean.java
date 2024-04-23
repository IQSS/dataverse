/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

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
        TypedQuery<DatasetLinkingDataverse> typedQuery = em.createNamedQuery("DatasetLinkingDataverse.findByLinkingDataverseId", DatasetLinkingDataverse.class)
            .setParameter("linkingDataverseId", dataverseId);
        for (DatasetLinkingDataverse datasetLinkingDataverse : typedQuery.getResultList()) {
            datasets.add(datasetLinkingDataverse.getDataset());
        }
        return datasets;
    }

    public List<Dataverse> findLinkingDataverses(Long datasetId) {
        List<Dataverse> retList = new ArrayList<>();
        TypedQuery<DatasetLinkingDataverse> typedQuery = em.createNamedQuery("DatasetLinkingDataverse.findByDatasetId", DatasetLinkingDataverse.class)
            .setParameter("datasetId", datasetId);
        for (DatasetLinkingDataverse datasetLinkingDataverse : typedQuery.getResultList()) {
            retList.add(datasetLinkingDataverse.getLinkingDataverse());
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
            return em.createNamedQuery("DatasetLinkingDataverse.findByDatasetIdAndLinkingDataverseId",DatasetLinkingDataverse.class)
                .setParameter("datasetId", datasetId)
                .setParameter("linkingDataverseId", linkingDataverseId)
                .getSingleResult();            
        } catch (NoResultException e) {
            logger.fine("no datasetLinkingDataverse found for datasetId " + datasetId + " and linkingDataverseId " + linkingDataverseId);        
            return null;
        }
    }
    
    
    public boolean alreadyLinked(Dataverse dataverse, Dataset dataset) {
        return findDatasetLinkingDataverse(dataset.getId(), dataverse.getId()) != null;        
    }

}
