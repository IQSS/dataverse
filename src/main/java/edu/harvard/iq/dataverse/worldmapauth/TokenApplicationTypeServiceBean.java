/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import edu.harvard.iq.dataverse.util.MD5Checksum;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author rmp553
 */
@Stateless
@Named
public class TokenApplicationTypeServiceBean {
    
    private static final Logger logger = Logger.getLogger(TokenApplicationTypeServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    
    public TokenApplicationType find(Object pk) {
        if (pk==null){
            return null;
        }
        return (TokenApplicationType) em.find(TokenApplicationType.class, pk);
    }
    
    public TokenApplicationType save( TokenApplicationType token_application ) {
        if (token_application==null){
            return null;
        }
        
        if (token_application.getName()==null){
            logger.warning("Name is missing for token_application");
            return null;            
        }
        
        if (token_application.getMapitLink()==null){
            logger.warning("mapitLink is missing for token_application");
            return null;
        }
        
        // Set time limit minutes
        Integer time_limit_minutes = token_application.getTimeLimitMinutes();
        if (time_limit_minutes == null){
            token_application.setTimeLimitMinutes(30);
        }
        // Set time limit seconds
        long time_limit_secs = 60 * token_application.getTimeLimitMinutes();
        token_application.setTimeLimitSeconds(time_limit_secs);
        
        // set md5
        MD5Checksum md5Checksum = new MD5Checksum();
        try {
            token_application.setMd5(md5Checksum.CalculateMD5(token_application.getName()));
        } catch (Exception md5ex) {
            logger.warning("Failed to calculate MD5 signature for token_application");
            return null;
        }
        
        token_application.setModified();

        
        if ( token_application.getId() == null ) {
            token_application.setCreated();
            em.persist(token_application);
            logger.fine("New token_application saved");
            return token_application;
	} else {
            logger.fine("Existing token_application saved");
            return em.merge( token_application );
	}
    }
	        
   
    public TokenApplicationType findByName(String name){
        if (name == null){
            return null;
        }
        try{
            return em.createQuery("select m from TokenApplicationType m WHERE m.name=:name", TokenApplicationType.class)
					.setParameter("name", name)
					.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }    
    }
    
    
    public List<TokenApplicationType> getAllTokenApplicationTypes(){
        Query query = em.createQuery("select object(o) from TokenApplicationType order by o.modified desc");
        return query.getResultList();
    }    
    
}