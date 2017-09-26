/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 *
 * @author raprasad
 */
@Stateless
@Named
public class WorldMapTokenServiceBean {
    
    @EJB
    PermissionServiceBean permissionService;

    @EJB
    TokenApplicationTypeServiceBean tokenApplicationService;
     
    private static final Logger logger = Logger.getLogger(TokenApplicationTypeServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    /**
     * 
     * Retrieve a fresh token to map a Dataverse file on GeoConnect
     * 
     * @param dataFile
     * @param dvUser
     * @return WorldMapToken
     */
    @TransactionAttribute(REQUIRES_NEW) 
    public WorldMapToken getNewToken(DataFile dataFile, AuthenticatedUser dvUser){
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
    }

    public WorldMapToken find(Object pk) {
        if (pk==null){
            return null;
        }
        return em.find(WorldMapToken.class, pk);
    }
    

    /**
     * Expire the token, set the "hasExpired" flag to True
     * 
     * @param wmToken 
     */
    public void expireToken(WorldMapToken wmToken){
        if (wmToken==null){
            return;
        }
        wmToken.expireToken();
        em.merge(wmToken);     
    }
    
    /**
     * Expire and then Delete the token
     * (The expire is a bit extraneous)
     * 
     * @param wmToken 
     */
    public void deleteToken(WorldMapToken wmToken){
    
        if (wmToken==null){
            return;
        }
        em.remove(em.merge(wmToken));
    }
    
    /*
        Remove expired tokens from the database
    */
    public void deleteExpiredTokens(){

        TypedQuery<WorldMapToken> query = em.createQuery("select object(w) from WorldMapToken as w where w.hasExpired IS TRUE", WorldMapToken.class);// order by o.name");
        List<WorldMapToken> tokenList = query.getResultList();
        for (WorldMapToken wmToken : tokenList) {
           // em.remove(token);
            em.remove(em.merge(wmToken));
	}
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

    
    
    /**
     * Given a string token, retrieve the related WorldMapToken object
     * 
     * @param worldmapTokenParam
     * @return WorldMapToken object (if it hasn't expired)
     */
    public WorldMapToken retrieveAndRefreshValidToken(String worldmapTokenParam){
        if (worldmapTokenParam==null){
            logger.warning("worldmapTokenParam is null.  Permission denied.");
            return null;
        }
        WorldMapToken wmToken = this.findByName(worldmapTokenParam);
        if (wmToken==null){
            logger.warning("WorldMapToken not found for '" + worldmapTokenParam + "'.  Permission denied.");
            return null;
        }
        if (wmToken.hasTokenExpired()){
            em.remove(em.merge(wmToken));   // Delete expired token from the database.
            logger.warning("WorldMapToken has expired.  Permission denied.");
            return null;
        }
        wmToken.refreshToken();
        
        logger.info("WorldMapToken refreshed.");
        this.save(wmToken);
        
        return wmToken;
    }
    
    
    /*
        Can the user connected to the WorldMapToken still
            edit the dataset for the file connected to the token?         
    */
    public boolean canTokenUserEditFile(WorldMapToken wmToken){
        if (wmToken==null){
            return false;
        }
        if (permissionService.userOn(wmToken.getDataverseUser(), wmToken.getDatafile()).has(Permission.EditDataset)) { 
            logger.info("WorldMap token-based auth: Token's User is still authorized to edit the dataset for the datafile.");
            return true;
        }
        return false;
        
    }

    /*
        Can the user connected to the WorldMapToken still
            download the file connected to the token?         
    */
    private boolean canTokenUserDownloadFile(WorldMapToken wmToken){
        if (wmToken==null){
            return false;
        }
        if (permissionService.userOn(wmToken.getDataverseUser(), wmToken.getDatafile()).has(Permission.DownloadFile)) { 
            logger.info("WorldMap token-based auth: Token's User is still authorized to download the datafile.");
            return true;
        }
        return false;
        
    }

    
    /*
        Given a string for a WorldMapToken and DataFile, check:
    
            (1) Is this token valid?
            (2) Does the token correspond to this DataFile?
    
        @return boolean if token is valid and corresponds to the given DataFile
    
    */
    public boolean isWorldMapTokenAuthorizedForDataFileDownload(String worldmapTokenParam, DataFile df){
        logger.info("-- isWorldMapTokenAuthorizedForDataFileworldmapTokenParam " + worldmapTokenParam);
        
        if ((worldmapTokenParam == null)||(df == null)){
            logger.info("nope: worldmapTokenParam or data file is null");
            return false;
        }
        
        // Check 1:  Is this a valid WorldMap token?
        //
        WorldMapToken token = this.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (token==null){
            logger.info("WorldMap token-based auth: Token is not invalid.");
            return false;
        }
        

        // Check 2:  Does this WorldMap token's datafile match the requested datafile?
        //
        if (!(token.getDatafile().getId().equals(df.getId()))){
            logger.info("WorldMap token-based auth: Token's datafile does not match the requested datafile.");
            return false;
        }
        
        // Check 3:  Does this WorldMap token's user have permission for the requested datafile?
        //
        if (!(this.canTokenUserDownloadFile(token))){ 
            logger.info("WorldMap token-based auth: Token's User is not authorized for the requested datafile.");
            return false;
        }
            
        
        logger.info("WorldMap token-based auth: Token is valid for the requested datafile");
        return true;
        
    }
    
    
    /*
        Given a string for a WorldMapToken, check:
    
            (1) Is this token valid?
            (2) Re-verify that can edit the Dataset connected to the token's DataFile
    
        @return boolean if token is valid and corresponds to the given DataFile
    
    */
    public boolean isWorldMapTokenAuthorizedForMetadataRetrievalAndUpdates(String worldmapTokenParam){
        logger.info("-- isWorldMapTokenAuthorizedForDataFile?");
        if (worldmapTokenParam == null){
            logger.info("nope: worldmapTokenParam or data file is null");
            return false;
        }
        
        // Check 1:  Is this a valid WorldMap token?
        //
        WorldMapToken token = this.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (token==null){
            logger.info("WorldMap token-based auth: Token is not invalid.");
            return false;
        }
        
               
   
        // Check 2:  Does this WorldMap token's user still have edit permission for the requested datafile?
        //
        if (!(this.canTokenUserEditFile(token))){ 
            logger.info("WorldMap token-based auth: Token's User is not authorized for the requested datafile.");
            return false;
        }
            
        
        logger.info("WorldMap token-based auth: Token is valid for the requested datafile");
        return true;
        
    }
} // end of class
