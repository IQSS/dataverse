/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.storageuse.StorageQuota;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.validation.JSONDataValidation;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

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
    SolrIndexServiceBean solrIndexService; 

    @EJB
    AuthenticationServiceBean authService;
    
    @EJB
    DatasetServiceBean datasetService;
    
    @EJB
    DataverseLinkingServiceBean dataverseLinkingService;

    @EJB
    DatasetLinkingServiceBean datasetLinkingService;
    
    @EJB
    GroupServiceBean groupService;
    
    @EJB
    DataverseRoleServiceBean rolesService;
    
    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;
    
    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession session;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private static final String BASE_QUERY_DATASET_TITLES_WITHIN_DV = "select v.value, o.id\n" 
                + "from datasetfieldvalue v, dvobject o "
                + "where " 
                + "v.datasetfield_id = (select id from datasetfield f where datasetfieldtype_id = 1 " 
                + "and datasetversion_id = (select max(id) from datasetversion where dataset_id = o.id))";

    public Dataverse save(Dataverse dataverse) {
       
        dataverse.setModificationTime(new Timestamp(new Date().getTime()));
        Dataverse savedDataverse = em.merge(dataverse);
        return savedDataverse;
    }
    
    public boolean index(Dataverse dataverse) {
        return index(dataverse, false);

    }
        
    public boolean index(Dataverse dataverse, boolean indexPermissions) {    
        /**
         * @todo check the result to see if indexing was successful or not
         * added logging of exceptions 
         */
        try {
            indexService.indexDataverse(dataverse);
            if (indexPermissions) {
                solrIndexService.indexPermissionsOnSelfAndChildren(dataverse);
            }
        } catch (IOException | SolrServerException e) {
            String failureLogText = "Post-save indexing failed. You can kickoff a re-index of this dataverse with: \r\n curl http://localhost:8080/api/admin/index/dataverses/" + dataverse.getId().toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataverse);
            return false;
        }

        return true;
    }    

    public Dataverse find(Object pk) {
        return em.find(Dataverse.class, pk);
    }

    public List<Dataverse> findAll() {
        return em.createNamedQuery("Dataverse.findAll").getResultList();
    }
    
    public List<Long> findIdStale() {
        return em.createNamedQuery("Dataverse.findIdStale").getResultList();
    }
    public List<Long> findIdStalePermission() {
        return em.createNamedQuery("Dataverse.findIdStalePermission").getResultList();
    }

    /**
     * @param numPartitions The number of partitions you intend to split the
     * indexing job into. Perhaps you have three Glassfish servers and you'd
     * like each one to operate on a subset of dataverses.
     *
     * @param partitionId Maybe "partitionId" is the wrong term but it's what we
     * call in the (text) UI. If you've specified three partitions the three
     * partitionIds are 0, 1, and 2. We do `dataverseId % numPartitions =
     * partitionId` to figure out which partition the dataverseId falls into.
     * 
     * @param skipIndexed If true, will skip any dvObjects that have a indexTime set 
     *
     * @return All dataverses if you say numPartitions=1 and partitionId=0.
     * Otherwise, a subset of dataverses.
     */
    public List<Dataverse> findAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        if (numPartitions < 1) {
            long saneNumPartitions = 1;
            numPartitions = saneNumPartitions;
        }
        String skipClause = skipIndexed ? "AND o.indexTime is null " : "";
        TypedQuery<Dataverse> typedQuery = em.createQuery("SELECT OBJECT(o) FROM Dataverse AS o WHERE MOD( o.id, :numPartitions) = :partitionId " +
                skipClause +
                "ORDER BY o.id", Dataverse.class);
        typedQuery.setParameter("numPartitions", numPartitions);
        typedQuery.setParameter("partitionId", partitionId);
        return typedQuery.getResultList();
    }
    
    public List<Long> findDataverseIdsForIndexing(boolean skipIndexed) {
        if (skipIndexed) {
            return em.createQuery("SELECT o.id FROM Dataverse o WHERE o.indexTime IS null ORDER BY o.id", Long.class).getResultList();
        }
        return em.createQuery("SELECT o.id FROM Dataverse o ORDER BY o.id", Long.class).getResultList();
        
    }

    public List<Dataverse> findByCreatorId(Long creatorId) {
        return em.createNamedQuery("Dataverse.findByCreatorId").setParameter("creatorId", creatorId).getResultList();
    }

    public List<Dataverse> findByReleaseUserId(Long releaseUserId) {
        return em.createNamedQuery("Dataverse.findByReleaseUserId").setParameter("releaseUserId", releaseUserId).getResultList();
    }

    public List<Dataverse> findByOwnerId(Long ownerId) {
        return em.createNamedQuery("Dataverse.findByOwnerId").setParameter("ownerId", ownerId).getResultList();
    }
    
    public List<Long> findIdsByOwnerId(Long ownerId) {
        String qr = "select o.id from Dataverse as o where o.owner.id =:ownerId order by o.id";
        return em.createQuery(qr, Long.class).setParameter("ownerId", ownerId).getResultList();
    }
    
    public List<Dataverse> findPublishedByOwnerId(Long ownerId) {
        String qr ="select object(o) from Dataverse as o where o.owner.id =:ownerId and o.publicationDate is not null order by o.name";
        return em.createQuery(qr, Dataverse.class).setParameter("ownerId", ownerId).getResultList();
    }

    /**
     * @return the root dataverse
     * @todo Do we really want this method to sometimes throw a
     * NoResultException which is a RuntimeException?
     */
    public Dataverse findRootDataverse() {
        return em.createNamedQuery("Dataverse.findRoot", Dataverse.class).getSingleResult();
    }
    
    
    //Similarly - if the above throws that exception, do we need to catch it here?
    //ToDo - consider caching?
    public String getRootDataverseName() {
        Dataverse root = findRootDataverse();
        String rootDataverseName=root.getName();
        return StringUtil.isEmpty(rootDataverseName) ? "" : rootDataverseName; 
    }
    
    public List<Dataverse> findAllPublishedByOwnerId(Long ownerId) {
        List<Dataverse> retVal = new ArrayList<>();       
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

    /**
     * A lookup of a dataverse alias should be case insensitive. If "cfa"
     * belongs to the Center for Astrophysics, we don't want to allow Code for
     * America to start using "CFA". Force all queries to be lower case.
     * @param anAlias
     * @return 
     */
    public Dataverse findByAlias(String anAlias) {
        try {
            return (anAlias.toLowerCase().equals(":root"))
              ? findRootDataverse()
              : em.createNamedQuery("Dataverse.findByAlias", Dataverse.class)
                  .setParameter("alias", anAlias.toLowerCase())
                  .getSingleResult();
        } catch ( NoResultException|NonUniqueResultException ex ) {
            logger.warning("Unable to find a single dataverse using alias \"" + anAlias + "\": " + ex);
            return null;
        }
    }

    public boolean hasData(Dataverse dataverse) {
        return (getChildCount(dataverse) > 0);
    }

    public Long getChildCount(Dataverse dataverse) {
        TypedQuery<Long> amountQry = em.createNamedQuery("Dataverse.ownedObjectsById", Long.class)
                .setParameter("id", dataverse.getId());

        return amountQry.getSingleResult();
    }

    public boolean isRootDataverseExists() {
        long count = em.createQuery("SELECT count(dv) FROM Dataverse dv WHERE dv.owner.id=null", Long.class).getSingleResult();
        return (count == 1);
    }

    public String determineDataversePath(Dataverse dataverse) {
        List<String> dataversePathSegments = new ArrayList<>();
        indexService.findPathSegments(dataverse, dataversePathSegments);
        StringBuilder dataversePath = new StringBuilder();
        for (String segment : dataversePathSegments) {
            dataversePath.append("/").append(segment);
        }
        return dataversePath.toString();
    }

    public MetadataBlock findMDB(Long id) {
        return em.find(MetadataBlock.class, id);
    }

    public MetadataBlock findMDBByName(String name) {
        return em.createQuery("select m from MetadataBlock m WHERE m.name=:name", MetadataBlock.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    public List<MetadataBlock> findAllMetadataBlocks() {
        return em.createQuery("select object(o) from MetadataBlock as o order by o.id", MetadataBlock.class).getResultList();
    }
    
    public List<MetadataBlock> findSystemMetadataBlocks(){
        String qr = "select object(o) from MetadataBlock as o where o.owner.id=null  order by o.id";
        return em.createQuery(qr, MetadataBlock.class).getResultList();
    }
    
    public List<MetadataBlock> findMetadataBlocksByDataverseId(Long dataverse_id) {
        String qr = "select object(o) from MetadataBlock as o where o.owner.id=:dataverse_id order by o.id";
        return em.createQuery(qr, MetadataBlock.class)
                .setParameter("dataverse_id", dataverse_id).getResultList();
    }
    
    public DataverseFacet findFacet(Long id) {
        return em.find(DataverseFacet.class, id);
    }
    
    public List<DataverseFacet> findAllDataverseFacets() {
        return em.createQuery("select object(o) from DataverseFacet as o order by o.display", DataverseFacet.class).getResultList();
    }
    
    public String getDataverseLogoThumbnailAsBase64(Dataverse dataverse, User user) {
        
        if (dataverse == null) {
            return null;
        }

        File dataverseLogoFile = getLogo(dataverse);
        if (dataverseLogoFile != null) {
            String logoThumbNailPath;

            if (dataverseLogoFile.exists()) {
                logoThumbNailPath = ImageThumbConverter.generateImageThumbnailFromFile(dataverseLogoFile.getAbsolutePath(), 48);
                if (logoThumbNailPath != null) {
                    return ImageThumbConverter.getImageAsBase64FromFile(new File(logoThumbNailPath));

                }
            }
        } 
        return null;
    }
    
    public String getDataverseLogoThumbnailAsBase64ById(Long dvId) {
     
        File dataverseLogoFile = getLogoById(dvId);
        
        if (dataverseLogoFile != null) {
            String logoThumbNailPath;

            if (dataverseLogoFile.exists()) {
                logoThumbNailPath = ImageThumbConverter.generateImageThumbnailFromFile(dataverseLogoFile.getAbsolutePath(), 48);
                if (logoThumbNailPath != null) {
                    return ImageThumbConverter.getImageAsBase64FromFile(new File(logoThumbNailPath));

                }
            }
        } 
        return null;
    }

    public String getDataverseLogoThumbnailAsUrl(Long dvId) {
        File dataverseLogoFile = getLogoById(dvId);
        if (dataverseLogoFile != null && dataverseLogoFile.exists()) {
            return SystemConfig.getDataverseSiteUrlStatic() + "/api/access/dvCardImage/" + dvId;
        }
        return null;
    }

    private File getLogo(Dataverse dataverse) {
        if (dataverse.getId() == null) {
            return null; 
        }
        
        DataverseTheme theme = dataverse.getDataverseTheme(); 
        if (theme != null && theme.getLogo() != null && !theme.getLogo().isEmpty()) {
            return ThemeWidgetFragment.getLogoDir(dataverse.getLogoOwnerId()).resolve(theme.getLogo()).toFile();
        }
            
        return null;         
    }
    
    private File getLogoById(Long id) {
        if (id == null) {
            return null; 
        }
        
        String logoFileName;
        
        try {
                logoFileName = (String) em.createNativeQuery("SELECT logo FROM dataversetheme WHERE dataverse_id = " + id).getSingleResult();
            
        } catch (Exception ex) {
            return null;
        }
        
        if (logoFileName != null && !logoFileName.isEmpty()) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
  
            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File (domainRoot + File.separator + 
                    "docroot" + File.separator + 
                    "logos" + File.separator + 
                    id + File.separator + 
                    logoFileName);
            }
        }
            
        return null;         
    }
    
    public DataverseTheme findDataverseThemeByIdQuick(Long id) {
        if (id == null) {
            return null; 
        }
        
        Object[] result;
        
        try {
            result = (Object[]) em.createNativeQuery("SELECT logo, logoFormat, logothumbnail FROM dataversetheme WHERE dataverse_id = " + id).getSingleResult();
            
        } catch (Exception ex) {
            return null;
        }
        
        if (result == null) {
            return null;
        }
        
        DataverseTheme theme = new DataverseTheme();
        
        if (result[0] != null) {
            theme.setLogo((String) result[0]);
        }

        if (result[1] != null) {
            String format = (String) result[1];
            switch (format) {
                case "RECTANGLE":
                theme.setLogoFormat(DataverseTheme.ImageFormat.RECTANGLE);
                    break;
                case "SQUARE":
                theme.setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
                    break;
            }
        }

        if (result[2] != null) {
            theme.setLogoThumbnail((String) result[2]);
        }
        
        return theme;
    }

    public List<Dataverse> findDataversesThisIdHasLinkedTo(long dataverseId) {
        return dataverseLinkingService.findLinkedDataverses(dataverseId);
    }

    public List<Dataverse> findDataversesThatLinkToThisDvId(long dataverseId) {
        return dataverseLinkingService.findLinkingDataverses(dataverseId);
    }

    public List<Dataset> findDatasetsThisIdHasLinkedTo(long dataverseId, boolean onlyPublished) {
        List<Dataset> linkedDatasets = datasetLinkingService.findLinkedDatasets(dataverseId);

        if (!onlyPublished) {
            return linkedDatasets;
        }

        List<Dataset> retList = new ArrayList();

        for (Dataset ds : linkedDatasets) {
            if (ds.isReleased() && !ds.isDeaccessioned()) {
                retList.add(ds);
            }
        }

        return retList;
    }

    public List<Dataset> findDatasetsThisIdHasLinkedTo(long dataverseId) {
        return this.findDatasetsThisIdHasLinkedTo(dataverseId, false);
    }

    public List<Dataverse> findDataversesThatLinkToThisDatasetId(long datasetId) {
        return datasetLinkingService.findLinkingDataverses(datasetId);
    }
    
    public List<Dataverse> filterByAliasQuery(String filterQuery) {
        //Query query = em.createNativeQuery("select o from Dataverse o where o.alias LIKE '" + filterQuery + "%' order by o.alias");
        //Query query = em.createNamedQuery("Dataverse.filterByAlias", Dataverse.class).setParameter("alias", filterQuery.toLowerCase() + "%");
        List<Dataverse> ret = em.createNamedQuery("Dataverse.filterByAliasNameAffiliation", Dataverse.class)
                .setParameter("alias", filterQuery.toLowerCase() + "%")
                .setParameter("name", "%" + filterQuery.toLowerCase() + "%")
                .setParameter("affiliation", "%" + filterQuery.toLowerCase() + "%").getResultList();
        //logger.info("created native query: select o from Dataverse o where o.alias LIKE '" + filterQuery + "%' order by o.alias");
        logger.info("created named query");
        if (ret != null) {
            logger.info("results list: "+ret.size()+" results.");
        }
        return ret;
    }
    
    public List<Dataverse> filterDataversesForLinking(String query, DataverseRequest req, Dataset dataset) {

        List<Dataverse> dataverseList = new ArrayList<>();

        List<Dataverse> results = filterDataversesByNamePattern(query);
        
        if (results == null || results.size() == 0) {
            return null; 
        }

        List<Object> alreadyLinkeddv_ids = em.createNativeQuery("SELECT linkingdataverse_id   FROM datasetlinkingdataverse WHERE dataset_id = " + dataset.getId()).getResultList();
        List<Dataverse> remove = new ArrayList<>();

        if (alreadyLinkeddv_ids != null && !alreadyLinkeddv_ids.isEmpty()) {
            alreadyLinkeddv_ids.stream().map((testDVId) -> this.find(testDVId)).forEachOrdered((removeIt) -> {
                remove.add(removeIt);
            });
        }
        
        for (Dataverse res : results) {
            if (!remove.contains(res)) {
                if (this.permissionService.requestOn(req, res).has(Permission.PublishDataset)) {
                    dataverseList.add(res);
                }
            }
        }

        return dataverseList;
    }
    public List<Dataverse> filterDataversesForUnLinking(String query, DataverseRequest req, Dataset dataset) {
        List<Object> alreadyLinkeddv_ids = em.createNativeQuery("SELECT linkingdataverse_id FROM datasetlinkingdataverse WHERE dataset_id = " + dataset.getId()).getResultList();
        List<Dataverse> dataverseList = new ArrayList<>();
        if (alreadyLinkeddv_ids != null && !alreadyLinkeddv_ids.isEmpty()) {
            alreadyLinkeddv_ids.stream().map((testDVId) -> this.find(testDVId)).forEachOrdered((dataverse) -> {
                if (this.permissionService.requestOn(req, dataverse).has(Permission.PublishDataset)) {
                    dataverseList.add(dataverse);
                }
            });
        }
        return dataverseList;
    }

    public List<Dataverse> filterDataversesForHosting(String pattern, DataverseRequest req) {

        // Find the dataverses matching the search parameters: 
        
        List<Dataverse> searchResults = filterDataversesByNamePattern(pattern);
        
        if (searchResults == null || searchResults.size() == 0) {
            return null; 
        }
        
        logger.fine("search query found " + searchResults.size() + " results");
        
        // Filter the results and drop the dataverses where the user is not allowed to 
        // add datasets:
        
        if (req.getAuthenticatedUser().isSuperuser()) {
            logger.fine("will skip permission check...");
            return searchResults;
        }
        
        List<Dataverse> finalResults = new ArrayList<>();
        
        for (Dataverse res : searchResults) {
            if (this.permissionService.requestOn(req, res).has(Permission.AddDataset)) {
                finalResults.add(res);
            }
        }
        
        logger.fine("returning " + finalResults.size() + " final results");

        return finalResults;
    }
    
    
    /* 
        This method takes a search parameter and expands it into a list of 
        Dataverses with matching names. 
        The search is performed on the name with the trailing word "dataverse"
        stripped (if present). This way the search on "data" (or on "da" pr 
        "dat") does NOT return almost every dataverse in the database - since
        most of them have names that end in "... Dataverse". 
        The query isn't pretty, but it works, and it's still EJB QL (and NOT a 
        native query). 
    */
    public List<Dataverse> filterDataversesByNamePattern(String pattern) {

        pattern = pattern.toLowerCase();
        
        String pattern1 = pattern + "%";
        String pattern2 = "% " + pattern + "%";

        // Adjust the queries for very short, 1 and 2-character patterns:
        if (pattern.length() == 1) {
            pattern1 = pattern;
            pattern2 = pattern + " %";
        } 
        /*if (pattern.length() == 2) {
            pattern2 = pattern + "%";
        }*/
        
        
        String qstr = "select dv from Dataverse dv "
                + "where (LOWER(dv.name) LIKE :dataverse and ((SUBSTRING(LOWER(dv.name),0,(LENGTH(dv.name)-9)) LIKE :pattern1) "
                + "     or (SUBSTRING(LOWER(dv.name),0,(LENGTH(dv.name)-9)) LIKE :pattern2))) "
                + "or (LOWER(dv.name) NOT LIKE :dataverse and ((LOWER(dv.name) LIKE :pattern1) "
                + "     or (LOWER(dv.name) LIKE :pattern2))) "
                + "order by dv.alias";
                
        List<Dataverse> searchResults = null;
        
        try {
            searchResults = em.createQuery(qstr, Dataverse.class)
                    .setParameter("dataverse", "%dataverse")
                    .setParameter("pattern1", pattern1)
                    .setParameter("pattern2", pattern2)
                    .getResultList();
        } catch (Exception ex) {
            searchResults = null;
        }
        
        return searchResults;
    }
    
    /**
     * Used to identify and properly display Harvested objects on the dataverse page.
     * 
     *//*
    @Deprecated
    public Map<Long, String> getAllHarvestedDataverseDescriptions(){
        
        String qstr = "SELECT dataverse_id, archiveDescription FROM harvestingClient;";
        List<Object[]> searchResults = null;
        
        try {
            searchResults = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            searchResults = null;
        }
        
        if (searchResults == null) {
            return null;
        }
        
        Map<Long, String> ret = new HashMap<>();
        
        for (Object[] result : searchResults) {
            Long dvId = null;
            if (result[0] != null) {
                try {
                    dvId = (Long)result[0];
                } catch (Exception ex) {
                    dvId = null;
                }
                if (dvId == null) {
                    continue;
                }
                
                ret.put(dvId, (String)result[1]);
            }
        }
        
        return ret;        
    }*/
    
    public String getParentAliasString(SolrSearchResult solrSearchResult){
        Long dvId = solrSearchResult.getEntityId();
        String retVal = "";
        
        if (dvId == null) {
            return retVal;
        }
        
        String searchResult;
        try {
            searchResult = (String) em.createNativeQuery("select  t0.ALIAS FROM DATAVERSE t0, DVOBJECT t1,  DVOBJECT t2 WHERE (t0.ID = t1.ID) AND (t2.OWNER_ID = t1.ID)  AND (t2.ID =" + dvId + ")").getSingleResult();

        } catch (Exception ex) {
            return retVal;
        }

        if (searchResult == null) {
            return retVal;
        }

        if (searchResult != null) {
            return searchResult;
        }
        
        return retVal;
    }
    
    
    public void populateDvSearchCard(SolrSearchResult solrSearchResult) {
  
        Long dvId = solrSearchResult.getEntityId();
        
        if (dvId == null) {
            return;
        }
        
        Long parentDvId = null;
        String parentId = solrSearchResult.getParent().get("id");
        if (parentId != null) {
            try {
                parentDvId = Long.parseLong(parentId);
            } catch (NumberFormatException ex) {
                parentDvId = null;
            }
        }
        
        Object[] searchResult;
        
        try {
            if (parentDvId == null) {
                searchResult = (Object[]) em.createNativeQuery("SELECT t0.AFFILIATION, t0.ALIAS FROM DATAVERSE t0 WHERE t0.ID = " + dvId).getSingleResult();
            } else {
                searchResult = (Object[]) em.createNativeQuery("SELECT t0.AFFILIATION, t0.ALIAS, t2.ALIAS FROM DATAVERSE t0, DVOBJECT t1, DATAVERSE t2, DVOBJECT t3 WHERE (t0.ID = t1.ID) AND (t1.OWNER_ID = t3.ID) AND (t2.ID = t3.ID) AND (t0.ID = " + dvId + ")").getSingleResult();
            }
        } catch (Exception ex) {
            return;
        }

        if (searchResult == null) {
            return;
        }
        
        if (searchResult[0] != null) {
            solrSearchResult.setDataverseAffiliation((String) searchResult[0]);
        }

        if (searchResult[1] != null) {
            solrSearchResult.setDataverseAlias((String) searchResult[1]);
        }
        
        if (parentDvId != null) {
            if (searchResult[2] != null) {
                solrSearchResult.setDataverseParentAlias((String) searchResult[2]);
            }
        }
    }
    
    // function to recursively find ids of all children of a dataverse that 
    // are also of type dataverse
    public List<Long> findAllDataverseDataverseChildren(Long dvId) {
        // get list of Dataverse children
        List<Long> dataverseChildren = findIdsByOwnerId(dvId);
        
        if (dataverseChildren == null) {
            return dataverseChildren;
        } else {
            List<Long> newChildren = new ArrayList<>();
            for (Long childDvId : dataverseChildren) {
                newChildren.addAll(findAllDataverseDataverseChildren(childDvId));
            }
            dataverseChildren.addAll(newChildren);
            return dataverseChildren;
        }
    }
    
    // function to recursively find ids of all children of a dataverse that are 
    // of type dataset
    public List<Long> findAllDataverseDatasetChildren(Long dvId, boolean onlyPublished, boolean includeHarvested) {
        // get list of Dataverse children
        List<Long> dataverseChildren = findIdsByOwnerId(dvId);
        // get list of Dataset children
        List<Long> datasetChildren = datasetService.findIdsByOwnerId(dvId, onlyPublished, includeHarvested);
        
        if (dataverseChildren == null) {
            return datasetChildren;
        } else {
            for (Long childDvId : dataverseChildren) {
                datasetChildren.addAll(findAllDataverseDatasetChildren(childDvId, onlyPublished, includeHarvested));
            }
            return datasetChildren;
        }
    }

    public List<Long> findAllDataverseDatasetChildren(Long dvId) {
        return findAllDataverseDatasetChildren(dvId, false, false);
    }
    
    public String addRoleAssignmentsToChildren(Dataverse owner, ArrayList<String> rolesToInherit,
            boolean inheritAllRoles) {
        /*
         * This query recursively finds all Dataverses that are inside/children of the
         * specified one. It recursively finds dvobjects of dtype 'Dataverse' whose
         * owner_id equals an id already in the list and then returns the list of ids
         * found, excluding the id of the original specified Dataverse.
         */
        String qstr = "WITH RECURSIVE path_elements AS ((" + " SELECT id, dtype FROM dvobject WHERE id in ("
                + owner.getId() + "))" + " UNION\n"
                + " SELECT o.id, o.dtype FROM path_elements p, dvobject o WHERE o.owner_id = p.id and o.dtype='Dataverse') "
                + "SELECT id FROM path_elements WHERE id !=" + owner.getId() + ";";

        List<Integer> childIds;
        try {
            childIds = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            childIds = null;
        }

        // Set up to track the set of users/groups that get assigned a role and those
        // that don't
        JsonArrayBuilder usedNames = Json.createArrayBuilder();
        JsonArrayBuilder unusedNames = Json.createArrayBuilder();
        // Set up to track the list of dataverses, by id and alias, that are traversed.
        JsonArrayBuilder dataverseIds = Json.createArrayBuilder();
        JsonArrayBuilder dataverseAliases = Json.createArrayBuilder();
        // Get the Dataverses for the returned ids

        List<Dataverse> children = new ArrayList<Dataverse>();

        for (int i = 0; i < childIds.size(); i++) {
            Integer childId = childIds.get(i);
            Dataverse child = find(new Long(childId.longValue()));
            if (child != null) {
                // Add to the list of Dataverses
                children.add(child);
                // Add ids and aliases to the tracking arrays
                dataverseIds.add(childId.longValue());
                dataverseAliases.add(child.getAlias());
            }
        }
        // Find the role assignments on the specified Dataverse
        List<RoleAssignment> allRAsOnOwner = rolesService.directRoleAssignments(owner);

        // Create a list of just the inheritable role assignments on the original
        // dataverse
        List<RoleAssignment> inheritableRAsOnOwner = new ArrayList<RoleAssignment>();
        for (RoleAssignment role : allRAsOnOwner) {
            if (inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias())) {
                //Only supporting built-in/non-dataverse-specific custom roles. Custom roles all have an owner.
                if(role.getRole().getOwner()==null) {
                    inheritableRAsOnOwner.add(role);
                }
            }
        }

        String privateUrlToken = null;
        // Create lists of the existing inheritable roles for each child Dataverse
        Map<Long, List<RoleAssignment>> existingRAs = new HashMap<Long, List<RoleAssignment>>();
        for (Dataverse childDv : children) {
            List<RoleAssignment> allRAsOnChild = rolesService.directRoleAssignments(childDv);
            List<RoleAssignment> inheritableRoles = new ArrayList<RoleAssignment>();
            for (RoleAssignment role : allRAsOnChild) {
                if (inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias())) {
                    inheritableRoles.add(role);
                }
            }
            existingRAs.put(childDv.getId(), inheritableRoles);
        }

        for (RoleAssignment roleAssignment : inheritableRAsOnOwner) {
            DataverseRole inheritableRole = roleAssignment.getRole();
            String identifier = roleAssignment.getAssigneeIdentifier();
            if (identifier.startsWith(AuthenticatedUser.IDENTIFIER_PREFIX)) {
                // The RoleAssignment is for an individual user
                // Add their name to the tracking list
                usedNames.add(identifier);
                // Strip the Identifier prefix so we can retrieve the user
                identifier = identifier.substring(AuthenticatedUser.IDENTIFIER_PREFIX.length());
                AuthenticatedUser roleUser = authService.getAuthenticatedUser(identifier);
                // Now loop over all children and add the roleUser in this role if they don't
                // yet have this role
                for (Dataverse childDv : children) {
                    try {
                        RoleAssignment ra = new RoleAssignment(inheritableRole, roleUser, childDv, privateUrlToken);
                        if (!existingRAs.get(childDv.getId()).contains(ra)) {
                            rolesService.save(ra);
                        }
                    } catch (Exception e) {
                        logger.warning("Unable to assign " + roleAssignment.getAssigneeIdentifier()
                                + "as an admin for new Dataverse: " + childDv.getName());
                        logger.warning(e.getMessage());
                        throw (e);
                    }
                }
            } else if (identifier.startsWith(Group.IDENTIFIER_PREFIX)) {
                // The role assignment is for a group
                usedNames.add(identifier);
                identifier = identifier.substring(Group.IDENTIFIER_PREFIX.length());
                Group roleGroup = groupService.getGroup(identifier);
                if (roleGroup != null) {
                    for (Dataverse childDv : children) {
                        try {
                            RoleAssignment ra = new RoleAssignment(inheritableRole, roleGroup, childDv,
                                    privateUrlToken);
                            if (!existingRAs.get(childDv.getId()).contains(ra)) {
                                rolesService.save(ra);
                            }
                        } catch (Exception e) {
                            logger.warning("Unable to assign " + roleAssignment.getAssigneeIdentifier()
                                    + "as an admin for new Dataverse: " + childDv.getName());
                            logger.warning(e.getMessage());
                            throw (e);
                        }
                    }
                } else {
                    // Add any groups of types not yet supported
                    unusedNames.add(identifier);
                }
            } else {
                // Add any other types of entity found (not user or group) that aren't supported
                unusedNames.add(identifier);
            }
        }
        /*
         * Report the list of Dataverses affected and the set of users/groups that
         * should now have admin roles on them (they may already have had them) and any
         * entities that had an admin role on the specified dataverse which were not
         * handled. Add this to the log and the API return message.
         */
        String result = Json.createObjectBuilder().add("Dataverses Updated", dataverseIds)
                .add("Updated Dataverse Aliases", dataverseAliases).add("Assignments added for", usedNames)
                .add("Assignments not added for", unusedNames).build().toString();
        logger.info(result);
        return (result);
    }
    
    // A quick custom query that finds all the (direct children) dataset titles 
    // with a dataverse and returns a list of (dataset_id, title) pairs. 
    public List<Object[]> getDatasetTitlesWithinDataverse(Long dataverseId) {
        String cqString = BASE_QUERY_DATASET_TITLES_WITHIN_DV
                + "and o.owner_id = " + dataverseId;

        return em.createNativeQuery(cqString).getResultList();
    }

    public  String getCollectionDatasetSchema(String dataverseAlias) {
        return getCollectionDatasetSchema(dataverseAlias, null);
    }
    public  String getCollectionDatasetSchema(String dataverseAlias, Map<String, Map<String,List<String>>> schemaChildMap) {
        
        Dataverse testDV = this.findByAlias(dataverseAlias);
        
        while (!testDV.isMetadataBlockRoot()) {
            if (testDV.getOwner() == null) {
                break; // we are at the root; which by definition is metadata block root, regardless of the value
            }
            testDV = testDV.getOwner();
        }
        
        /* Couldn't get the 'return base if no extra required fields to work with the path provided
        leaving it as 'out of scope' for now SEK 11/27/2023

        List<DataverseFieldTypeInputLevel> required = new ArrayList<>();

        required = dataverseFieldTypeInputLevelService.findRequiredByDataverseId(testDV.getId());
        
        if (required == null || required.isEmpty()){
            String pathToJsonFile = "src/main/resources/edu/harvas/iq/dataverse/baseDatasetSchema.json";
            String baseSchema = getBaseSchemaStringFromFile(pathToJsonFile);
            if (baseSchema != null && !baseSchema.isEmpty()){
                return baseSchema;
            }
        }
        
        */
        List<MetadataBlock> selectedBlocks = new ArrayList<>();
        List<DatasetFieldType> requiredDSFT = new ArrayList<>();
        
        selectedBlocks.addAll(testDV.getMetadataBlocks());

        // Process all fields in all metadata blocks
        for (MetadataBlock mdb : selectedBlocks) {
            for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                if (!dsft.isChild()) {
                    // Get or set the input level settings for the parent field
                    DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(testDV.getId(), dsft.getId());
                    if (dsfIl != null) {
                        dsft.setRequiredDV(dsfIl.isRequired());
                        dsft.setInclude(dsfIl.isInclude());
                        dsft.setLocalDisplayOnCreate(dsfIl.getDisplayOnCreate());
                    } else {
                        dsft.setRequiredDV(dsft.isRequired());
                        dsft.setInclude(true);
                        // Default displayOnCreate to true for required fields
                        dsft.setLocalDisplayOnCreate(dsft.isRequired());
                    }
                    List<String> childrenRequired = new ArrayList<>();
                    List<String> childrenAllowed = new ArrayList<>();
                    if (dsft.isHasChildren()) {
                        for (DatasetFieldType child : dsft.getChildDatasetFieldTypes()) {
                            DataverseFieldTypeInputLevel dsfIlChild = dataverseFieldTypeInputLevelService.findByDataverseIdDatasetFieldTypeId(testDV.getId(), child.getId());
                            if (dsfIlChild != null) {
                                child.setRequiredDV(dsfIlChild.isRequired());
                                child.setInclude(dsfIlChild.isInclude());
                                child.setLocalDisplayOnCreate(dsfIlChild.getDisplayOnCreate());
                            } else {
                                child.setRequiredDV(child.isRequired() && dsft.isRequired());
                                child.setInclude(true);
                                // Default displayOnCreate to true for required child fields
                                child.setLocalDisplayOnCreate(child.isRequired());
                            }
                            if (child.isRequired()) {
                                childrenRequired.add(child.getName());
                            }
                            childrenAllowed.add(child.getName());
                        }
                    }
                    
                    if (schemaChildMap != null) {
                        Map<String, List<String>> map = new HashMap<>();
                        map.put("required", childrenRequired);
                        map.put("allowed", childrenAllowed);
                        schemaChildMap.put(dsft.getName(), map);
                    }
                    
                    if(dsft.isRequiredDV()){
                        requiredDSFT.add(dsft);
                    }
                }
            }            
        }
        
        String reqMDBNames = "";
        List<MetadataBlock> hasReqFields = new ArrayList<>();
        String retval = datasetSchemaPreface;
        
        // Build list of metadata blocks with required fields
        for (MetadataBlock mdb : selectedBlocks) {
            for (DatasetFieldType dsft : requiredDSFT) {
                if (dsft.getMetadataBlock().equals(mdb)) {
                    hasReqFields.add(mdb);
                    if (!reqMDBNames.isEmpty()) reqMDBNames += ",";
                    reqMDBNames += "\"" + mdb.getName() + "\"";
                    break;
                }
            }
        }
        
        // Generate schema for each metadata block
        int countMDB = 0;
        for (MetadataBlock mdb : hasReqFields) {
            if (countMDB > 0) {
                retval += ",";
            }
            retval += getCustomMDBSchema(mdb, requiredDSFT);
            countMDB++;            
        }
        
        retval += "\n                     }";
        retval += endOfjson.replace("blockNames", reqMDBNames);

        return retval;
    }
    
    private String getCustomMDBSchema (MetadataBlock mdb, List<DatasetFieldType> requiredDSFT){
        String retval = "";
        boolean mdbHasReqField = false;
        int numReq = 0;
        List<DatasetFieldType> requiredThisMDB = new ArrayList<>();
        List<DatasetFieldType> allFieldsThisMDB = new ArrayList<>(mdb.getDatasetFieldTypes());
        
        // First collect all required fields for this metadata block
        for (DatasetFieldType dsft : requiredDSFT) {
            if(dsft.getMetadataBlock().equals(mdb)){
                numReq++;
                mdbHasReqField = true;
                requiredThisMDB.add(dsft);
            }
        }

        // Start building the schema for this metadata block
        retval += startOfMDB.replace("blockName", mdb.getName());
        
        // Add minItems constraint only if there are required fields
        if (mdbHasReqField) {
            retval += minItemsTemplate.replace("numMinItems", Integer.toString(requiredThisMDB.size()));
            
            // Add contains validation for each required field
            int count = 0;
            for (DatasetFieldType dsft : requiredThisMDB) {
                count++;
                String reqValImp = reqValTemplate.replace("reqFieldTypeName", dsft.getName());
                if (count < requiredThisMDB.size()) {
                    retval += reqValImp + "\n";
                } else {
                    reqValImp = StringUtils.substring(reqValImp, 0, reqValImp.length() - 1);
                    retval += reqValImp + "\n";
                    retval += endOfReqVal;
                }
            }
        } else {
            // If no required fields, just close the items definition
            retval += "\n                                    \"items\": {\n" +
                     "                                        \"$ref\": \"#/$defs/field\"\n" +
                     "                                    }\n" +
                     "                                }\n" +
                     "                            },\n" +
                     "                            \"required\": [\"fields\"]\n" +
                     "                        }";
        }
        
        return retval;
    }
    
    public String isDatasetJsonValid(String dataverseAlias, String jsonInput) {
        Map<String, Map<String,List<String>>> schemaChildMap = new HashMap<>();
        JSONObject rawSchema = new JSONObject(new JSONTokener(getCollectionDatasetSchema(dataverseAlias, schemaChildMap)));
        
        try {
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(jsonInput)); // throws a ValidationException if this object is invalid
            JSONDataValidation.validate(schema, schemaChildMap, jsonInput); // throws a ValidationException if any objects are invalid
        } catch (ValidationException vx) {
            logger.info(BundleUtil.getStringFromBundle("dataverses.api.validate.json.failed") + " " + vx.getErrorMessage()); 
            String accumulatedexceptions = "";
            for (ValidationException va : vx.getCausingExceptions()){
                accumulatedexceptions = accumulatedexceptions + va;
                accumulatedexceptions = accumulatedexceptions.replace("org.everit.json.schema.ValidationException:", " ");
            }
            if (!accumulatedexceptions.isEmpty()){
                return BundleUtil.getStringFromBundle("dataverses.api.validate.json.failed") + " "  + accumulatedexceptions;
            } else {
                return BundleUtil.getStringFromBundle("dataverses.api.validate.json.failed") + " "  + vx.getErrorMessage();
            }
            
        } catch (Exception ex) {            
            logger.info(BundleUtil.getStringFromBundle("dataverses.api.validate.json.exception") + ex.getLocalizedMessage());
            return BundleUtil.getStringFromBundle("dataverses.api.validate.json.exception") + ex.getLocalizedMessage();
        } 

        return BundleUtil.getStringFromBundle("dataverses.api.validate.json.succeeded");
    }
    
    static String getBaseSchemaStringFromFile(String pathToJsonFile) {
        File datasetSchemaJson = new File(pathToJsonFile);
        try {
            String datasetSchemaAsJson = new String(Files.readAllBytes(Paths.get(datasetSchemaJson.getAbsolutePath())));
            return datasetSchemaAsJson;
        } catch (IOException ex) {
            logger.info("IO - failed to get schema file  - will build on fly " +ex.getMessage());
            return null;
        } catch (Exception e){
            logger.info("Other exception - failed to get schema file  - will build on fly. " + e.getMessage());
            return null;
        }
    }
    
    private  String datasetSchemaPreface = 
    "{\n" +
    "    \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
    "    \"$defs\": {\n" +
    "    \"field\": {\n" + 
    "        \"type\": \"object\",\n" +
    "        \"required\": [\"typeClass\", \"multiple\", \"typeName\"],\n" +
    "        \"properties\": {\n" + 
    "            \"value\": {\n" +
    "                \"anyOf\": [\n" +
    "                    {\n" +
    "                        \"type\": \"array\"\n" +
    "                    },\n" +
    "                    {\n" + 
    "                        \"type\": \"string\"\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"$ref\": \"#/$defs/field\"\n" +
    "                    }\n" + 
    "                ]\n" + 
    "            },\n" + 
    "            \"typeClass\": {\n" +
    "                \"type\": \"string\"\n" +
    "            },\n" +
    "            \"multiple\": {\n" +
    "                \"type\": \"boolean\"\n" +
    "            },\n" +
    "            \"typeName\": {\n" + 
    "                \"type\": \"string\"\n" +
    "            },\n" +
    "            \"displayOnCreate\": {\n" +
    "                \"type\": \"boolean\"\n" +
    "            }\n" +
    "        }\n" +
    "    }\n" + 
    "},\n" + 
    "\"type\": \"object\",\n" +
    "\"properties\": {\n" + 
    "    \"datasetVersion\": {\n" + 
    "        \"type\": \"object\",\n" +
    "        \"properties\": {\n" + 
    "           \"license\": {\n" + 
    "                \"type\": \"object\",\n" + 
    "                \"properties\": {\n" + 
    "                    \"name\": {\n" +
    "                        \"type\": \"string\"\n" + 
    "                    },\n" + 
    "                    \"uri\": {\n" + 
    "                        \"type\": \"string\",\n" + 
    "                        \"format\": \"uri\"\n" + 
    "                   }\n" + 
    "                },\n" + 
    "                \"required\": [\"name\", \"uri\"]\n" + 
    "            },\n" + 
    "            \"metadataBlocks\": {\n" + 
    "                \"type\": \"object\",\n" + 
    "               \"properties\": {\n" +
    ""  ;
    
    private String startOfMDB = "" +
"                           \"blockName\": {\n" +
"                            \"type\": \"object\",\n" +
"                            \"properties\": {\n" +
"                                \"fields\": {\n" +
"                                    \"type\": \"array\",\n" +
"                                    \"items\": {\n" +
"                                        \"$ref\": \"#/$defs/field\"\n" +
"                                    },";
    
    private String reqValTemplate = "                                        {\n" +
"                                            \"contains\": {\n" +
"                                                \"properties\": {\n" +
"                                                    \"typeName\": {\n" +
"                                                        \"const\": \"reqFieldTypeName\"\n" +
"                                                    }\n" +
"                                                }\n" +
"                                            }\n" +
"                                        },";
    
    private String minItemsTemplate = "\n                                    \"minItems\": numMinItems,\n" +
"                                    \"allOf\": [\n";
    private String endOfReqVal = "                                    ]\n" +
"                                }\n" +
"                            },\n" +
"                            \"required\": [\"fields\"]\n" +
"                        }";
    
    private String endOfjson = ",\n" +
"                    \"required\": [blockNames]\n" +
"                }\n" +
"            },\n" +
"            \"required\": [\"metadataBlocks\"]\n" +
"        }\n" +
"    },\n" +
"    \"required\": [\"datasetVersion\"]\n" +
"}\n";
    
    public void saveStorageQuota(Dataverse target, Long allocation) {
        StorageQuota storageQuota = target.getStorageQuota();
        
        if (storageQuota != null) {
            storageQuota.setAllocation(allocation);
            em.merge(storageQuota);
        } else {
            storageQuota = new StorageQuota(); 
            storageQuota.setDefinitionPoint(target);
            storageQuota.setAllocation(allocation);
            target.setStorageQuota(storageQuota);
            em.persist(storageQuota);
        }
        em.flush();
    }
    
    public void disableStorageQuota(StorageQuota storageQuota) {
        if (storageQuota != null && storageQuota.getAllocation() != null) {
            storageQuota.setAllocation(null);
            em.merge(storageQuota);
            em.flush();
        }
    }

    /**
     * Returns the total number of Dataverses
     * @return the number of dataverse in the database
     */
    public long getDataverseCount() {
        return em.createNamedQuery("Dataverse.countAll", Long.class).getSingleResult();
    }

    /**
     * Returns the total number of published datasets within a Dataverse collection. The number includes harvested and
     * linked datasets. Datasets in subcollections are also counted.
     * @param dvId ID of a Dataverse collection
     * @return the total number of published datasets within that Dataverse collection
     */
    public long getDatasetCount(Long dvId) {
        Set<Long> dvIds = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        dvIds.add(dvId);
        stack.push(dvId);

        // Collect IDs of all subdataverses
        while (!stack.isEmpty()) {
            Long currentId = stack.pop();
            List<Long> children = em.createQuery("SELECT d.id FROM Dataverse d WHERE d.owner.id = :parentId", Long.class)
                                    .setParameter("parentId", currentId)
                                    .getResultList();

            for (Long childId : children) {
                if (dvIds.add(childId)) {
                    stack.push(childId);
                }
            }
        }

        return em.createNamedQuery("Dataverse.getDatasetCount", Long.class)
                 .setParameter("ids", dvIds)
                 .setParameter("datasetState", VersionState.RELEASED)
                 .getSingleResult();
    }
}
