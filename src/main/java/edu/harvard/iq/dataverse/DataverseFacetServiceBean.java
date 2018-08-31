package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.LruCache;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author xyang
 * @author Michael Bar-Sinai
 */
@Stateless
@Named
public class DataverseFacetServiceBean implements java.io.Serializable {
    
    public static final LruCache<Long,List<DataverseFacet>> cache = new LruCache<>();
    
    @Inject
    EntityManagerBean emBean;
    
    @EJB
    DataverseServiceBean dataverses;
    
    public List<DataverseFacet> findByDataverseId(Long dataverseId) {
        List<DataverseFacet> res = cache.get(dataverseId);

        if ( res == null ) {
            res = emBean.getMasterEM().createNamedQuery("DataverseFacet.findByDataverseId", DataverseFacet.class)
                            .setParameter("dataverseId", dataverseId).getResultList();
            cache.put(dataverseId, res);
        }

        return res; 
    }

    public void delete(DataverseFacet dataverseFacet) {
        emBean.getMasterEM().remove(emBean.getMasterEM().merge(dataverseFacet));
        cache.invalidate();
    }
    
	public void deleteFacetsFor( Dataverse d ) {
		emBean.getMasterEM().createNamedQuery("DataverseFacet.removeByOwnerId")
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
        emBean.getMasterEM().persist(dataverseFacet);
        return dataverseFacet;
    }
    
    public DataverseFacet create(int displayOrder, Long datasetFieldTypeId, Long dataverseId) {
        return create(displayOrder, emBean.getEntityManager().find(DatasetFieldType.class,datasetFieldTypeId),
                        dataverses.find(dataverseId) );
    }
    
}

