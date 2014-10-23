/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.LruCache;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DataverseFieldTypeInputLevelServiceBean {

    public static final LruCache<Long, List<DataverseFieldTypeInputLevel>> cache = new LruCache();

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<DataverseFieldTypeInputLevel> findByDataverseId(Long dataverseId) {
        List<DataverseFieldTypeInputLevel> res = cache.get(dataverseId);

        if (res == null) {
            Query query = em.createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseId", DataverseFacet.class);
            query.setParameter("dataverseId", dataverseId);
            res = query.getResultList();
            cache.put(dataverseId, res);
        }

        return res;
    }

    public DataverseFieldTypeInputLevel findByDataverseIdDatasetFieldTypeId(Long dataverseId, Long datasetFieldTypeId) {
        Query query = em.createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseIdDatasetFieldTypeId", DataverseFieldTypeInputLevel.class);
        query.setParameter("dataverseId", dataverseId);
        query.setParameter("datasetFieldTypeId", datasetFieldTypeId);
        try{
            return (DataverseFieldTypeInputLevel) query.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        } 
        
    }

    public void delete(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {
        em.remove(em.merge(dataverseFieldTypeInputLevel));
        cache.invalidate();
    }

    public void deleteFacetsFor(Dataverse d) {
        em.createNamedQuery("DataverseFieldTypeInputLevel.removeByOwnerId")
                .setParameter("ownerId", d.getId())
                .executeUpdate();
        cache.invalidate(d.getId());

    }

    public void create(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {

        em.persist(dataverseFieldTypeInputLevel);
    }

}
