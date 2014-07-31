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
 * @author skraffmiller
 */
@Stateless
@Named
public class FeaturedDataverseServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<DataverseFeaturedDataverse> findByDataverseId(Long dataverseId) {
        Query query = em.createQuery("select object(o) from DataverseFeaturedDataverse as o where o.dataverse.id = :dataverseId order by o.displayOrder");
        query.setParameter("dataverseId", dataverseId);
        return query.getResultList();
    }
    public List<DataverseFeaturedDataverse> findByRootDataverse() {
        return em.createQuery("select object(o) from DataverseFeaturedDataverse as o where o.dataverse.id = 1 order by o.displayOrder").getResultList();
    }

    public void delete(DataverseFeaturedDataverse dataverseFeaturedDataverse) {
        em.remove(em.merge(dataverseFeaturedDataverse));
    }
    
	public void deleteFeaturedDataversesFor( Dataverse d ) {
		em.createNamedQuery("DataverseFeaturedDataverse.removeByOwnerId")
			.setParameter("ownerId", d.getId())
				.executeUpdate();
	}
        
    public void create(int diplayOrder, Long featuredDataverseId, Long dataverseId) {
        DataverseFeaturedDataverse dataverseFeaturedDataverse = new DataverseFeaturedDataverse();
        
        dataverseFeaturedDataverse.setDisplayOrder(diplayOrder);
        
        Dataverse dataverse = (Dataverse)em.find(Dataverse.class,dataverseId);
        dataverseFeaturedDataverse.setDataverse(dataverse);
        
        Dataverse featuredDataverse = (Dataverse)em.find(Dataverse.class,featuredDataverseId);
        dataverseFeaturedDataverse.setFeaturedDataverse(featuredDataverse);

        em.persist(dataverseFeaturedDataverse);
    }	
    
    
}
