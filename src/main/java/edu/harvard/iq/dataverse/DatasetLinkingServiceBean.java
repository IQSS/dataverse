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
    
    /**
     * @param linkingDataverseId
     * @return 
     * @todo Should this method simply be deleted? It isn't used anywhere and is
     * throwing exceptions: Syntax error parsing [select object(o.dataverse.id)
     * from DatasetLinkingDataverse as o where o.linkingDataverse.id
     * =:linkingDataverseId order by o.id]
     */
    @Deprecated
    public List<Dataset> findLinkedDataverses(Long linkingDataverseId) {
        List<Dataset> retList = new ArrayList<>();
        Query query = em.createQuery("select object(o.dataverse.id) from DatasetLinkingDataverse as o where o.linkingDataverse.id =:linkingDataverseId order by o.id");
        query.setParameter("linkingDataverseId", linkingDataverseId);
        for (Object o : query.getResultList()) {
            DatasetLinkingDataverse convterted = (DatasetLinkingDataverse) o;
            retList.add(convterted.getDataset());
        }
        return retList;
    }

    public List<Dataset> findDatasetsThisDataverseIdHasLinkedTo(Long dataverseId) {
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
        for (DatasetLinkingDataverse dld : findDatasetLinkingDataverses(datasetId)) {
            retList.add(dld.getLinkingDataverse());
        }
        return retList;
    }
    
    public DatasetLinkingDataverse findDatasetLinkingDataverse(Long datasetId, Long linkingDataverseId) {
        DatasetLinkingDataverse foundDatasetLinkingDataverse = null;
        try {
            foundDatasetLinkingDataverse = em.createQuery("SELECT OBJECT(o) FROM DatasetLinkingDataverse AS o WHERE o.linkingDataverse.id = :dataverseId AND o.dataset.id = :datasetId", DatasetLinkingDataverse.class)
                    .setParameter("datasetId", datasetId)
                    .setParameter("dataverseId", linkingDataverseId)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            logger.fine("no datasetLinkingDataverse found for datasetId " + datasetId + " and linkingDataverseId " + linkingDataverseId);        
        }
        return foundDatasetLinkingDataverse;
    }

    public List<DatasetLinkingDataverse> findDatasetLinkingDataverses(Long datasetId) {
        return em.createQuery("select object(o) from DatasetLinkingDataverse as o where o.dataset.id =:datasetId order by o.id", DatasetLinkingDataverse.class)
                .setParameter("datasetId", datasetId)
                .getResultList();
    }

    public void save(DatasetLinkingDataverse datasetLinkingDataverse) {
        if (datasetLinkingDataverse.getId() == null) {
            em.persist(datasetLinkingDataverse);
        } else {
            em.merge(datasetLinkingDataverse);
        }
    }
    
    public boolean alreadyLinked(Dataverse dataverse, Dataset dataset) {
        TypedQuery<DatasetLinkingDataverse> typedQuery = em.createQuery("SELECT OBJECT(o) FROM DatasetLinkingDataverse AS o WHERE o.linkingDataverse.id = :dataverseId AND o.dataset.id = :datasetId", DatasetLinkingDataverse.class);
        typedQuery.setParameter("dataverseId", dataverse.getId());
        typedQuery.setParameter("datasetId", dataset.getId());
        return !typedQuery.getResultList().isEmpty();
    }

}
