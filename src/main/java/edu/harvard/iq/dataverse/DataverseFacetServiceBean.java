package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.LruCache;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author xyang
 * @author Michael Bar-Sinai
 */
@Stateless
@Named
public class DataverseFacetServiceBean implements java.io.Serializable {
    
    public static final LruCache<Long,List<DataverseFacet>> cache = new LruCache<>();
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    DataverseServiceBean dataverses;
    
    public List<DataverseFacet> findByDataverseId(Long dataverseId) {
        List<DataverseFacet> res = cache.get(dataverseId);

        if ( res == null ) {
            res = em.createNamedQuery("DataverseFacet.findByDataverseId", DataverseFacet.class)
                            .setParameter("dataverseId", dataverseId).getResultList();
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
	
    public DataverseFacet create(int displayOrder, DatasetFieldType fieldType, Dataverse ownerDv) {
        DataverseFacet dataverseFacet = new DataverseFacet();
        
        dataverseFacet.setDisplayOrder(displayOrder);
        dataverseFacet.setDatasetFieldType(fieldType);
        dataverseFacet.setDataverse(ownerDv);
        
        ownerDv.getDataverseFacets().add(dataverseFacet);
        em.persist(dataverseFacet);
        return dataverseFacet;
    }
    
    public DataverseFacet create(int displayOrder, Long datasetFieldTypeId, Long dataverseId) {
        return create(displayOrder, em.find(DatasetFieldType.class,datasetFieldTypeId),
                        dataverses.find(dataverseId) );
    }
    
}

