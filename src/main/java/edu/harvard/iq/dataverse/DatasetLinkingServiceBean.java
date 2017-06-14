/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
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

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<Dataset> findDatasetsThisDataverseIdHasLinkedTo(Long dataverseId) {
        List<Dataset> datasets = new ArrayList<>();
        TypedQuery<DatasetLinkingDataverse> typedQuery = em.createNamedQuery("DatasetLinkingDataverse.findByLinkingDataverseId", DatasetLinkingDataverse.class);
        typedQuery.setParameter("linkingDataverseId", dataverseId);
        List<DatasetLinkingDataverse> datasetLinkingDataverses = typedQuery.getResultList();
        for (DatasetLinkingDataverse datasetLinkingDataverse : datasetLinkingDataverses) {
            datasets.add(datasetLinkingDataverse.getDataset());
        }
        return datasets;
    }

    public List<Dataverse> findLinkingDataverses(Long datasetId) {
        List<Dataverse> retList = new ArrayList();
        for (DatasetLinkingDataverse dld : findDatasetLinkingDataverses(datasetId)) {
            retList.add(dld.getLinkingDataverse());
        }
        return retList;
    }

    public List<DatasetLinkingDataverse> findDatasetLinkingDataverses(Long datasetId) {
        return em.createNamedQuery("DatasetLinkingDataverse.findByDatasetId")
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
        TypedQuery<DatasetLinkingDataverse> typedQuery = em.createNamedQuery("DatasetLinkingDataverse.findByDatasetIdAndDataverseId",
                                                                              DatasetLinkingDataverse.class);
        typedQuery.setParameter("dataverseId", dataverse.getId());
        typedQuery.setParameter("datasetId", dataset.getId());
        return !typedQuery.getResultList().isEmpty();
    }

}
