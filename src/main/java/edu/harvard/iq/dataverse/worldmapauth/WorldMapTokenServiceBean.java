/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataverseUser;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 *
 * @author raprasad
 */
@Stateless
@Named
public class WorldMapTokenServiceBean {
    
    @EJB
    TokenApplicationTypeServiceBean tokenApplicationService;
     
    private static final Logger logger = Logger.getLogger(TokenApplicationTypeServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    /**
     * 
     * Retrieve a fresh token to map a Dataverse file on GeoConnect
     * 
     * @param dataFileID
     * @param dvUserID
     * @return WorldMapToken
     */
    public WorldMapToken getNewToken(DataFile dataFile, DataverseUser dvUser){
        if ((dataFile==null)||(dvUser==null)){
            logger.severe("dataFile or dvUser is null");
            return null;
        }
        WorldMapToken token = new WorldMapToken();
        token.setApplication(tokenApplicationService.getGeoConnectApplication());
        token.setDatafile(dataFile);
        token.setDataverseUser(dvUser);
        token.setModified();
        token.setToken();   
        return this.save(token);
//        return token;
       // getGeoConnectApplication
    }
    public WorldMapToken find(Object pk) {
        if (pk==null){
            return null;
        }
        return (WorldMapToken) em.find(WorldMapToken.class, pk);
    }

     public WorldMapToken save( WorldMapToken dvToken ) {
        
         if (dvToken==null){
            return null;
        }
        
        if (dvToken.getToken()==null){
            dvToken.setToken();
        }
        
        if (dvToken.getApplication().getMapitLink()==null){
            logger.warning("mapitLink is missing for token_application");
            return null;
        }
        
        TokenApplicationType dat = dvToken.getApplication();
        if (dat==null){
            logger.warning("TokenApplicationType is null for WorldMapToken");
            return null;
        }
        
        
        if ( dvToken.getId()== null ) {            
            em.persist(dvToken);
            logger.fine("New token_application saved");
            return dvToken;
	} else {
            logger.fine("Existing token_application saved");
            return em.merge( dvToken );
	}
    }
	        
   
    public WorldMapToken findByName(String token){
        if (token == null){
            return null;
        }
        try{
            return em.createQuery("select m from WorldMapToken m WHERE m.token=:token", WorldMapToken.class)
					.setParameter("token", token)
					.getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }    
    }
}
