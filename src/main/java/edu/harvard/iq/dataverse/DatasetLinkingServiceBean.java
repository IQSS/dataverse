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

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetLinkingServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<Dataset> findLinkedDataverses(Long linkingDataverseId) {
        List<Dataset> retList = new ArrayList();
        Query query = em.createQuery("select object(o.dataverse.id) from DatasetLinkingDataverse as o where o.linkingDataverse.id =:linkingDataverseId order by o.id");
        query.setParameter("linkingDataverseId", linkingDataverseId);
        for (Object o : query.getResultList()) {
            DatasetLinkingDataverse convterted = (DatasetLinkingDataverse) o;
            retList.add(convterted.getDataset());
        }
        return retList;
    }

    public List<Dataverse> findLinkingDataverses(Long datasetId) {
        List<Dataverse> retList = new ArrayList();
        for (DatasetLinkingDataverse dld : findDatasetLinkingDataverses(datasetId)) {
            retList.add(dld.getLinkingDataverse());
        }
        return retList;
    }

    public List<DatasetLinkingDataverse> findDatasetLinkingDataverses(Long datasetId) {
        return em.createQuery("select object(o) from DatasetLinkingDataverse as o where o.dataset.id =:datasetId order by o.id")
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

}
