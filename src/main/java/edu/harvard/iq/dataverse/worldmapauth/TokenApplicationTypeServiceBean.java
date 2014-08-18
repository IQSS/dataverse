/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * @author raprasad
 */
@Stateless
@Named
public class TokenApplicationTypeServiceBean {
    
    private static final Logger logger = Logger.getLogger(TokenApplicationTypeServiceBean.class.getCanonicalName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public TokenApplicationType getGeoConnectApplication(){
        logger.info("--getGeoConnectApplication--");
        TokenApplicationType tat = this.findByName(TokenApplicationType.DEFAULT_GEOCONNECT_APPLICATION_NAME);
        if (tat==null){
            logger.info("-- Got it!!");
            tat = new TokenApplicationType();
            //return tat;
        }
        // Make a default application for GeoConnect
        tat.setName(TokenApplicationType.DEFAULT_GEOCONNECT_APPLICATION_NAME);
        tat.setContactEmail("info@iq.harvard.edu");
        tat.setHostname("localhost");
        tat.setIpAddress("127.0.0.1");
        tat.setTimeLimitMinutes(TokenApplicationType.DEFAULT_TOKEN_TIME_LIMIT_MINUTES);
        //tat.setMapitLink(TokenApplicationType.TEST_MAPIT_LINK);
        tat.setMapitLink(TokenApplicationType.DEV_MAPIT_LINK);
        
        return this.save(tat);
        
        //return null;
    }
    public TokenApplicationType find(Object pk) {
        if (pk==null){
            return null;
        }
        return (TokenApplicationType) em.find(TokenApplicationType.class, pk);
    }
    
    /**
     * 
     * Convert string to md5 hash
     * 
        import hashlib
        m = hashlib.md5()
        m.update("Give me python or give me...more time, more time -- c.mena")
        m.hexdigest()     #'266cf94160a22fe1ef118c907379cd60'
        
    */
    public String getMD5Hash(String stringToHash){
        if (stringToHash==null){
            return null;
        }
        MessageDigest md;   
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            logger.severe("Failed to set TokenApplicationType for 'Map It' request!!!");
            return null;
        }
        md.update(stringToHash.getBytes());
          
        byte[] mdbytes = md.digest();
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
                
       
    }
            
    public TokenApplicationType save( TokenApplicationType tokenApp ) {
        
        if (tokenApp==null){
            return null;
        }
        
        if (tokenApp.getName()==null){
            tokenApp.setName(TokenApplicationType.DEFAULT_GEOCONNECT_APPLICATION_NAME);
        }
        
        if (tokenApp.getMapitLink()==null){
            logger.warning("mapitLink is missing for tokenApp");
            return null;
        }
        
        // Set time limit minutes
        Integer time_limit_minutes = tokenApp.getTimeLimitMinutes();
        if (time_limit_minutes == null){
            tokenApp.setTimeLimitMinutes(TokenApplicationType.DEFAULT_TOKEN_TIME_LIMIT_MINUTES);
            // (also sets the time limit seconds)
        }
        
        // set md5
        tokenApp.setMd5(this.getMD5Hash(tokenApp.getName()));
        
        if ( tokenApp.getId() == null ) {
            tokenApp.setCreated();
            em.persist(tokenApp);
            logger.fine("New tokenApp saved");
            return tokenApp;
	} else {
            tokenApp.setModified();
            logger.fine("Existing tokenApp saved");
            return em.merge( tokenApp );
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