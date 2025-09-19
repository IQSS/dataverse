/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import jakarta.ejb.EJB;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
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
    
    @EJB
    DataverseServiceBean dataverseService;
    


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
        return findLinkingDataverses(datasetId, "");
    }
    
    public List<Dataverse> findLinkingDataverses(Long datasetId, String searchTerm) {
        List<Dataverse> retList = new ArrayList<>();
        if (searchTerm == null || searchTerm.isEmpty()) {
            TypedQuery<DatasetLinkingDataverse> typedQuery = em.createNamedQuery("DatasetLinkingDataverse.findByDatasetId", DatasetLinkingDataverse.class)
                    .setParameter("datasetId", datasetId);
            for (DatasetLinkingDataverse datasetLinkingDataverse : typedQuery.getResultList()) {
                retList.add(datasetLinkingDataverse.getLinkingDataverse());
            }
            return retList;

        } else {

            String pattern = searchTerm.toLowerCase();

            String pattern1 = pattern + "%";
            String pattern2 = "% " + pattern + "%";

            // Adjust the queries for very short, 1 and 2-character patterns:
            if (pattern.length() == 1) {
                pattern1 = pattern;
                pattern2 = pattern + " %";
            }
            System.out.print("pattern1: " + pattern1);
            System.out.print("pattern2: " + pattern2);
            TypedQuery<Long> typedQuery
                    = em.createNamedQuery("DatasetLinkingDataverse.findByDatasetIdAndLinkingDataverseName", Long.class)
                            .setParameter(1, datasetId).setParameter(2, "%dataverse").setParameter(3, pattern1)
                            .setParameter(4, pattern2).setParameter(5, "%dataverse").setParameter(6, pattern1).setParameter(7, pattern2);

            for (Long id : typedQuery.getResultList()) {
                retList.add(dataverseService.find(id));
            }
            return retList;
        }

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
