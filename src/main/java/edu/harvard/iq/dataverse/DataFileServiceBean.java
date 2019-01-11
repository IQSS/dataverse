package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import org.apache.commons.lang.RandomStringUtils;

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
    DvObjectServiceBean dvObjectService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    UserServiceBean userService; 
    @EJB
    SettingsServiceBean settingsService;
    
    @EJB 
    IngestServiceBean ingestService;
    
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
    private static final String FILE_CLASS_PACKAGE = "package";
    private static final String FILE_CLASS_OTHER = "other";

    // Assorted useful mime types:
    
    // 3rd-party and/or proprietary tabular data formasts that we know
    // how to ingest: 
    
    private static final String MIME_TYPE_STATA = "application/x-stata";
    private static final String MIME_TYPE_STATA13 = "application/x-stata-13";
    private static final String MIME_TYPE_RDATA = "application/x-rlang-transport";
    private static final String MIME_TYPE_CSV   = "text/csv";
    private static final String MIME_TYPE_CSV_ALT = "text/comma-separated-values";
    private static final String MIME_TYPE_TSV   = "text/tsv";
    private static final String MIME_TYPE_TSV_ALT   = "text/tab-separated-values";
    private static final String MIME_TYPE_XLSX  = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    private static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    
    // Tabular data formats we don't know how to ingets, but still recognize
    // as "tabular data":
    // TODO: - add more to this list? -- L.A. 4.0 beta13
    
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

    // Network Data files: 
    // (only GRAPHML at this point): 
    
    private static final String MIME_TYPE_NETWORK_GRAPHML = "text/xml-graphml";
   
    // SHAPE file type: 
    // this is the only supported file type in the GEO DATA class:
        
    private static final String MIME_TYPE_ZIP   = "application/zip";
    
    private static final String MIME_TYPE_UNDETERMINED_DEFAULT = "application/octet-stream";
    private static final String MIME_TYPE_UNDETERMINED_BINARY = "application/binary";

    /**
     * Per https://en.wikipedia.org/wiki/Media_type#Vendor_tree just "dataverse"
     * should be fine.
     *
     * @todo Consider registering this at http://www.iana.org/form/media-types
     * or switch to "prs" which "includes media types created experimentally or
     * as part of products that are not distributed commercially" according to
     * the page URL above.
     */
    public static final String MIME_TYPE_PACKAGE_FILE = "application/vnd.dataverse.file-package";
    
    public DataFile find(Object pk) {
        return em.find(DataFile.class, pk);
    }   
    
    /*public DataFile findByMD5(String md5Value){
        if (md5Value == null){
            return null;
        }
        Query query = em.createQuery("select object(o) from DataFile as o where o.md5 =:md5Value order by o.id");
        query.setParameter("md5Value", md5Value);
        return (DataFile)query.getSingleResult();
        
    }*/
    
    public DataFile findByGlobalId(String globalId) {
            return (DataFile) dvObjectService.findByGlobalId(globalId, DataFile.DATAFILE_DTYPE_STRING);
    }
    
    public DataFile findReplacementFile(Long previousFileId){
        Query query = em.createQuery("select object(o) from DataFile as o where o.previousDataFileId = :previousFileId");
        query.setParameter("previousFileId", previousFileId);
        try {
            DataFile retVal = (DataFile)query.getSingleResult();
            return retVal;
        } catch(Exception ex) {
            return null;
        }
    }

    
    public DataFile findPreviousFile(DataFile df){
        TypedQuery<DataFile> query = em.createQuery("select o from DataFile o" + " WHERE o.id = :dataFileId", DataFile.class);
        query.setParameter("dataFileId", df.getPreviousDataFileId());
        try {
            DataFile retVal = query.getSingleResult();
            return retVal;
        } catch(Exception ex) {
            return null;
        }
    }
    
    public List<DataFile> findByDatasetId(Long studyId) {
        /* 
           Sure, we don't have *studies* any more, in 4.0; it's a tribute 
           to the past. -- L.A.
        */
        String qr = "select o from DataFile o where o.owner.id = :studyId order by o.id";
        return em.createQuery(qr, DataFile.class)
                .setParameter("studyId", studyId).getResultList();
    }
    
    public List<DataFile> findAllRelatedByRootDatafileId(Long datafileId) {
        /* 
         Get all files with the same root datafile id
         the first file has its own id as root so only one query needed.
        */
        String qr = "select o from DataFile o where o.rootDataFileId = :datafileId order by o.id";
        return em.createQuery(qr, DataFile.class)
                .setParameter("datafileId", datafileId).getResultList();
    }

    public DataFile findByStorageIdandDatasetVersion(String storageId, DatasetVersion dv) {
        try {
            Query query = em.createNativeQuery("select o.id from dvobject o, filemetadata m " +
                    "where o.storageidentifier = '" + storageId + "' and o.id = m.datafile_id and m.datasetversion_id = " +
                    dv.getId() + "");
            query.setMaxResults(1);
            if (query.getResultList().size() < 1) {
                return null;
            } else {
                return findCheapAndEasy((Long) query.getSingleResult());
                //Pretty sure the above return will always error due to a conversion error
                //I "reverted" my change because I ended up not using this, but here is the fix below --MAD
//                Integer qr = (Integer) query.getSingleResult();
//                return findCheapAndEasy(qr.longValue());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error finding datafile by storageID and DataSetVersion: " + e.getMessage());
            return null;
        }
    }
    
    public List<FileMetadata> findFileMetadataByDatasetVersionId(Long datasetVersionId, int maxResults, String userSuppliedSortField, String userSuppliedSortOrder) {
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);
        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();
        if (maxResults < 0) {
            // return all results if user asks for negative number of results
            maxResults = 0;
        }
        String qr = "select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." + sortField + " " + sortOrder;
        return em.createQuery(qr, FileMetadata.class)
                    .setParameter("datasetVersionId", datasetVersionId)
                    .setMaxResults(maxResults)
                    .getResultList();
    }
    
    public List<FileMetadata> findFileMetadataByDatasetVersionIdLabelSearchTerm(Long datasetVersionId, String searchTerm, String userSuppliedSortField, String userSuppliedSortOrder){
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);

        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();
        String searchClause = "";
        if(searchTerm != null && !searchTerm.isEmpty()){
            searchClause = " and  (lower(o.label) like '%" + searchTerm.toLowerCase() + "%' or lower(o.description) like '%" + searchTerm.toLowerCase() + "%')";
        }
        
        String queryString = "select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId"
                + searchClause
                + " order by o." + sortField + " " + sortOrder;
        return em.createQuery(queryString, FileMetadata.class) 
            .setParameter("datasetVersionId", datasetVersionId)
            .getResultList();
    }
    
    public List<Integer> findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(Long datasetVersionId, String searchTerm, String userSuppliedSortField, String userSuppliedSortOrder){
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);
        
        searchTerm=searchTerm.trim();
        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();
        String searchClause = "";
        if(searchTerm != null && !searchTerm.isEmpty()){
            searchClause = " and  (lower(o.label) like '%" + searchTerm.toLowerCase() + "%' or lower(o.description) like '%" + searchTerm.toLowerCase() + "%')";
        }
        
        //the createNativeQuary takes persistant entities, which Integer.class is not,
        //which is causing the exception. Hence, this query does not need an Integer.class
        //as the second parameter. 
        return em.createNativeQuery("select o.id from FileMetadata o where o.datasetVersion_id = "  + datasetVersionId
                + searchClause
                + " order by o." + sortField + " " + sortOrder)
                .getResultList();
    }
        
    
    public List<FileMetadata> findFileMetadataByDatasetVersionIdLazy(Long datasetVersionId, int maxResults, String userSuppliedSortField, String userSuppliedSortOrder, int firstResult) {
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);
        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();

        if (maxResults < 0) {
            // return all results if user asks for negative number of results
            maxResults = 0;
        }
        return em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." + sortField + " " + sortOrder, FileMetadata.class)
                .setParameter("datasetVersionId", datasetVersionId)
                .setMaxResults(maxResults)
                .setFirstResult(firstResult)
                .getResultList();
    }
    
    public Long findCountByDatasetVersionId(Long datasetVersionId){
        return (Long) em.createNativeQuery("select count(*)  from FileMetadata fmd "
                + " where fmd.datasetVersion_id = " + datasetVersionId
                + ";").getSingleResult();
    }

    public FileMetadata findFileMetadata(Long fileMetadataId) {
        return em.find(FileMetadata.class, fileMetadataId);
    }
    
    public FileMetadata findFileMetadataByDatasetVersionIdAndDataFileId(Long datasetVersionId, Long dataFileId) {

        Query query = em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId  and o.dataFile.id = :dataFileId");
        query.setParameter("datasetVersionId", datasetVersionId);
        query.setParameter("dataFileId", dataFileId);
        try {
            FileMetadata retVal = (FileMetadata) query.getSingleResult();
            return retVal;
        } catch(Exception ex) {
            return null;
        }
    }

    public FileMetadata findMostRecentVersionFileIsIn(DataFile file) {
        if (file == null) {
            return null;
        }
        List<FileMetadata> fileMetadatas = file.getFileMetadatas();
        if (fileMetadatas == null || fileMetadatas.isEmpty()) {
            return null;
        } else {
            return fileMetadatas.get(0);
        }
    }

    public DataFile findCheapAndEasy(Long id) {
        DataFile dataFile;

        Object[] result;

        try {
            result = (Object[]) em.createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, t0.PREVIEWIMAGEAVAILABLE, t1.CONTENTTYPE, t0.STORAGEIDENTIFIER, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, t1.RESTRICTED, t3.ID, t2.AUTHORITY, t2.IDENTIFIER, t1.CHECKSUMTYPE, t1.PREVIOUSDATAFILEID, t1.ROOTDATAFILEID, t0.AUTHORITY, T0.PROTOCOL, T0.IDENTIFIER FROM DVOBJECT t0, DATAFILE t1, DVOBJECT t2, DATASET t3 WHERE ((t0.ID = " + id + ") AND (t0.OWNER_ID = t2.ID) AND (t2.ID = t3.ID) AND (t1.ID = t0.ID))").getSingleResult();
        } catch (Exception ex) {
            return null;
        }

        if (result == null) {
            return null;
        }

        Integer file_id = (Integer) result[0];

        dataFile = new DataFile();
        dataFile.setMergeable(false);

        dataFile.setId(file_id.longValue());

        Timestamp createDate = (Timestamp) result[1];
        Timestamp indexTime = (Timestamp) result[2];
        Timestamp modificationTime = (Timestamp) result[3];
        Timestamp permissionIndexTime = (Timestamp) result[4];
        Timestamp permissionModificationTime = (Timestamp) result[5];
        Timestamp publicationDate = (Timestamp) result[6];

        dataFile.setCreateDate(createDate);
        dataFile.setIndexTime(indexTime);
        dataFile.setModificationTime(modificationTime);
        dataFile.setPermissionIndexTime(permissionIndexTime);
        dataFile.setPermissionModificationTime(permissionModificationTime);
        dataFile.setPublicationDate(publicationDate);

        // no support for users yet!
        // (no need to - so far? -- L.A. 4.2.2) 
        /*
         Long creatorId = (Long) result[7];
         if (creatorId != null) {
         AuthenticatedUser creator = userMap.get(creatorId);
         if (creator == null) {
         creator = userService.find(creatorId);
         if (creator != null) {
         userMap.put(creatorId, creator);
         }
         }
         if (creator != null) {
         dataFile.setCreator(creator);
         }
         }

         Long releaseUserId = (Long) result[8];
         if (releaseUserId != null) {
         AuthenticatedUser releaseUser = userMap.get(releaseUserId);
         if (releaseUser == null) {
         releaseUser = userService.find(releaseUserId);
         if (releaseUser != null) {
         userMap.put(releaseUserId, releaseUser);
         }
         }
         if (releaseUser != null) {
         dataFile.setReleaseUser(releaseUser);
         }
         }
         */
        Boolean previewAvailable = (Boolean) result[9];
        if (previewAvailable != null) {
            dataFile.setPreviewImageAvailable(previewAvailable);
        }
        
        String contentType = (String) result[10];
        
        if (contentType != null) {
            dataFile.setContentType(contentType);
        }

        String storageIdentifier = (String) result[11];

        if (storageIdentifier != null) {
            dataFile.setStorageIdentifier(storageIdentifier);
        }

        Long fileSize = (Long) result[12];

        if (fileSize != null) {
            dataFile.setFilesize(fileSize);
        }

        if (result[13] != null) {
            String ingestStatusString = (String) result[13];
            dataFile.setIngestStatus(ingestStatusString.charAt(0));
        }

        String md5 = (String) result[14];

        if (md5 != null) {
            dataFile.setChecksumValue(md5);
        }

        Boolean restricted = (Boolean) result[15];
        if (restricted != null) {
            dataFile.setRestricted(restricted);
        }


        Dataset owner = new Dataset();

        
        // TODO: check for nulls
        owner.setId((Long)result[16]);
        owner.setAuthority((String)result[17]);
        owner.setIdentifier((String)result[18]);

        String checksumType = (String) result[19];
        if (checksumType != null) {
            try {
                // In the database we store "SHA1" rather than "SHA-1".
                DataFile.ChecksumType typeFromStringInDatabase = DataFile.ChecksumType.valueOf(checksumType);
                dataFile.setChecksumType(typeFromStringInDatabase);
            } catch (IllegalArgumentException ex) {
                logger.info("Exception trying to convert " + checksumType + " to enum: " + ex);
            }
        }
        
        Long previousDataFileId = (Long) result[20];
        if (previousDataFileId != null){
            dataFile.setPreviousDataFileId(previousDataFileId);
        }
        
        Long rootDataFileId = (Long) result[21];
        if (rootDataFileId != null){
            dataFile.setRootDataFileId(rootDataFileId);
        } 
        
        String authority = (String) result[22];
        if (authority != null) {
            dataFile.setAuthority(authority);
        }

        String protocol = (String) result[23];
        if (protocol != null) {
            dataFile.setProtocol(protocol);
        }

        String identifier = (String) result[24];
        if (identifier != null) {
            dataFile.setIdentifier(identifier);
        }
                
        dataFile.setOwner(owner);

        // If content type indicates it's tabular data, spend 2 extra queries 
        // looking up the data table and tabular tags objects:
        
        if (MIME_TYPE_TSV.equalsIgnoreCase(contentType)) {
            Object[] dtResult;
            try {
                dtResult = (Object[]) em.createNativeQuery("SELECT ID, UNF, CASEQUANTITY, VARQUANTITY, ORIGINALFILEFORMAT, ORIGINALFILESIZE FROM dataTable WHERE DATAFILE_ID = " + id).getSingleResult();
            } catch (Exception ex) {
                dtResult = null;
            }
        
            if (dtResult != null) {
                DataTable dataTable = new DataTable(); 

                dataTable.setId(((Integer) dtResult[0]).longValue());
            
                dataTable.setUnf((String)dtResult[1]);
            
                dataTable.setCaseQuantity((Long)dtResult[2]);
            
                dataTable.setVarQuantity((Long)dtResult[3]);
            
                dataTable.setOriginalFileFormat((String)dtResult[4]);
                
                dataTable.setOriginalFileSize((Long)dtResult[5]);
                
                dataTable.setDataFile(dataFile);
                dataFile.setDataTable(dataTable);
                
                // tabular tags: 
                
                List<Object[]> tagResults;
                try {
                    tagResults = em.createNativeQuery("SELECT t.TYPE, t.DATAFILE_ID FROM DATAFILETAG t WHERE t.DATAFILE_ID = " + id).getResultList();
                } catch (Exception ex) {
                    logger.info("EXCEPTION looking up tags.");
                    tagResults = null;
                }
                
                if (tagResults != null) {
                    List<String> fileTagLabels = DataFileTag.listTags();
                    
                    for (Object[] tagResult : tagResults) {
                        Integer tagId = (Integer)tagResult[0];
                        DataFileTag tag = new DataFileTag();
                        tag.setTypeByLabel(fileTagLabels.get(tagId));
                        tag.setDataFile(dataFile);
                        dataFile.addTag(tag);
                    }
                }
            }
        }
        
        return dataFile;
    }
    /* 
     * This is an experimental method for populating the versions of 
     * the datafile with the filemetadatas, optimized for making as few db 
     * queries as possible. 
     * It should only be used to retrieve filemetadata for the DatasetPage!
     * It is not guaranteed to adequately perform anywhere else. 
    */
    public void findFileMetadataOptimizedExperimental(Dataset owner) {
        findFileMetadataOptimizedExperimental(owner, null);
    }
    public void findFileMetadataOptimizedExperimental(Dataset owner, DatasetVersion requestedVersion) {
        List<DataFile> dataFiles = new ArrayList<>();
        List<DataTable> dataTables = new ArrayList<>();
        //List<FileMetadata> retList = new ArrayList<>(); 
        
        // TODO: 
        //  replace these maps with simple lists and run binary search on them. -- 4.2.1
        
        Map<Long, AuthenticatedUser> userMap = new HashMap<>(); 
        Map<Long, Integer> filesMap = new HashMap<>();
        Map<Long, Integer> datatableMap = new HashMap<>();
        Map<Long, Integer> categoryMap = new HashMap<>();
        Map<Long, Set<Integer>> fileTagMap = new HashMap<>();
        
        List<String> fileTagLabels = DataFileTag.listTags();
        
        
        int i = 0; 
        
        List<Object[]> dataTableResults = em.createNativeQuery("SELECT t0.ID, t0.DATAFILE_ID, t0.UNF, t0.CASEQUANTITY, t0.VARQUANTITY, t0.ORIGINALFILEFORMAT, t0.ORIGINALFILESIZE FROM dataTable t0, dataFile t1, dvObject t2 WHERE ((t0.DATAFILE_ID = t1.ID) AND (t1.ID = t2.ID) AND (t2.OWNER_ID = " + owner.getId() + ")) ORDER BY t0.ID").getResultList();
        
        for (Object[] result : dataTableResults) {
            DataTable dataTable = new DataTable(); 
            long fileId = ((Number) result[1]).longValue();

            dataTable.setId(((Number) result[1]).longValue());
            
            dataTable.setUnf((String)result[2]);
            
            dataTable.setCaseQuantity((Long)result[3]);
            
            dataTable.setVarQuantity((Long)result[4]);
            
            dataTable.setOriginalFileFormat((String)result[5]);
            
            dataTable.setOriginalFileSize((Long)result[6]);
            
            dataTables.add(dataTable);
            datatableMap.put(fileId, i++);
            
        }
        
        logger.fine("Retrieved "+dataTables.size()+" DataTable objects.");
        
        i = 0; 
        List<Object[]> dataTagsResults = em.createNativeQuery("SELECT t0.DATAFILE_ID, t0.TYPE FROM DataFileTag t0, dvObject t1 WHERE (t1.ID = t0.DATAFILE_ID) AND (t1.OWNER_ID="+ owner.getId() + ")").getResultList();
        for (Object[] result : dataTagsResults) {
            Long datafile_id = (Long) result[0];
            Integer tagtype_id = (Integer) result[1];
            if (fileTagMap.get(datafile_id) == null) {
                fileTagMap.put(datafile_id, new HashSet<>());
            }
            fileTagMap.get(datafile_id).add(tagtype_id);
            i++; 
        }
        dataTagsResults = null;
        
        logger.fine("Retrieved "+i+" data tags.");
        
        i = 0; 
        
        List<Object[]> fileResults = em.createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, t1.CONTENTTYPE, t0.STORAGEIDENTIFIER, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, t1.RESTRICTED, t1.CHECKSUMTYPE, t1.PREVIOUSDATAFILEID, t1.ROOTDATAFILEID, t0.PROTOCOL, t0.AUTHORITY, t0.IDENTIFIER FROM DVOBJECT t0, DATAFILE t1 WHERE ((t0.OWNER_ID = " + owner.getId() + ") AND ((t1.ID = t0.ID) AND (t0.DTYPE = 'DataFile'))) ORDER BY t0.ID").getResultList(); 
    
        for (Object[] result : fileResults) {
            Integer file_id = (Integer) result[0];
            
            DataFile dataFile = new DataFile();
            dataFile.setMergeable(false);
            
            dataFile.setId(file_id.longValue());
            
            Timestamp createDate = (Timestamp) result[1];
            Timestamp indexTime = (Timestamp) result[2];
            Timestamp modificationTime = (Timestamp) result[3];
            Timestamp permissionIndexTime = (Timestamp) result[4];
            Timestamp permissionModificationTime = (Timestamp) result[5];
            Timestamp publicationDate = (Timestamp) result[6];
            
            dataFile.setCreateDate(createDate);
            dataFile.setIndexTime(indexTime);
            dataFile.setModificationTime(modificationTime);
            dataFile.setPermissionIndexTime(permissionIndexTime);
            dataFile.setPermissionModificationTime(permissionModificationTime);
            dataFile.setPublicationDate(publicationDate);
            
            Long creatorId = (Long) result[7]; 
            if (creatorId != null) {
                AuthenticatedUser creator = userMap.get(creatorId);
                if (creator == null) {
                    creator = userService.find(creatorId);
                    if (creator != null) {
                        userMap.put(creatorId, creator);
                    }
                }
                if (creator != null) {
                    dataFile.setCreator(creator);
                }
            }
            
            dataFile.setOwner(owner);
            
            Long releaseUserId = (Long) result[8]; 
            if (releaseUserId != null) {
                AuthenticatedUser releaseUser = userMap.get(releaseUserId);
                if (releaseUser == null) {
                    releaseUser = userService.find(releaseUserId);
                    if (releaseUser != null) {
                        userMap.put(releaseUserId, releaseUser);
                    }
                }
                if (releaseUser != null) {
                    dataFile.setReleaseUser(releaseUser);
                }
            }
            
            String contentType = (String) result[9]; 
            
            if (contentType != null) {
                dataFile.setContentType(contentType);
            }
            
            String storageIdentifier = (String) result[10];
            
            if (storageIdentifier != null) {
                dataFile.setStorageIdentifier(storageIdentifier);
            }
            
            Long fileSize = (Long) result[11];
            
            if (fileSize != null) {
                dataFile.setFilesize(fileSize);
            }
            
            if (result[12] != null) {
                String ingestStatusString = (String) result[12];
                dataFile.setIngestStatus(ingestStatusString.charAt(0));
            }
            
            String md5 = (String) result[13]; 
            
            if (md5 != null) {
                dataFile.setChecksumValue(md5);
            }
            
            Boolean restricted = (Boolean) result[14];
            if (restricted != null) {
                dataFile.setRestricted(restricted);
            }

            String checksumType = (String) result[15];
            if (checksumType != null) {
                try {
                    // In the database we store "SHA1" rather than "SHA-1".
                    DataFile.ChecksumType typeFromStringInDatabase = DataFile.ChecksumType.valueOf(checksumType);
                    dataFile.setChecksumType(typeFromStringInDatabase);
                } catch (IllegalArgumentException ex) {
                    logger.info("Exception trying to convert " + checksumType + " to enum: " + ex);
                }
            }

            Long previousDataFileId = (Long) result[16];
            if (previousDataFileId != null) {
                dataFile.setPreviousDataFileId(previousDataFileId);
            }
            
            Long rootDataFileId = (Long) result[17];
            if (rootDataFileId != null) {
                dataFile.setRootDataFileId(rootDataFileId);
            }
            
            String protocol = (String) result[18];
            if (protocol != null) {
                dataFile.setProtocol(protocol);
            }
            
            String authority = (String) result[19];
            if (authority != null) {
                dataFile.setAuthority(authority);
            }
            
            String identifier = (String) result[20];
            if (identifier != null) {
                dataFile.setIdentifier(identifier);
            }
            
            // TODO: 
            // - if ingest status is "bad", look up the ingest report; 
            // - is it a dedicated thumbnail for the dataset? (do we ever need that info?? - not on the dataset page, I don't think...)
            
            // Is this a tabular file? 
            
            if (datatableMap.get(dataFile.getId()) != null) {
                dataTables.get(datatableMap.get(dataFile.getId())).setDataFile(dataFile);
                dataFile.setDataTable(dataTables.get(datatableMap.get(dataFile.getId())));
                
            }            

            if (fileTagMap.get(dataFile.getId()) != null) {
                for (Integer tag_id : fileTagMap.get(dataFile.getId())) {
                    DataFileTag tag = new DataFileTag();
                    tag.setTypeByLabel(fileTagLabels.get(tag_id));
                    tag.setDataFile(dataFile);
                    dataFile.addTag(tag);
                }
            }            
            dataFile.setFileAccessRequesters(retrieveFileAccessRequesters(dataFile));              
            dataFiles.add(dataFile);
            filesMap.put(dataFile.getId(), i++);
        }
        fileResults = null;
        
        logger.fine("Retrieved and cached "+i+" datafiles.");

        i = 0; 
        for (DataFileCategory fileCategory : owner.getCategories()) {
            //logger.fine("category: id="+fileCategory.getId());
            categoryMap.put(fileCategory.getId(), i++);
        }
        
        logger.fine("Retrieved "+i+" file categories attached to the dataset.");
        
        if (requestedVersion != null) {
            requestedVersion.setFileMetadatas(retrieveFileMetadataForVersion(owner, requestedVersion, dataFiles, filesMap, categoryMap));
        } else {
            for (DatasetVersion version : owner.getVersions()) {
                version.setFileMetadatas(retrieveFileMetadataForVersion(owner, version, dataFiles, filesMap, categoryMap));
                logger.fine("Retrieved "+version.getFileMetadatas().size()+" filemetadatas for the version "+version.getId());
            }
        }
        owner.setFiles(dataFiles);
    }
    
     private List<AuthenticatedUser> retrieveFileAccessRequesters(DataFile fileIn){
        List<AuthenticatedUser> retList = new ArrayList<>();
        
        List<Object> requesters  = em.createNativeQuery("select authenticated_user_id from fileaccessrequests where datafile_id = "+fileIn.getId()).getResultList();
        
        for (Object userIdObj : requesters){
            Long userId = (Long) userIdObj;
            AuthenticatedUser user = userService.find(userId);
            if (user != null){
                retList.add(user);
            }
        }
        
        return retList;
    }
    
    private List<FileMetadata> retrieveFileMetadataForVersion(Dataset dataset, DatasetVersion version, List<DataFile> dataFiles, Map<Long, Integer> filesMap, Map<Long, Integer> categoryMap) {
        List<FileMetadata> retList = new ArrayList<>();
        Map<Long, Set<Long>> categoryMetaMap = new HashMap<>();
        
        List<Object[]> categoryResults = em.createNativeQuery("select t0.filecategories_id, t0.filemetadatas_id from filemetadata_datafilecategory t0, filemetadata t1 where (t0.filemetadatas_id = t1.id) AND (t1.datasetversion_id = "+version.getId()+")").getResultList();
        int i = 0;
        for (Object[] result : categoryResults) {
            Long category_id = (Long) result[0];
            Long filemeta_id = (Long) result[1];
            if (categoryMetaMap.get(filemeta_id) == null) {
                categoryMetaMap.put(filemeta_id, new HashSet<>());
            }
            categoryMetaMap.get(filemeta_id).add(category_id);
            i++;
        }
        logger.fine("Retrieved and mapped "+i+" file categories attached to files in the version "+version.getId());
        
        List<Object[]> metadataResults = em.createNativeQuery("select id, datafile_id, DESCRIPTION, LABEL, RESTRICTED, DIRECTORYLABEL, prov_freeform from FileMetadata where datasetversion_id = "+version.getId() + " ORDER BY LABEL").getResultList();
        
        for (Object[] result : metadataResults) {
            Integer filemeta_id = (Integer) result[0];
            
            if (filemeta_id == null) {
                continue;
            }
            
            Long file_id = (Long) result[1];
            if (file_id == null) {
                continue;
            }
            
            Integer file_list_id = filesMap.get(file_id);
            if (file_list_id == null) {
                continue;
            }
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setId(filemeta_id.longValue());
            fileMetadata.setCategories(new LinkedList<>());

            if (categoryMetaMap.get(fileMetadata.getId()) != null) {
                for (Long cat_id : categoryMetaMap.get(fileMetadata.getId())) {
                    if (categoryMap.get(cat_id) != null) {
                        fileMetadata.getCategories().add(dataset.getCategories().get(categoryMap.get(cat_id)));
                    }
                }
            }

            fileMetadata.setDatasetVersion(version);
            
            //fileMetadata.setDataFile(dataset.getFiles().get(file_list_id));
            fileMetadata.setDataFile(dataFiles.get(file_list_id));
            
            String description = (String) result[2]; 
            
            if (description != null) {
                fileMetadata.setDescription(description);
            }
            
            String label = (String) result[3];
            
            if (label != null) {
                fileMetadata.setLabel(label);
            }
                        
            Boolean restricted = (Boolean) result[4];
            if (restricted != null) {
                fileMetadata.setRestricted(restricted);
            }
            
            String dirLabel = (String) result[5];
            if (dirLabel != null){
                fileMetadata.setDirectoryLabel(dirLabel);
            }
            
            String provFreeForm = (String) result[6];
            if (provFreeForm != null){
                fileMetadata.setProvFreeForm(provFreeForm);
            }
                        
            retList.add(fileMetadata);
        }
        
        logger.fine("Retrieved "+retList.size()+" file metadatas for version "+version.getId()+" (inside the retrieveFileMetadataForVersion method).");
                
        
        /* 
            We no longer perform this sort here, just to keep this filemetadata
            list as identical as possible to when it's produced by the "traditional"
            EJB method. When it's necessary to have the filemetadatas sorted by 
            FileMetadata.compareByLabel, the DatasetVersion.getFileMetadatasSorted()
            method should be called. 
        
        Collections.sort(retList, FileMetadata.compareByLabel); */
        
        return retList; 
    }
    
    public List<DataFile> findIngestsInProgress() {
        if ( em.isOpen() ) {
            String qr = "select object(o) from DataFile as o where o.ingestStatus =:scheduledStatusCode or o.ingestStatus =:progressStatusCode order by o.id";
            return em.createQuery(qr, DataFile.class)
                .setParameter("scheduledStatusCode", DataFile.INGEST_STATUS_SCHEDULED)
                .setParameter("progressStatusCode", DataFile.INGEST_STATUS_INPROGRESS)
                .getResultList();
        } else {
            return Collections.emptyList();
        }
    }
    
    
    public DataTable findDataTableByFileId(Long fileId) {
        Query query = em.createQuery("select object(o) from DataTable as o where o.dataFile.id =:fileId order by o.id");
        query.setParameter("fileId", fileId);
        
        Object singleResult;
        
        try{
            return (DataTable)query.getSingleResult();
        }catch(NoResultException ex){
            return null;
        }
    }
    
    public List<DataFile> findAll() {
        return em.createQuery("select object(o) from DataFile as o order by o.id", DataFile.class).getResultList();
    }
    
    public DataFile save(DataFile dataFile) {

        if (dataFile.isMergeable()) {   
            DataFile savedDataFile = em.merge(dataFile);
            return savedDataFile;
        } else {
            throw new IllegalArgumentException("This DataFile object has been set to NOT MERGEABLE; please ensure a MERGEABLE object is passed to the save method.");
        } 
    }
    
    private void msg(String m){
        System.out.println(m);
    }
    private void dashes(){
        msg("----------------");
    }
    private void msgt(String m){
        dashes(); msg(m); dashes();
    }
    
    /*
        Make sure the file replace ids are set for a initial version 
        of a file
    
    */
    public DataFile setAndCheckFileReplaceAttributes(DataFile savedDataFile){
               
        // Is this the initial version of a file?
        
        if ((savedDataFile.getRootDataFileId() == null)||
                (savedDataFile.getRootDataFileId().equals(DataFile.ROOT_DATAFILE_ID_DEFAULT))){
            msg("yes, initial version");
 
           // YES!  Set the RootDataFileId to the Id
           savedDataFile.setRootDataFileId(savedDataFile.getId());
           
           // SAVE IT AGAIN!!!
           msg("yes, save again");
        
            return em.merge(savedDataFile);   
           
        }else{       
            // Looking Good Billy Ray! Feeling Good Louis!    
            msg("nope, looks ok");

            return savedDataFile;
        }
    }
    
    
    public Boolean isPreviouslyPublished(Long fileId){
        Query query = em.createQuery("select object(o) from FileMetadata as o where o.dataFile.id =:fileId");
        query.setParameter("fileId", fileId);
        List<?> retList = query.getResultList();
        return (retList.size() > 1);
    }
    
    public void deleteFromVersion( DatasetVersion d, DataFile f ) {
		em.createNamedQuery("DataFile.removeFromDatasetVersion")
			.setParameter("versionId", d.getId()).setParameter("fileId", f.getId())
				.executeUpdate();
    }

    /* 
     Convenience methods for merging and removingindividual file metadatas, 
     without touching the rest of the DataFile object:
    */
    
    public FileMetadata mergeFileMetadata(FileMetadata fileMetadata) {
        
        FileMetadata newFileMetadata = em.merge(fileMetadata);
        em.flush();
        
        // Set the initial value of the rootDataFileId
        //    (does nothing if it's already set)
        //DataFile updatedDataFile = setAndCheckFileReplaceAttributes(newFileMetadata.getDataFile());
               
        return newFileMetadata;
    }
    
    public void removeFileMetadata(FileMetadata fileMetadata) {
        msgt("removeFileMetadata: fileMetadata");
        FileMetadata mergedFM = em.merge(fileMetadata);
        em.remove(mergedFM);
    }
    
    /* 
     * Same, for DataTables:
    */
    
    public DataTable saveDataTable(DataTable dataTable) {
        DataTable merged = em.merge(dataTable);
        em.flush();
        return merged;
    }
    
    public List<DataFile> findHarvestedFilesByClient(HarvestingClient harvestingClient) {
        String qr = "SELECT d FROM DataFile d, DvObject o, Dataset s WHERE o.id = d.id AND o.owner.id = s.id AND s.harvestedFrom.id = :harvestingClientId";
        return em.createQuery(qr, DataFile.class)
            .setParameter("harvestingClientId", harvestingClient.getId())
            .getResultList();
    }
    
    /*moving to the fileutil*/
    
    public void generateStorageIdentifier(DataFile dataFile) {
        dataFile.setStorageIdentifier(generateStorageIdentifier());
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
        return (file != null) ? MIME_TYPE_SPSS_POR.equalsIgnoreCase(file.getContentType()) : false;
    }
    
    public boolean isSpssSavFile (DataFile file) {
        return (file != null) ? MIME_TYPE_SPSS_SAV.equalsIgnoreCase(file.getContentType()) : false;
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
     * This method will return true if the thumbnail is *actually available* and
     * ready to be downloaded. (it will try to generate a thumbnail for supported
     * file types, if not yet available)
     */
    public boolean isThumbnailAvailable (DataFile file) {
        if (file == null) {
            return false; 
        } 

        // If this file already has the "thumbnail generated" flag set,
        // we'll just trust that:
        if (file.isPreviewImageAvailable()) {
            logger.fine("returning true");
            return true;
        }
        
        // If thumbnails are not even supported for this class of files, 
        // there's notthing to talk about:      
        if (!FileUtil.isThumbnailSupported(file)) {
            return false;
        }
        
        /*
         Checking the permission here was resulting in extra queries; 
         it is now the responsibility of the client - such as the DatasetPage - 
         to make sure the permission check out, before calling this method.
         (or *after* calling this method? - checking permissions costs db 
         queries; checking if the thumbnail is available may cost cpu time, if 
         it has to be generated on the fly - so you have to figure out which 
         is more important... 
        
        */
                
        
       if (ImageThumbConverter.isThumbnailAvailable(file)) {
           file = this.find(file.getId());
           file.setPreviewImageAvailable(true);
           this.save(file); 
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
        
        if (isFileClassPackage(file)) {
            return FILE_CLASS_PACKAGE;
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
        
        if (FileUtil.MIME_TYPE_FITSIMAGE.equalsIgnoreCase(contentType)) {
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
        
        return (MIME_TYPE_FITS.equalsIgnoreCase(contentType) || FileUtil.MIME_TYPE_FITSIMAGE.equalsIgnoreCase(contentType));
        
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
        
        return FileUtil.MIME_TYPE_GEO_SHAPE.equalsIgnoreCase(contentType);
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
        if (FileUtil.canIngestAsTabular(file)) {
            return true;
        }
        
        String contentType = file.getContentType();
        
        // And these are the formats we DON'T know how to ingest, 
        // but nevertheless recognize as "tabular data":
        
        return (MIME_TYPE_TSV.equalsIgnoreCase(contentType)
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
    
    public boolean isFileClassPackage (DataFile file) {
        if (file == null) {
            return false;
        }
        
        String contentType = file.getContentType();
       
        return MIME_TYPE_PACKAGE_FILE.equalsIgnoreCase(contentType);
    }
    
    public void populateFileSearchCard(SolrSearchResult solrSearchResult) {
        solrSearchResult.setEntity(this.findCheapAndEasy(solrSearchResult.getEntityId()));
    }
        
    
    /**
     * Does this file have a replacement.  
     * Any file should have AT MOST 1 replacement
     * 
     * @param df
     * @return 
     * @throws java.lang.Exception if a DataFile has more than 1 replacement
     *         or is unpublished and has a replacement.
     */
    public boolean hasReplacement(DataFile df) throws Exception{
        
        if (df.getId() == null){
            // An unsaved file cannot have a replacment
            return false;
        }
       
        
        List<DataFile> dataFiles = em.createQuery("select o from DataFile o" +
                    " WHERE o.previousDataFileId = :dataFileId", DataFile.class)
                    .setParameter("dataFileId", df.getId())
                    .getResultList();
        
        if (dataFiles.isEmpty()){
            return false;
        }
        
         if (!df.isReleased()){
            // An unpublished SHOULD NOT have a replacment
            String errMsg = "DataFile with id: [" + df.getId() + "] is UNPUBLISHED with a REPLACEMENT.  This should NOT happen.";
            logger.severe(errMsg);
            
            throw new Exception(errMsg);
        }

        
        
        else if (dataFiles.size() == 1){
            return true;
        }else{
        
            String errMsg = "DataFile with id: [" + df.getId() + "] has more than one replacment!";
            logger.severe(errMsg);

            throw new Exception(errMsg);
        }
        
    }
    
    public boolean hasBeenDeleted(DataFile df){
        Dataset dataset = df.getOwner();
        DatasetVersion dsv = dataset.getLatestVersion();
        
        return findFileMetadataByDatasetVersionIdAndDataFileId(dsv.getId(), df.getId()) == null;
        
    }
    
    /**
     * Is this a replacement file??
     * 
     * The indication of a previousDataFileId says that it is
     * 
     * @param df
     * @return
     */
    public boolean isReplacementFile(DataFile df) {

        if (df.getPreviousDataFileId() == null){
            return false;
        }else if (df.getPreviousDataFileId() < 1){
            String errMSg = "Stop! previousDataFileId should either be null or a number greater than 0";
            logger.severe(errMSg);
            return false;
            // blow up -- this shouldn't happen!
            //throw new FileReplaceException(errMSg);
        }else if (df.getPreviousDataFileId() > 0){
            return true;
        }
        return false;
    }  // end: isReplacementFile
    
    public List<Long> selectFilesWithMissingOriginalTypes() {
        Query query = em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id AND (t.originalfileformat='" + MIME_TYPE_TSV + "' OR t.originalfileformat IS NULL) ORDER BY f.id");
        
        try {
            return query.getResultList();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
    
    public List<Long> selectFilesWithMissingOriginalSizes() {
        Query query = em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id AND (t.originalfilesize IS NULL ) AND (t.originalfileformat IS NOT NULL) ORDER BY f.id");
        
        try {
            return query.getResultList();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
    
    public String generateDataFileIdentifier(DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String doiIdentifierType = settingsService.getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "randomString");
        String doiDataFileFormat = settingsService.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");

        String prepend = "";
        if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.DEPENDENT.toString())){
            //If format is dependent then pre-pend the dataset identifier 
            prepend = datafile.getOwner().getIdentifier() + "/";
        } else {
            //If there's a shoulder prepend independent identifiers with it
        	prepend = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder, "");
        }
 
        switch (doiIdentifierType) {
            case "randomString":               
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
            case "sequentialNumber":
                if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.INDEPENDENT.toString())){ 
                    return generateIdentifierAsIndependentSequentialNumber(datafile, idServiceBean, prepend);
                } else {
                    return generateIdentifierAsDependentSequentialNumber(datafile, idServiceBean, prepend);
                }
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
        }
    }
    
    private String generateIdentifierAsRandomString(DataFile datafile, GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier = null;
        do {
            identifier = prepend + RandomStringUtils.randomAlphanumeric(6).toUpperCase();  
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }


    private String generateIdentifierAsIndependentSequentialNumber(DataFile datafile, GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier; 
        do {
            StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("Dataset.generateIdentifierAsSequentialNumber");
            query.execute();
            Integer identifierNumeric = (Integer) query.getOutputParameterValue(1); 
            // some diagnostics here maybe - is it possible to determine that it's failing 
            // because the stored procedure hasn't been created in the database?
            if (identifierNumeric == null) {
                return null; 
            }
            identifier = prepend + identifierNumeric.toString();
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));
        
        return identifier;
    }
    
    private String generateIdentifierAsDependentSequentialNumber(DataFile datafile, GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier;
        Long retVal;

        retVal = new Long(0);

        do {
            retVal++;
            identifier = prepend + retVal.toString();

        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network). Also check for duplicate
     * in the remote PID service if needed
     * @param userIdentifier
     * @param datafile
     * @param idServiceBean
     * @return  {@code true} iff the global identifier is unique.
     */
    public boolean isGlobalIdUnique(String userIdentifier, DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String testProtocol = "";
        String testAuthority = "";
        if (datafile.getAuthority() != null){
            testAuthority = datafile.getAuthority();
        } else {
            testAuthority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority);
        }
        if (datafile.getProtocol() != null){
            testProtocol = datafile.getProtocol();
        } else {
            testProtocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol);
        }
        
        boolean u = em.createNamedQuery("DvObject.findByProtocolIdentifierAuthority")
            .setParameter("protocol", testProtocol)
            .setParameter("authority", testAuthority)
            .setParameter("identifier",userIdentifier)
            .getResultList().isEmpty();
            
        try{
            if (idServiceBean.alreadyExists(new GlobalId(testProtocol, testAuthority, userIdentifier))) {
                u = false;
            }
        } catch (Exception e){
            //we can live with failure - means identifier not found remotely
        }

       
        return u;
    }
    
}
