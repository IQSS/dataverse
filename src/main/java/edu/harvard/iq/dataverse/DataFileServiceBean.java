/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

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
    @EJB
    UserServiceBean userService; 

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
    
    public List<FileMetadata> findFileMetadataByDatasetVersionId(Long datasetVersionId, int maxResults, String userSuppliedSortField, String userSuppliedSortOrder) {
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);
        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();
        if (maxResults < 0) {
            // return all results if user asks for negative number of results
            maxResults = 0;
        }
        TypedQuery query = em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." + sortField + " " + sortOrder, FileMetadata.class);
        query.setParameter("datasetVersionId", datasetVersionId);
        query.setMaxResults(maxResults);
        return query.getResultList();
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
        TypedQuery query = em.createQuery(queryString, FileMetadata.class); 
        query.setParameter("datasetVersionId", datasetVersionId);
        
        return query.getResultList();
    }
    
    public List<Integer> findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(Long datasetVersionId, String searchTerm, String userSuppliedSortField, String userSuppliedSortOrder){
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);

        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();
        String searchClause = "";
        if(searchTerm != null && !searchTerm.isEmpty()){
            searchClause = " and  (lower(o.label) like '%" + searchTerm.toLowerCase() + "%' or lower(o.description) like '%" + searchTerm.toLowerCase() + "%')";
        }
        
        Query query = em.createNativeQuery("select o.id from FileMetadata o where o.datasetVersion_id = "  + datasetVersionId
                + searchClause
                + " order by o." + sortField + " " + sortOrder);
        //System.out.print(query.toString());
        
        return query.getResultList();
    }
        
    
    public List<FileMetadata> findFileMetadataByDatasetVersionIdLazy(Long datasetVersionId, int maxResults, String userSuppliedSortField, String userSuppliedSortOrder, int firstResult) {
        FileSortFieldAndOrder sortFieldAndOrder = new FileSortFieldAndOrder(userSuppliedSortField, userSuppliedSortOrder);
        String sortField = sortFieldAndOrder.getSortField();
        String sortOrder = sortFieldAndOrder.getSortOrder();

        if (maxResults < 0) {
            // return all results if user asks for negative number of results
            maxResults = 0;
        }
        TypedQuery query = em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." + sortField + " " + sortOrder, FileMetadata.class);
        query.setParameter("datasetVersionId", datasetVersionId);
        query.setMaxResults(maxResults);

        query.setFirstResult(firstResult);

        List retList = query.getResultList();
        return retList;
    }
    
    public Long findCountByDatasetVersionId(Long datasetVersionId){
        return (Long) em.createNativeQuery("select count(*)  from FileMetadata fmd "
                + " where fmd.datasetVersion_id = " + datasetVersionId
                + ";").getSingleResult();
    }

    public FileMetadata findFileMetadataByFileAndVersionId(Long dataFileId, Long datasetVersionId) {
        Query query = em.createQuery("select object(o) from FileMetadata as o where o.dataFile.id = :dataFileId and o.datasetVersion.id = :datasetVersionId");
        query.setParameter("dataFileId", dataFileId);
        query.setParameter("datasetVersionId", datasetVersionId);
        return (FileMetadata)query.getSingleResult();
    }
    
    public FileMetadata findFileMetadataByDatasetVersionIdAndDataFileId(Long datasetVersionId, Long dataFileId) {

        Query query = em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId  and o.dataFile.id = :dataFileId");
        query.setParameter("datasetVersionId", datasetVersionId);
        query.setParameter("dataFileId", dataFileId);

        return (FileMetadata) query.getSingleResult();
    }

    public DataFile findCheapAndEasy(Long id) {
        DataFile dataFile = null;

        Object[] result = null;

        try {
            result = (Object[]) em.createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, t0.PREVIEWIMAGEAVAILABLE, t1.CONTENTTYPE, t1.FILESYSTEMNAME, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, t1.RESTRICTED, t3.ID, t3.AUTHORITY, t3.IDENTIFIER, t1.CHECKSUMTYPE FROM DVOBJECT t0, DATAFILE t1, DVOBJECT t2, DATASET t3 WHERE ((t0.ID = " + id + ") AND (t0.OWNER_ID = t2.ID) AND (t2.ID = t3.ID) AND (t1.ID = t0.ID))").getSingleResult();
        } catch (Exception ex) {
            return null;
        }

        if (result == null) {
            return null;
        }

        Integer file_id = (Integer) result[0];

        dataFile = new DataFile();

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
                
        dataFile.setOwner(owner);

        // look up data table; but only if content type indicates it's tabular data:
        
        if (MIME_TYPE_TAB.equalsIgnoreCase(contentType)) {
            Object[] dtResult = null;
            try {
                dtResult = (Object[]) em.createNativeQuery("SELECT ID, UNF, CASEQUANTITY, VARQUANTITY, ORIGINALFILEFORMAT FROM dataTable WHERE DATAFILE_ID = " + id).getSingleResult();
            } catch (Exception ex) {
                dtResult = null;
            }
        
            if (dtResult != null) {
                DataTable dataTable = new DataTable(); 

                dataTable.setId(((Integer)dtResult[0]).longValue());
            
                dataTable.setUnf((String)dtResult[1]);
            
                dataTable.setCaseQuantity((Long)dtResult[2]);
            
                dataTable.setVarQuantity((Long)dtResult[3]);
            
                dataTable.setOriginalFileFormat((String)dtResult[4]);
                
                dataTable.setDataFile(dataFile);
                dataFile.setDataTable(dataTable);
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
        
        List<Object[]> dataTableResults = em.createNativeQuery("SELECT t0.ID, t0.DATAFILE_ID, t0.UNF, t0.CASEQUANTITY, t0.VARQUANTITY, t0.ORIGINALFILEFORMAT FROM dataTable t0, dataFile t1, dvObject t2 WHERE ((t0.DATAFILE_ID = t1.ID) AND (t1.ID = t2.ID) AND (t2.OWNER_ID = " + owner.getId() + ")) ORDER BY t0.ID").getResultList();
        
        for (Object[] result : dataTableResults) {
            DataTable dataTable = new DataTable(); 
            Long fileId = (Long)result[1];

            dataTable.setId(((Integer)result[0]).longValue());
            
            dataTable.setUnf((String)result[2]);
            
            dataTable.setCaseQuantity((Long)result[3]);
            
            dataTable.setVarQuantity((Long)result[4]);
            
            dataTable.setOriginalFileFormat((String)result[5]);
            
            dataTables.add(dataTable);
            datatableMap.put(fileId, i++);
            
        }
        dataTableResults = null; 
        
        logger.fine("Retrieved "+dataTables.size()+" DataTable objects.");
        
        i = 0; 
        List<Object[]> dataTagsResults = em.createNativeQuery("SELECT t0.DATAFILE_ID, t0.TYPE FROM DataFileTag t0, dvObject t1 WHERE (t1.ID = t0.DATAFILE_ID) AND (t1.OWNER_ID="+ owner.getId() + ")").getResultList();
        for (Object[] result : dataTagsResults) {
            Long datafile_id = (Long) result[0];
            Integer tagtype_id = (Integer) result[1];
            if (fileTagMap.get(datafile_id) == null) {
                fileTagMap.put(datafile_id, new HashSet<Integer>());
            }
            fileTagMap.get(datafile_id).add(tagtype_id);
            i++; 
        }
        dataTagsResults = null;
        
        logger.fine("Retrieved "+i+" data tags.");
        
        i = 0; 
        
        List<Object[]> fileResults = em.createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, t1.CONTENTTYPE, t1.FILESYSTEMNAME, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, t1.RESTRICTED, t1.CHECKSUMTYPE FROM DVOBJECT t0, DATAFILE t1 WHERE ((t0.OWNER_ID = " + owner.getId() + ") AND ((t1.ID = t0.ID) AND (t0.DTYPE = 'DataFile'))) ORDER BY t0.ID").getResultList(); 
    
        for (Object[] result : fileResults) {
            Integer file_id = (Integer) result[0];
            
            DataFile dataFile = new DataFile();
            
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
        
        logger.fine("Retreived "+i+" file categories attached to the dataset.");
        
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
                categoryMetaMap.put(filemeta_id, new HashSet<Long>());
            }
            categoryMetaMap.get(filemeta_id).add(category_id);
            i++;
        }
        logger.fine("Retrieved and mapped "+i+" file categories attached to files in the version "+version.getId());
        categoryResults = null;
        
        List<Object[]> metadataResults = em.createNativeQuery("select id, datafile_id, DESCRIPTION, LABEL, RESTRICTED, DIRECTORYLABEL from FileMetadata where datasetversion_id = "+version.getId() + " ORDER BY LABEL").getResultList();
        
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
            fileMetadata.setCategories(new LinkedList<DataFileCategory>());

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
            
            retList.add(fileMetadata);
        }
        
        metadataResults = null;
        
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

    /* 
     Convenience methods for merging and removingindividual file metadatas, 
     without touching the rest of the DataFile object:
    */
    
    public FileMetadata mergeFileMetadata(FileMetadata fileMetadata) {
        return em.merge(fileMetadata);
    }
    
    public void removeFileMetadata(FileMetadata fileMetadata) {
        FileMetadata mergedFM = em.merge(fileMetadata);
        em.remove(mergedFM);
    }
    
    public List<DataFile> findHarvestedFilesByClient(HarvestingClient harvestingClient) {
        TypedQuery query = em.createQuery("SELECT d FROM DataFile d, DvObject o, Dataset s WHERE o.id = d.id AND o.owner.id = s.id AND s.harvestedFrom.id = :harvestingClientId", DataFile.class);
        query.setParameter("harvestingClientId", harvestingClient.getId());
        return query.getResultList();
    }
    
    /**/
    
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
     * This method tells you if thumbnail generation is *supported* 
     * on this type of file. i.e., if true, it does not guarantee that a thumbnail 
     * can/will be generated; but it means that we can try. 
     */
    public boolean thumbnailSupported (DataFile file) {
        if (file == null) {
            return false;
        }
        
        if (file.isHarvested() || "".equals(file.getStorageIdentifier())) {
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
    public boolean isThumbnailAvailable (DataFile file) {
        if (file == null) {
            return false; 
        } 
        
        // If this file already has the "thumbnail generated" flag set,
        // we'll just trust that:
        
        if (file.isPreviewImageAvailable()) {
            return true;
        }
        
        // If thumbnails are not even supported for this class of files, 
        // there's notthing to talk about: 
        
        if (!thumbnailSupported(file)) {
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
        TODO: adding a boolean flag isImageAlreadyGenerated to the DataFile 
         db table should help with this. -- L.A. 4.2.1 DONE: 4.2.2
        
        // Also, thumbnails are only shown to users who have permission to see 
        // the full-size image file. So before we do anything else, let's
        // do some authentication and authorization:        
        if (!permissionService.userOn(user, file).has(Permission.DownloadFile)) { 
            logger.fine("No permission to download the file.");
            return false; 
        }*/
        
        
        
       if (ImageThumbConverter.isThumbnailAvailable(file)) {
           file = this.find(file.getId());
           file.setPreviewImageAvailable(true);
           file = em.merge(file);
           // (should this be done here? - TODO:)
           return true;
       }
       return false;
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
    
    public void populateFileSearchCard(SolrSearchResult solrSearchResult) {
        solrSearchResult.setEntity(this.findCheapAndEasy(solrSearchResult.getEntityId()));
    }
        
}