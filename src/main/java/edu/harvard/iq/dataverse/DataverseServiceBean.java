/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author gdurand
 */
@Stateless
@Named
public class DataverseServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @EJB
    DatasetServiceBean datasetService;
    
    @Inject
    DataverseSession session;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataverse save(Dataverse dataverse) {
       
        dataverse.setModificationTime(new Timestamp(new Date().getTime()));
        Dataverse savedDataverse = em.merge(dataverse);
        /**
         * @todo check the result to see if indexing was successful or not
         */
        Future<String> indexingResult = indexService.indexDataverse(savedDataverse);
//        logger.log(Level.INFO, "during dataverse save, indexing result was: {0}", indexingResult);
        return savedDataverse;
    }

    public Dataverse find(Object pk) {
        return (Dataverse) em.find(Dataverse.class, pk);
    }

    public List<Dataverse> findAll() {
        return em.createQuery("select object(o) from Dataverse as o order by o.name").getResultList();
    }

    public List<Dataverse> findByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }
    
    public List<Dataverse> findPublishedByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataverse as o where o.owner.id =:ownerId and o.publicationDate is not null order by o.name");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }

    public Dataverse findRootDataverse() {
        return (Dataverse) em.createQuery("select object(o) from Dataverse as o where o.owner.id = null").getSingleResult();
    }
    
    public List<Dataverse> findAllPublishedByOwnerId(Long ownerId) {
        List<Dataverse> retVal = new ArrayList();       
        List<Dataverse> previousLevel = findPublishedByOwnerId(ownerId);
        
        retVal.addAll(previousLevel);
        /*
        if (!previousLevel.isEmpty()) {
            for (Dataverse dv : previousLevel) {
                retVal.addAll(findPublishedByOwnerId(dv.getId()));
            }
        }*/
        return retVal;
    }

    public Dataverse findByAlias(String anAlias) {
        try {
            return (anAlias.toLowerCase().equals(":root"))
				? findRootDataverse()
				: em.createQuery("select d from Dataverse d WHERE lower(d.alias)=:alias", Dataverse.class)
					.setParameter("alias", anAlias.toLowerCase())
					.getSingleResult();
        } catch ( NoResultException|NonUniqueResultException ex ) {
            return null;
        }
    }
	
	public boolean hasData( Dataverse dv ) {
		TypedQuery<Long> amountQry = em.createNamedQuery("Dataverse.ownedObjectsById", Long.class)
								.setParameter("id", dv.getId());
		
		return (amountQry.getSingleResult()>0);
	}
	
    public boolean isRootDataverseExists() {
        long count = em.createQuery("SELECT count(dv) FROM Dataverse dv WHERE dv.owner.id=null", Long.class).getSingleResult();
        return (count == 1);
    }

    public String determineDataversePath(Dataverse dataverse) {
        List<String> dataversePathSegments = new ArrayList();
        indexService.findPathSegments(dataverse, dataversePathSegments);
        StringBuilder dataversePath = new StringBuilder();
        for (String segment : dataversePathSegments) {
            dataversePath.append("/").append(segment);
        }
        return dataversePath.toString();
    }

    public MetadataBlock findMDB(Long id) {
        return (MetadataBlock) em.find(MetadataBlock.class, id);
    }

    public MetadataBlock findMDBByName(String name) {
        return em.createQuery("select m from MetadataBlock m WHERE m.name=:name", MetadataBlock.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    public List<MetadataBlock> findAllMetadataBlocks() {
        return em.createQuery("select object(o) from MetadataBlock as o order by o.id").getResultList();
    }
    
    public List<MetadataBlock> findSystemMetadataBlocks(){
        return em.createQuery("select object(o) from MetadataBlock as o where o.dataverse.id=null  order by o.id").getResultList();
    }
    
    public List<MetadataBlock> findMetadataBlocksByDataverseId(Long dataverse_id) {
        return em.createQuery("select object(o) from MetadataBlock as o where o.dataverse.id=:dataverse_id order by o.id")
                .setParameter("dataverse_id", dataverse_id).getResultList();
    }
    
    public DataverseFacet findFacet(Long id) {
        return (DataverseFacet) em.find(DataverseFacet.class, id);
    }
    
    public List<DataverseFacet> findAllDataverseFacets() {
        return em.createQuery("select object(o) from DataverseFacet as o order by o.display").getResultList();
    }  
   
    private String appVersionString;
    
    public String getApplicationVersion() {        
        if (appVersionString == null) {

            try {
                appVersionString = ResourceBundle.getBundle("VersionNumber").getString("version.number");
            } catch (MissingResourceException ex) {
                appVersionString = "4.0";
            }
            
            String buildNumber; 
            
            try {
                buildNumber = ResourceBundle.getBundle("BuildNumber").getString("build.number");
            } catch (MissingResourceException ex) {
                buildNumber = null; 
            }
            
            if (buildNumber != null && !buildNumber.equals("")) {
                appVersionString = appVersionString + " build " + buildNumber; 
            }
        }        
        
        return appVersionString; 
    }
    
    public boolean isDataverseCardImageAvailable(Long dataverseId, DataverseSession session) {
        Dataverse dataverse = find(dataverseId);
        
        if (dataverse == null) {
            logger.warning("Preview: Version service could not locate a DatasetVersion object for id "+dataverseId+"!");
            return false; 
        }
        
        String imageThumbFileName = null; 
        
        // First, check if the dataverse has a defined logo: 
        
        if (dataverse.getDataverseTheme() != null && dataverse.getDataverseTheme().getLogo() != null && !dataverse.getDataverseTheme().getLogo().equals("")) {
            File dataverseLogoFile = getLogo(dataverse);
            if (dataverseLogoFile != null) {
                String logoThumbNailPath = null;

                if (dataverseLogoFile.exists()) {
                    logoThumbNailPath = ImageThumbConverter.generateImageThumb(dataverseLogoFile.getAbsolutePath(), 48);
                    if (logoThumbNailPath != null) {
                        return true;
                    }
                }
            }
        }
        
        // If there's no uploaded logo for this dataverse, go through its 
        // [released] datasets and see if any of them have card images:
        // 
        // TODO:
        // Discuss/Decide if we really want to do this - i.e., go through every
        // file in every dataset below... 
        // -- L.A. 4.0 beta14
        
        for (Dataset dataset : datasetService.findPublishedByOwnerId(dataverseId)) {
            if (dataset != null) {
                DatasetVersion releasedVersion = dataset.getReleasedVersion();
                
                if (releasedVersion != null) {
                    if (datasetService.isDatasetCardImageAvailable(releasedVersion.getId(), session)) {
                        return true;
                    }
                }
            }
        }        
        
        return false; 
    }
        
    private File getLogo(Dataverse dataverse) {
        if (dataverse.getId() == null) {
            return null; 
        }
        
        DataverseTheme theme = dataverse.getDataverseTheme(); 
        if (theme != null && theme.getLogo() != null && !theme.getLogo().equals("")) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
  
            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File (domainRoot + File.separator + 
                    "docroot" + File.separator + 
                    "logos" + File.separator + 
                    dataverse.getLogoOwnerId() + File.separator + 
                    theme.getLogo());
            }
        }
            
        return null;         
    }
}  
