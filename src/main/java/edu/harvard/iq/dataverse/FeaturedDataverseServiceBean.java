/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class FeaturedDataverseServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    DataverseServiceBean dataverseService;
    
    private static final Logger logger = Logger.getLogger(FeaturedDataverseServiceBean.class.getCanonicalName());
    
    public List<DataverseFeaturedDataverse> findByDataverseId(Long dataverseId) {
        String qr = "select object(o) from DataverseFeaturedDataverse as o where o.dataverse.id = :dataverseId order by o.displayOrder";
        return em.createQuery(qr, DataverseFeaturedDataverse.class).setParameter("dataverseId", dataverseId).getResultList();
    }
    
    public List<Dataverse> findByDataverseIdQuick(Long dataverseId) {
         List<Object[]> searchResults;
         
         try {
             //searchResults = em.createNativeQuery("SELECT id, alias, name FROM dataverse WHERE id IN (select featureddataverse_id from DataverseFeaturedDataverse where dataverse_id = "+dataverseId+" order by displayOrder)").getResultList();
             searchResults = em.createNativeQuery("SELECT d.id, d.alias, d.name FROM dataverse d, DataverseFeaturedDataverse f WHERE f.featureddataverse_id = d.id AND f.dataverse_id = "+dataverseId+" order by f.displayOrder").getResultList();
         } catch (Exception ex) {
             return null;
         }

         List<Dataverse> ret = new ArrayList<>();
         
         for (Object[] result : searchResults) {
            Long id = (Long)result[0];
            
            if (id == null) {
                continue;
            }
            
            Dataverse dataverse = new Dataverse(); 
            dataverse.setId(id);
                        
            if (result[1] != null) {
                dataverse.setAlias((String)result[1]);
            }
            
            if (result[2] != null) {
                dataverse.setName((String)result[2]);
            }
            
            dataverse.setDataverseTheme(dataverseService.findDataverseThemeByIdQuick(id));
            if (dataverse.getDataverseTheme() != null) {
                logger.fine("THEME: "
                        + dataverse.getDataverseTheme().getLogo() + ", "
                        + dataverse.getDataverseTheme().getLogoFormat() + ", "
                        + dataverse.getDataverseTheme().getLogoThumbnail());
            }
            ret.add(dataverse);
         }
         
         return ret;
    }
    
    public List<DataverseFeaturedDataverse> findByRootDataverse() {
        return em.createQuery("select object(o) from DataverseFeaturedDataverse as o where o.dataverse.id = 1 order by o.displayOrder", DataverseFeaturedDataverse.class).getResultList();
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
        
        Dataverse dataverse = em.find(Dataverse.class, dataverseId);
        dataverseFeaturedDataverse.setDataverse(dataverse);
        
        Dataverse featuredDataverse = em.find(Dataverse.class, featuredDataverseId);
        dataverseFeaturedDataverse.setFeaturedDataverse(featuredDataverse);

        em.persist(dataverseFeaturedDataverse);
    }	
    
    
}
