/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author xyang
 * @author Michael Bar-Sinai
 */
@Stateless
@Named
public class DataverseFacetServiceBean {
    
    private static final LinkedHashMap<Long, List<DataverseFacet>> cache = new LinkedHashMap<>(10, 0.75f, true);
    private static final ReentrantLock cacheLock = new ReentrantLock();
    private static final int MAX_CACHE_SIZE = 100;
    
    private static final Logger logger = Logger.getLogger(DataverseFacetServiceBean.class.getName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<DataverseFacet> findByDataverseId(Long dataverseId) {
        List<DataverseFacet> res = null;
        try {
            cacheLock.lock();
            res = cache.get(dataverseId);
            
        } finally { cacheLock.unlock(); }

        if ( res == null ) {
            logger.log(Level.INFO, "Cache miss on dataverse {0}", dataverseId);
            Query query = em.createNamedQuery("DataverseFacet.findByDataverseId", DataverseFacet.class);
            query.setParameter("dataverseId", dataverseId);
            res = query.getResultList();
            try {
                cacheLock.lock();
                cache.put(dataverseId, res);
                
                if ( cache.size() > MAX_CACHE_SIZE ) {
                    cache.remove( cache.entrySet().iterator().next().getKey() );
                }

            } finally { cacheLock.unlock(); }
        } else {
            logger.log(Level.INFO, "Cache hit on dataverse {0}", dataverseId);
        }

        return res; 
    }

    public void delete(DataverseFacet dataverseFacet) {
        em.remove(em.merge(dataverseFacet));
        invalidateCache();
    }
    
	public void deleteFacetsFor( Dataverse d ) {
		em.createNamedQuery("DataverseFacet.removeByOwnerId")
			.setParameter("ownerId", d.getId())
				.executeUpdate();
        try {
            cacheLock.lock();
            cache.remove(d.getId());
        } finally {
            cacheLock.unlock();
        }
	}
	
    public void create(int diplayOrder, Long datasetFieldId, Long dataverseId) {
        DataverseFacet dataverseFacet = new DataverseFacet();
        
        dataverseFacet.setDisplayOrder(diplayOrder);
        
        DatasetFieldType dsfType = (DatasetFieldType)em.find(DatasetFieldType.class,datasetFieldId);
        dataverseFacet.setDatasetFieldType(dsfType);
        
        Dataverse dataverse = (Dataverse)em.find(Dataverse.class,dataverseId);
        dataverseFacet.setDataverse(dataverse);
        
        dataverse.getDataverseFacets().add(dataverseFacet);
        em.persist(dataverseFacet);
    }
    
    public void invalidateCache() {
        try {
            cacheLock.lock();
            cache.clear();
        } finally {
            cacheLock.unlock();
        }
    }
}

