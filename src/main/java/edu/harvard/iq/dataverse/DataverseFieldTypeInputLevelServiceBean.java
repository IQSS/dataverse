/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.LruCache;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DataverseFieldTypeInputLevelServiceBean {

//    private static final Logger logger = Logger.getLogger(DataverseFieldTypeInputLevelServiceBean.class.getCanonicalName());
    public static final LruCache<Long, List<DataverseFieldTypeInputLevel>> cache = new LruCache<>();

    @Inject
    EntityManagerBean emBean;

    public List<DataverseFieldTypeInputLevel> findByDataverseId(Long dataverseId) {
        List<DataverseFieldTypeInputLevel> res = cache.get(dataverseId);

        if (res == null) {
            res = emBean.getMasterEM().createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseId", DataverseFieldTypeInputLevel.class)
                .setParameter("dataverseId", dataverseId)
                .getResultList();
            cache.put(dataverseId, res);
        }

        return res;
    }
    
    private void msg(String s){
        //logger.fine(s);
    }
    
    /**
     * Find a list of DataverseFieldTypeInputLevel objects
     *  Search criteria: 
     *      - Dataverse Id, 
     *      - list of DatasetField Ids
     * 
     * @param dataverseId
     * @param datasetFieldIdList
     * @return List of DataverseFieldTypeInputLevel
     */
    public List<DataverseFieldTypeInputLevel> findByDataverseIdAndDatasetFieldTypeIdList( Long dataverseId, List<Long> datasetFieldIdList){
        msg("---- findByDataverseIdAndDatasetFieldTypeIdList ----");
        if (datasetFieldIdList==null || datasetFieldIdList.isEmpty()){
            return null;
        }
        if (dataverseId == null){                    
            return null;
        }
       
        try{
            return emBean.getMasterEM().createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseIdAndDatasetFieldTypeIdList", DataverseFieldTypeInputLevel.class)
                    .setParameter("datasetFieldIdList", datasetFieldIdList)
                    .setParameter("dataverseId", dataverseId)
                    .getResultList();
            /*List res = query.getResultList();
            msg("Number of results: " + res.size());
            return res;*/
        } catch ( NoResultException nre ) {  
            return null;
        }    
    }
            //     
    
    //    Query query = emBean.getMasterEM().createQuery("select object(o) from MapLayerMetadata as o where o.dataset=:dataset");// order by o.name");
    //    query.setParameter("dataset", dataset);
    
    public DataverseFieldTypeInputLevel findByDataverseIdDatasetFieldTypeId(Long dataverseId, Long datasetFieldTypeId) {
        Query query = emBean.getMasterEM().createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseIdDatasetFieldTypeId", DataverseFieldTypeInputLevel.class);
        query.setParameter("dataverseId", dataverseId);
        query.setParameter("datasetFieldTypeId", datasetFieldTypeId);
        try{
            return (DataverseFieldTypeInputLevel) query.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }         
    }

    public void delete(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {
        emBean.getMasterEM().remove(emBean.getMasterEM().merge(dataverseFieldTypeInputLevel));
        cache.invalidate();
    }

    public void deleteFacetsFor(Dataverse d) {
        emBean.getMasterEM().createNamedQuery("DataverseFieldTypeInputLevel.removeByOwnerId")
                .setParameter("ownerId", d.getId())
                .executeUpdate();
        cache.invalidate(d.getId());

    }

    public void create(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {

        emBean.getMasterEM().persist(dataverseFieldTypeInputLevel);
    }

}
