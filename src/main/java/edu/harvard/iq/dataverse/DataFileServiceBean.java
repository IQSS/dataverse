/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Leonid Andreev
 * 
 * Basic skeleton of the new DataFile service for DVN 4.0
 * 
 */

@Stateless
@Named
public class DataFileServiceBean {
    
    private static final Logger logger = Logger.getLogger(DataFileServiceBean.class.getCanonicalName());
    @EJB
    DatasetServiceBean datasetService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataFile find(Object pk) {
        return (DataFile) em.find(DataFile.class, pk);
    }   
    
    /*public DataFile findByMD5(String md5Value){
        if (md5Value == null){
            return null;
        }
        Query query = em.createQuery("select object(o) from DataFile as o where o.md5 =:md5Value order by o.id");
        query.setParameter("md5Value", md5Value);
        return (DataFile)query.getSingleResult();
        
    }*/
    
    public List<DataFile> findByDatasetId(Long studyId) {
        /* 
           Sure, we don't have *studies* any more, in 4.0; it's a tribute 
           to the past. -- L.A.
        */
        Query query = em.createQuery("select object(o) from DataFile as o where o.dataset.id =:studyId order by o.id");
        query.setParameter("studyId", studyId);
        return query.getResultList();
    }  

    public DataTable findDataTableByFileId(Long fileId) {
        Query query = em.createQuery("select object(o) from DataTable as o where o.dataFile.id =:fileId order by o.id");
        query.setParameter("fileId", fileId);
        return (DataTable)query.getSingleResult();
    }
    
    public List<DataFile> findAll() {
        return em.createQuery("select object(o) from DataFile as o order by o.id").getResultList();
    }
    
    public DataFile save(DataFile dataFile) {
            
        DataFile savedDataFile = em.merge(dataFile);
        return savedDataFile;
    }
    
    public Boolean isPreviouslyPublished(Long fileId){
        Query query = em.createQuery("select object(o) from FileMetadata as o where o.dataFile.id =:fileId");
        query.setParameter("fileId", fileId);
        List retList = query.getResultList();
        return (retList.size() > 1);
    }
    
    public void deleteFromVersion( DatasetVersion d, DataFile f ) {
		em.createNamedQuery("DataFile.removeFromDatasetVersion")
			.setParameter("versionId", d.getId()).setParameter("fileId", f.getId())
				.executeUpdate();
    }

    public void generateStorageIdentifier(DataFile dataFile) {
        dataFile.setFileSystemName(generateStorageIdentifier());
    }
    
    public String generateStorageIdentifier() {
        String storageIdentifier = null; 
        
        UUID uid = UUID.randomUUID();
                
        logger.info("UUID value: "+uid.toString());
        
        // last 6 bytes, of the random UUID, in hex: 
        
        String hexRandom = uid.toString().substring(24);
        
        logger.info("UUID (last 6 bytes, 12 hex digits): "+hexRandom);
        
        String hexTimestamp = Long.toHexString(new Date().getTime());
        
        logger.info("(not UUID) timestamp in hex: "+hexTimestamp);
            
        storageIdentifier = hexTimestamp + "-" + hexRandom;
        
        logger.info("timestamp/UUID hybrid: "+storageIdentifier);
        return storageIdentifier; 
    }
        
}
