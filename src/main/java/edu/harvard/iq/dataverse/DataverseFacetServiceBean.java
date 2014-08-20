package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.LruCache;
import java.util.List;
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
    
    public static final LruCache<Long,List<DataverseFacet>> cache = new LruCache();
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<DataverseFacet> findByDataverseId(Long dataverseId) {
        List<DataverseFacet> res = cache.get(dataverseId);

        if ( res == null ) {
            Query query = em.createNamedQuery("DataverseFacet.findByDataverseId", DataverseFacet.class);
            query.setParameter("dataverseId", dataverseId);
            res = query.getResultList();
            cache.put(dataverseId, res);
        }

        return res; 
    }

    public void delete(DataverseFacet dataverseFacet) {
        em.remove(em.merge(dataverseFacet));
        cache.invalidate();
    }
    
	public void deleteFacetsFor( Dataverse d ) {
		em.createNamedQuery("DataverseFacet.removeByOwnerId")
			.setParameter("ownerId", d.getId())
				.executeUpdate();
        cache.invalidate(d.getId());
        
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
    
}

