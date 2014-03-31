/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class DataverseFacetServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<DataverseFacet> findByDataverseId(Long dataverseId) {
        Query query = em.createQuery("select object(o) from DataverseFacet as o where o.dataverse.id = :dataverseId order by o.displayOrder");
        query.setParameter("dataverseId", dataverseId);
        return query.getResultList();
    }
    public List<DataverseFacet> findByRootDataverse() {
        return em.createQuery("select object(o) from DataverseFacet as o where o.dataverse.id = 1 order by o.displayOrder").getResultList();
    }

    public void delete(DataverseFacet dataverseFacet) {
        em.remove(em.merge(dataverseFacet));
    }
    
	public void deleteFacetsFor( Dataverse d ) {
		em.createNamedQuery("DataverseFacet.removeByOwnerId")
			.setParameter("ownerId", d.getId())
				.executeUpdate();
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

