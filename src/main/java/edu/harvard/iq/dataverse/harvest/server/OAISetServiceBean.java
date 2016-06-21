/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Leonid Andreev
 * dedicated service for managing OAI sets, 
 * for the Harvesting server.
 */

@Stateless
@Named
public class OAISetServiceBean implements java.io.Serializable {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean");
    
    public HarvestingClient find(Object pk) {
        return (HarvestingClient) em.find(HarvestingClient.class, pk);
    }
    
    public boolean specExists(String spec) {
        boolean specExists = false;
        OAISet set = findBySpec(spec);
        
        if (set != null) {
            specExists = true;
        }
        return specExists;
    }

    public OAISet findBySpec(String spec) {
        String query = "SELECT o FROM OAISet o where o.spec = :specName";
        OAISet oaiSet = null;
        logger.info("Query: "+query+"; spec: "+spec);
        try {
            oaiSet = (OAISet) em.createQuery(query).setParameter("specName", spec).getSingleResult();
        } catch (Exception e) {
            // Do nothing, just return null. 
        }
        return oaiSet;
    }

    public List<OAISet> findAll() {
        try {
            logger.info("setService, findAll; query: select object(o) from OAISet as o order by o.name");
            List<OAISet> oaiSets = em.createQuery("select object(o) from OAISet as o order by o.name").getResultList();
            logger.info((oaiSets != null ? oaiSets.size() : 0) + " results found.");
            return oaiSets;
        } catch (Exception e) {
            return null;
        }
    }
    
    public void remove(OAISet oaiSet) {
        em.createQuery("delete from OAIRecord hs where hs.setName = '" + oaiSet.getSpec() + "'").executeUpdate();
        em.remove(oaiSet);
    }
    
    public OAISet findById(Long id) {
       return em.find(OAISet.class,id);
    }   
    
    public void save(OAISet oaiSet) {
        em.merge(oaiSet);
    }
    
}
