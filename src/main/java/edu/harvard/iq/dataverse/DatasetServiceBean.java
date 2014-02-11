/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Collection;
import java.util.Iterator;
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

    public Dataset save(Dataset dataset) { 

        //em.merge(dataset.getVersions().get(0));           
        Dataset savedDataset = em.merge(dataset);
       //String indexingResult = indexService.indexDataset(savedDataset);
       //logger.info("during dataset save, indexing result was: " + indexingResult);
        return savedDataset;
    }

    public Dataset find(Object pk) {
        return (Dataset) em.find(Dataset.class, pk);
    }    
    
    public List<Dataset> findByOwnerId(Long ownerId) {
         Query query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.id");
         query.setParameter("ownerId", ownerId);
         return query.getResultList();
    }  

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id").getResultList();
    }
    
    public void removeCollectionElement(Collection coll, Object elem) {
        coll.remove(elem);
        em.remove(elem);
    }
     public void removeCollectionElement(List list,int index) {
        System.out.println("index is "+index+", list size is "+list.size());
        em.remove(list.get(index));
        list.remove(index);
    }  
    public void removeCollectionElement(Iterator iter, Object elem) {
        iter.remove();
        em.remove(elem);
    }
    
    public String getDatasetVersionTitle(DatasetVersion version){
        Long id = version.getId();
        Query query = em.createQuery("select v.strValue from DatasetFieldValue as v, DatasetVersion as dv, DatasetField as dsf where dsf.name ='title'"
                 + " and dsf.id = v.datasetField.id and dv.id =:id ");
        query.setParameter("id", id);         
        return (String) query.getSingleResult();
    }

}
