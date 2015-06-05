/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
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
    @EJB
    PermissionServiceBean permissionService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    // File type "classes" tags:
    
    private static final String FILE_CLASS_AUDIO = "audio";
    private static final String FILE_CLASS_CODE = "code";
    private static final String FILE_CLASS_DOCUMENT = "document";
    private static final String FILE_CLASS_ASTRO = "astro";
    private static final String FILE_CLASS_IMAGE = "image";
    private static final String FILE_CLASS_NETWORK = "network";
    private static final String FILE_CLASS_GEO = "geodata";
    private static final String FILE_CLASS_TABULAR = "tabular";
    private static final String FILE_CLASS_VIDEO = "video";
    private static final String FILE_CLASS_OTHER = "other";

    // Assorted useful mime types:
    
    // 3rd-party and/or proprietary tabular data formasts that we know
    // how to ingest: 
    
    private static final String MIME_TYPE_STATA = "application/x-stata";
    private static final String MIME_TYPE_STATA13 = "application/x-stata-13";
    private static final String MIME_TYPE_RDATA = "application/x-rlang-transport";
    private static final String MIME_TYPE_CSV   = "text/csv";
    private static final String MIME_TYPE_CSV_ALT = "text/comma-separated-values";
    private static final String MIME_TYPE_XLSX  = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    private static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    
    // Tabular data formats we don't know how to ingets, but still recognize
    // as "tabular data":
    // TODO: - add more to this list? -- L.A. 4.0 beta13
    
    private static final String MIME_TYPE_TAB   = "text/tab-separated-values";
    private static final String MIME_TYPE_FIXED_FIELD = "text/x-fixed-field";
    private static final String MIME_TYPE_SAS_TRANSPORT = "application/x-sas-transport";
    private static final String MIME_TYPE_SAS_SYSTEM = "application/x-sas-system";
    
    // The following are the "control card/syntax" formats that we recognize 
    // as "code":
    
    private static final String MIME_TYPE_R_SYNTAX = "application/x-r-syntax";
    private static final String MIME_TYPE_STATA_SYNTAX = "text/x-stata-syntax";
    private static final String MIME_TYPE_SPSS_CCARD = "text/x-spss-syntax";
    private static final String MIME_TYPE_SAS_SYNTAX = "text/x-sas-syntax";

    // The types recognized as "documents":
    // TODO: there has to be more! -- L.A. 4.0 beta13
    
    private static final String MIME_TYPE_PLAIN_TEXT = "text/plain";
    private static final String MIME_TYPE_DOCUMENT_PDF = "application/pdf";
    private static final String MIME_TYPE_DOCUMENT_MSWORD = "application/msword";
    private static final String MIME_TYPE_DOCUMENT_MSEXCEL = "application/vnd.ms-excel";
    private static final String MIME_TYPE_DOCUMENT_MSWORD_OPENXML = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    
    // Supported Astrophysics formats: 
    // (only FITS at this point)
    
    private static final String MIME_TYPE_FITS  = "application/fits";
    private static final String MIME_TYPE_FITSIMAGE = "image/fits";
   
    // Network Data files: 
    // (only GRAPHML at this point): 
    
    private static final String MIME_TYPE_NETWORK_GRAPHML = "text/xml-graphml";
   
    // SHAPE file type: 
    // this is the only supported file type in the GEO DATA class:
    
    private static final String MIME_TYPE_GEO_SHAPE = "application/zipped-shapefile";
    
    private static final String MIME_TYPE_ZIP   = "application/zip";
    
    private static final String MIME_TYPE_UNDETERMINED_DEFAULT = "application/octet-stream";
    private static final String MIME_TYPE_UNDETERMINED_BINARY = "application/binary";
    
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
        Query query = em.createQuery("select o from DataFile o where o.owner.id = :studyId order by o.id");
        query.setParameter("studyId", studyId);
        return query.getResultList();
    }  

    public List<DataFile> findIngestsInProgress() {
        if ( em.isOpen() ) {
            Query query = em.createQuery("select object(o) from DataFile as o where o.ingestStatus =:scheduledStatusCode or o.ingestStatus =:progressStatusCode order by o.id");
            query.setParameter("scheduledStatusCode", DataFile.INGEST_STATUS_SCHEDULED);
            query.setParameter("progressStatusCode", DataFile.INGEST_STATUS_INPROGRESS);
            return query.getResultList();
        } else {
            return Collections.emptyList();
        }
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
        
        UUID uid = UUID.randomUUID();
                
        logger.log(Level.FINE, "UUID value: {0}", uid.toString());
        
        // last 6 bytes, of the random UUID, in hex: 
        
        String hexRandom = uid.toString().substring(24);
        
        logger.log(Level.FINE, "UUID (last 6 bytes, 12 hex digits): {0}", hexRandom);
        
        String hexTimestamp = Long.toHexString(new Date().getTime());
        
        logger.log(Level.FINE, "(not UUID) timestamp in hex: {0}", hexTimestamp);
            
        String storageIdentifier = hexTimestamp + "-" + hexRandom;
        
        logger.log(Level.FINE, "timestamp/UUID hybrid: {0}", storageIdentifier);
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
        if (MIME_TYPE_FITSIMAGE.equalsIgnoreCase(contentType)) {
            return false;
        }
        // besides most image/* types, we can generate thumbnails for 
        // pdf and "world map" files:
        
        return (contentType != null && 
                (contentType.startsWith("image/") || 
                contentType.equalsIgnoreCase("application/pdf") ||
                contentType.equalsIgnoreCase(MIME_TYPE_GEO_SHAPE)));
    }
    
    /* 
     * This method will return true if the thumbnail is *actually available* and
     * ready to be downloaded. (it will try to generate a thumbnail for supported
     * file types, if not yet available)
    */
    public boolean isThumbnailAvailable (DataFile file, User user) {
        if (file == null) {
            return false; 
        } 
        
        // If thumbnails are not even supported for this class of files, 
        // there's notthing to talk about: 
        
        if (!thumbnailSupported(file)) {
            return false;
        }
        
        // Also, thumbnails are only shown to users who have permission to see 
        // the full-size image file. So before we do anything else, let's
        // do some authentication and authorization:        
        if (!permissionService.userOn(user, file).has(Permission.DownloadFile)) { 
            logger.fine("No permission to download the file.");
            return false; 
        }
        
        
        
       return ImageThumbConverter.isThumbnailAvailable(file);      
    }

    
    // TODO: 
    // Document this.
    // -- L.A. 4.0 beta14
    
    public boolean isTemporaryPreviewAvailable(String fileSystemId, String mimeType) {
        
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        String fileSystemName = filesRootDirectory + "/temp/" + fileSystemId;
        
        String imageThumbFileName = null;
        
        if ("application/pdf".equals(mimeType)) {
            imageThumbFileName = ImageThumbConverter.generatePDFThumb(fileSystemName);
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            imageThumbFileName = ImageThumbConverter.generateImageThumb(fileSystemName);
        }
        
        if (imageThumbFileName != null) {
            return true; 
        }
            
        return false;
    }
    
    /* 
     * TODO: 
     * similar method, but for non-default thumbnail sizes:
    */
    
    public boolean isThumbnailAvailableForSize (DataFile file) {
        return false; 
    }
    
    public boolean ingestableAsTabular(DataFile dataFile) {
        /* 
         * In the final 4.0 we'll be doing real-time checks, going through the 
         * available plugins and verifying the lists of mime types that they 
         * can handle. In 4.0 beta, the ingest plugins are still built into the 
         * main code base, so we can just go through a hard-coded list of mime 
         * types. -- L.A. 
         */
        
        String mimeType = dataFile.getContentType();
        
        if (mimeType == null) {
            return false;
        }
        
        if (mimeType.equals(MIME_TYPE_STATA)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_STATA13)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_RDATA)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_CSV) || mimeType.equals(MIME_TYPE_CSV_ALT)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_XLSX)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_SPSS_SAV)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_SPSS_POR)) {
            return true;
        }

        return false;
    }
    
    /* 
     * Methods for identifying "classes" (groupings) of files by type:
    */
    
    public String getFileClassById (Long fileId) {
        DataFile file = find(fileId);
        
        if (file == null) {
            return null; 
        }
        
        return getFileClass(file);
    }
    
    public String getFileClass (DataFile file) {
        if (isFileClassImage(file)) {
            return FILE_CLASS_IMAGE;
        }
        
        if (isFileClassVideo(file)) {
            return FILE_CLASS_VIDEO;
        }
        
        if (isFileClassAudio(file)) {
            return FILE_CLASS_AUDIO;
        }
        
        if (isFileClassCode(file)) {
            return FILE_CLASS_CODE;
        }
        
        if (isFileClassDocument(file)) {
            return FILE_CLASS_DOCUMENT;
        }
        
        if (isFileClassAstro(file)) {
            return FILE_CLASS_ASTRO;
        }
        
        if (isFileClassNetwork(file)) {
            return FILE_CLASS_NETWORK;
        }
        
        if (isFileClassGeo(file)) {
            return FILE_CLASS_GEO;
        }
        
        if (isFileClassTabularData(file)) {
            return FILE_CLASS_TABULAR;
        }
        
        
        return FILE_CLASS_OTHER;
    }
    
    
    
    public boolean isFileClassImage (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();

        // Some browsers (Chrome?) seem to identify FITS files as mime
        // type "image/fits" on upload; this is both incorrect (the official
        // mime type for FITS is "application/fits", and problematic: then
        // the file is identified as an image, and the page will attempt to 
        // generate a preview - which of course is going to fail...
        
        if (MIME_TYPE_FITSIMAGE.equalsIgnoreCase(contentType)) {
            return false;
        }
        // besides most image/* types, we can generate thumbnails for 
        // pdf and "world map" files:
        
        return (contentType != null && (contentType.toLowerCase().startsWith("image/")));
    }
    
    public boolean isFileClassAudio (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
        
        // TODO: 
        // verify that there are no audio types that don't start with "audio/" - 
        //  some exotic mp[34]... ?
        
        return (contentType != null && (contentType.toLowerCase().startsWith("audio/")));    
    }
    
    public boolean isFileClassCode (DataFile file) {
        if (file == null) {
            return false;
        }
     
        String contentType = file.getContentType();
        
        // The following are the "control card/syntax" formats that we recognize 
        // as "code":
    
        return (MIME_TYPE_R_SYNTAX.equalsIgnoreCase(contentType)
            || MIME_TYPE_STATA_SYNTAX.equalsIgnoreCase(contentType) 
            || MIME_TYPE_SAS_SYNTAX.equalsIgnoreCase(contentType)
            || MIME_TYPE_SPSS_CCARD.equalsIgnoreCase(contentType));
        
    }
    
    public boolean isFileClassDocument (DataFile file) {
        if (file == null) {
            return false;
        }
        
        // "Documents": PDF, assorted MS docs, etc. 
        
        String contentType = file.getContentType();
        int scIndex = 0;
        if (contentType != null && (scIndex = contentType.indexOf(';')) > 0) {
            contentType = contentType.substring(0, scIndex);
        }
        
        return (MIME_TYPE_PLAIN_TEXT.equalsIgnoreCase(contentType)
            || MIME_TYPE_DOCUMENT_PDF.equalsIgnoreCase(contentType)
            || MIME_TYPE_DOCUMENT_MSWORD.equalsIgnoreCase(contentType)
            || MIME_TYPE_DOCUMENT_MSEXCEL.equalsIgnoreCase(contentType)
            || MIME_TYPE_DOCUMENT_MSWORD_OPENXML.equalsIgnoreCase(contentType));
        
    }
    
    public boolean isFileClassAstro (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
       
        // The only known/supported "Astro" file type is FITS,
        // so far:
        
        return (MIME_TYPE_FITS.equalsIgnoreCase(contentType) || MIME_TYPE_FITSIMAGE.equalsIgnoreCase(contentType));
        
    }
    
    public boolean isFileClassNetwork (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
       
        // The only known/supported Network Data type is GRAPHML,
        // so far:
        
        return MIME_TYPE_NETWORK_GRAPHML.equalsIgnoreCase(contentType);
        
    }
    
    /* 
     * we don't really need a method for "other" - 
     * it's "other" if it fails to identify as any specific class... 
     * (or do we?)
    public boolean isFileClassOther (DataFile file) {
        if (file == null) {
            return false;
        }
        
    }
    */
    
    public boolean isFileClassGeo (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
       
        // The only known/supported Geo Data type is SHAPE,
        // so far:
        
        return MIME_TYPE_GEO_SHAPE.equalsIgnoreCase(contentType);
    }
    
    public boolean isFileClassTabularData (DataFile file) {
        if (file == null) {
            return false;
        }
        
        // "Tabular data" is EITHER an INGESTED tabular data file, i.e.
        // a file with a DataTable and DataVariables; or a DataFile 
        // of one of the many known tabular data formats - SPSS, Stata, etc.
        // that for one reason or another didn't get ingested: 
        
        if (file.isTabularData()) {
            return true; 
        }
        
        // The formats we know how to ingest: 
        if (ingestableAsTabular(file)) {
            return true;
        }
        
        String contentType = file.getContentType();
        
        // And these are the formats we DON'T know how to ingest, 
        // but nevertheless recognize as "tabular data":
        
        return (MIME_TYPE_TAB.equalsIgnoreCase(contentType)
            || MIME_TYPE_FIXED_FIELD.equalsIgnoreCase(contentType) 
            || MIME_TYPE_SAS_TRANSPORT.equalsIgnoreCase(contentType)
            || MIME_TYPE_SAS_SYSTEM.equalsIgnoreCase(contentType));
        
    }
    
    public boolean isFileClassVideo (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
        
        // TODO: 
        // check if there are video types that don't start with "audio/" - 
        // some exotic application/... formats ?
        
        return (contentType != null && (contentType.toLowerCase().startsWith("video/")));    
        
    }
        
}
