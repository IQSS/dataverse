/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
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
public class DataFileServiceBean implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(DataFileServiceBean.class.getCanonicalName());
    @EJB
    DatasetServiceBean datasetService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    private static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";

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
                
        logger.fine("UUID value: "+uid.toString());
        
        // last 6 bytes, of the random UUID, in hex: 
        
        String hexRandom = uid.toString().substring(24);
        
        logger.fine("UUID (last 6 bytes, 12 hex digits): "+hexRandom);
        
        String hexTimestamp = Long.toHexString(new Date().getTime());
        
        logger.fine("(not UUID) timestamp in hex: "+hexTimestamp);
            
        storageIdentifier = hexTimestamp + "-" + hexRandom;
        
        logger.fine("timestamp/UUID hybrid: "+storageIdentifier);
        return storageIdentifier; 
    }
    
    public boolean isSpssPorFile (DataFile file) {
        return MIME_TYPE_SPSS_POR.equalsIgnoreCase(file.getContentType());
    }
    
    public boolean isSpssSavFile (DataFile file) {
        return MIME_TYPE_SPSS_SAV.equalsIgnoreCase(file.getContentType());
    }
    
    /*
    public boolean isSpssPorFile (FileMetadata fileMetadata) {
        if (fileMetadata != null && fileMetadata.getDataFile() != null) {
            return isSpssPorFile(fileMetadata.getDataFile());
        }
        return false; 
    }
    */
    
    
    /* 
     * This method tells you if thumbnail generation is *supported* 
     * on this type of file. i.e., if true, it does not guarantee that a thumbnail 
     * can/will be generated; but it means that we can try. 
     */
    public boolean thumbnailSupported (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
        
        // Some browsers (Chrome?) seem to identify FITS files as mime
        // type "image/fits" on upload; this is both incorrect (the official
        // mime type for FITS is "application/fits", and problematic: then
        // the file is identified as an image, and the page will attempt to 
        // generate a preview - which of course is going to fail...
        if ("image/fits".equalsIgnoreCase(contentType)) {
            return false;
        }
        // besides most image/* types, we can generate thumbnails for 
        // pdf and "world map" files:
        
        return (contentType != null && 
                (contentType.startsWith("image/") || 
                contentType.equalsIgnoreCase("application/pdf") ||
                contentType.equalsIgnoreCase("application/zipped-shapefile")));
    }
    
    /* 
     * This method will return true if the thumbnail is *actually available* and
     * ready to be downloaded. (it will try to generate a thumbnail for supported
     * file types, if not yet available)
    */
    public boolean isThumbnailAvailable (DataFile file) {
        if (!thumbnailSupported(file)) {
            return false;
        }
        
       return ImageThumbConverter.isThumbnailAvailable(file);      
    }
    
    /* 
     * TODO: 
     * similar method, but for non-default thumbnail sizes:
    */
    
    public boolean isThumbnailAvailableForSize (DataFile file) {
        return false; 
    }
    
        
}
