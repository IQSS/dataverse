package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.settings.JvmSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

@Named
@Stateless
public class SolrIndexServiceBean {

    private static final Logger logger = Logger.getLogger(SolrIndexServiceBean.class.getCanonicalName());

    @EJB
    private SolrIndexServiceBean self; // Self-injection to allow calling methods in new transactions (from other methods in this bean)
    
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SearchPermissionsServiceBean searchPermissionsService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataverseRoleServiceBean rolesSvc;
    @EJB
    SolrClientIndexService solrClientService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public static String numRowsClearedByClearAllIndexTimes = "numRowsClearedByClearAllIndexTimes";
    public static String messageString = "message";

    public List<DvObjectSolrDoc> determineSolrDocs(DvObject dvObject) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        if (dvObject == null) {
            return emptyList;
        }
        List<DvObjectSolrDoc> solrDocs = emptyList;
        if (dvObject.isInstanceofDataverse()) {
            DvObjectSolrDoc dataverseSolrDoc = constructDataverseSolrDoc((Dataverse) dvObject);
            solrDocs.add(dataverseSolrDoc);
        } else if (dvObject.isInstanceofDataset()) {
            List<DvObjectSolrDoc> datasetSolrDocs = constructDatasetSolrDocs((Dataset) dvObject);
            solrDocs.addAll(datasetSolrDocs);
        } else if (dvObject.isInstanceofDataFile()) {
            DataFile datafile = (DataFile) dvObject;
            Set<DatasetVersion> datasetVersions = datasetVersionsToBuildCardsFor(datafile.getOwner());
            for (DatasetVersion version : datasetVersions) {
                if (datafile.isInDatasetVersion(version)) {
                    List<String> cachedPerms = searchPermissionsService.findDatasetVersionPerms(version);
                    String solrIdEnd = getDatasetOrDataFileSolrEnding(version.getVersionState());
                    Long versionId = version.getId();
                    DvObjectSolrDoc fileSolrDoc = constructDatafileSolrDoc(new DataFileProxy(datafile.getFileMetadata()), cachedPerms, versionId, solrIdEnd);
                    solrDocs.add(fileSolrDoc);
                }
            }
        } else {
            logger.info("Unexpected DvObject: " + dvObject.getClass().getName());
        }
        return solrDocs;
    }

    private List<DvObjectSolrDoc> determineSolrDocsForFilesFromDataset(Map.Entry<Long, List<Long>> datasetHash) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        DvObject dvObject = dvObjectService.findDvObject(datasetHash.getKey());
        if (dvObject == null) {
            return emptyList;
        }
        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;
            solrDocs.addAll(constructDatafileSolrDocsFromDataset(dataset));
        }
        return solrDocs;
    }

    /**
     * @todo should this method return a List? The equivalent methods for
     * datasets and files return lists.
     */
    private DvObjectSolrDoc constructDataverseSolrDoc(Dataverse dataverse) {
        List<String> perms = new ArrayList<>();
        if (dataverse.isReleased()) {
            perms.add(IndexServiceBean.getPublicGroupString());
        } else {
            perms = searchPermissionsService.findDataversePerms(dataverse);
        }
        Long noDatasetVersionForDataverses = null;
        DvObjectSolrDoc dvDoc = new DvObjectSolrDoc(dataverse.getId().toString(), IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId(), noDatasetVersionForDataverses, dataverse.getName(), perms);
        return dvDoc;
    }

    private List<DvObjectSolrDoc> constructDatasetSolrDocs(Dataset dataset) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
            DvObjectSolrDoc datasetSolrDoc = makeDatasetSolrDoc(version);
            solrDocs.add(datasetSolrDoc);
        }
        return solrDocs;
    }

    private DvObjectSolrDoc constructDatafileSolrDoc(DataFileProxy fileProxy, List<String> cachedPerms, long versionId, String solrIdEnd) {
        String solrIdStart = IndexServiceBean.solrDocIdentifierFile + fileProxy.getFileId();
        String solrId = solrIdStart + solrIdEnd;
        List<String> perms = new ArrayList<>();
        assert(cachedPerms != null);
        if (cachedPerms != null) {
            logger.finest("reusing cached perms for file " + fileProxy.getFileId());
            perms = cachedPerms;
        }
        return new DvObjectSolrDoc(fileProxy.getFileId().toString(), solrId, versionId, fileProxy.getName(), perms);
    }

    private List<DvObjectSolrDoc> constructDatafileSolrDocsFromDataset(Dataset dataset) {
        List<DvObjectSolrDoc> datafileSolrDocs = new ArrayList<>();
        for (DatasetVersion datasetVersionFileIsAttachedTo : datasetVersionsToBuildCardsFor(dataset)) {
            List<String> perms = new ArrayList<>();
            if (datasetVersionFileIsAttachedTo.isReleased()) {
                perms.add(IndexServiceBean.getPublicGroupString());
            } else {
                perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
            }

            for (FileMetadata fileMetadata : datasetVersionFileIsAttachedTo.getFileMetadatas()) {
                Long fileId = fileMetadata.getDataFile().getId();
                String solrIdStart = IndexServiceBean.solrDocIdentifierFile + fileId;
                String solrIdEnd = getDatasetOrDataFileSolrEnding(datasetVersionFileIsAttachedTo.getVersionState());
                String solrId = solrIdStart + solrIdEnd;
                DvObjectSolrDoc dataFileSolrDoc = new DvObjectSolrDoc(fileId.toString(), solrId, datasetVersionFileIsAttachedTo.getId(), fileMetadata.getLabel(), perms);
                logger.finest("adding fileid " + fileId);
                datafileSolrDocs.add(dataFileSolrDoc);
            }
        }
        return datafileSolrDocs;
    }

    /** Find the versions to index. The overall logic is
     *  If there is only one version, or no released version (all non-draft versions are deaccessioned)
     *    then index it regardless of it's versionstate
     *  If there are released versions
     *    then index the latest released version and a draft version if one exists
     *  Hence - the latest deaccessioned version is only indexed if there is no released version
     * @param dataset
     * @return  the set of versions to build cards for
     */
    private Set<DatasetVersion> datasetVersionsToBuildCardsFor(Dataset dataset) {
        Set<DatasetVersion> datasetVersions = new HashSet<>();
        DatasetVersion latest = dataset.getLatestVersion();
        DatasetVersion released = dataset.getReleasedVersion();
        if (latest != null && (released == null || latest.isDraft())) {
            datasetVersions.add(latest);
        }
        if (released != null) {
            //May be the same as the latest version - only one copy will be in the set in that case
            datasetVersions.add(released);
        }
        return datasetVersions;
    }

    private DvObjectSolrDoc makeDatasetSolrDoc(DatasetVersion version) {
        String solrIdStart = IndexServiceBean.solrDocIdentifierDataset + version.getDataset().getId().toString();
        String solrIdEnd = getDatasetOrDataFileSolrEnding(version.getVersionState());
        String solrId = solrIdStart + solrIdEnd;
        String name = version.getTitle();
        List<String> perms = new ArrayList<>();
        if (version.isReleased()) {
            perms.add(IndexServiceBean.getPublicGroupString());
        } else {
            perms = searchPermissionsService.findDatasetVersionPerms(version);
        }
        return new DvObjectSolrDoc(version.getDataset().getId().toString(), solrId, version.getId(), name, perms);
    }

    private String getDatasetOrDataFileSolrEnding(DatasetVersion.VersionState versionState) {
        if (versionState.equals(DatasetVersion.VersionState.RELEASED)) {
            return "";
        } else if (versionState.equals(DatasetVersion.VersionState.DRAFT)) {
            return IndexServiceBean.draftSuffix;
        } else if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {
            return IndexServiceBean.deaccessionedSuffix;
        } else {
            return "_unexpectedDatasetVersion";
        }
    }

    public IndexResponse indexAllPermissions() {
        Collection<SolrInputDocument> docs = new ArrayList<>();

        List<DvObjectSolrDoc> definitionPoints = new ArrayList<>();
        Map<Long, List<Long>> filesPerDataset = new HashMap<>();
        List<DvObject> allExceptFiles = dvObjectService.findAll();
        for (DvObject dvObject : allExceptFiles) {
            logger.fine("determining definition points for dvobject id " + dvObject.getId());
            if (dvObject.isInstanceofDataFile()) {
                Long dataset = dvObject.getOwner().getId();
                Long datafile = dvObject.getId();

                List<Long> files = filesPerDataset.get(dataset);
                if (files == null) {
                    files = new ArrayList<>();
                    filesPerDataset.put(dataset, files);
                }
                files.add(datafile);
            } else {
                definitionPoints.addAll(determineSolrDocs(dvObject));
            }
        }

        List<DvObject> all = allExceptFiles;
        for (Map.Entry<Long, List<Long>> filePerDataset : filesPerDataset.entrySet()) {
            definitionPoints.addAll(determineSolrDocsForFilesFromDataset(filePerDataset));
            for (long fileId : filePerDataset.getValue()) {
                DvObject file = dvObjectService.findDvObject(fileId);
                if (file != null) {
                    all.add(file);
                }
            }
        }

        for (DvObjectSolrDoc dvObjectSolrDoc : definitionPoints) {
            logger.fine("creating solr doc in memory for " + dvObjectSolrDoc.getSolrId());
            SolrInputDocument solrInputDocument = SearchUtil.createSolrDoc(dvObjectSolrDoc);
            logger.fine("adding to list of docs to index " + dvObjectSolrDoc.getSolrId());
            docs.add(solrInputDocument);
        }
        try {
            persistToSolr(docs);
            /**
             * @todo Do we need a separate permissionIndexTime timestamp?
             * Probably. Update it here.
             */
            for (DvObject dvObject : all) {
                dvObjectService.updatePermissionIndexTime(dvObject);
            }
            return new IndexResponse("indexed all permissions");
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    public IndexResponse indexPermissionsForOneDvObject(DvObject dvObject) {
        if (dvObject == null) {
            return new IndexResponse("problem indexing... null DvObject passed in");
        }
        long dvObjectId = dvObject.getId();
        Collection<SolrInputDocument> docs = new ArrayList<>();

        List<DvObjectSolrDoc> definitionPoints = determineSolrDocs(dvObject);

        for (DvObjectSolrDoc dvObjectSolrDoc : definitionPoints) {
            SolrInputDocument solrInputDocument = SearchUtil.createSolrDoc(dvObjectSolrDoc);
            docs.add(solrInputDocument);
        }
        try {
            persistToSolr(docs);
            boolean updatePermissionTimeSuccessful = false;
            if (dvObject != null) {
                DvObject savedDvObject = dvObjectService.updatePermissionIndexTime(dvObject);
                if (savedDvObject != null) {
                    updatePermissionTimeSuccessful = true;
                }
            }
            return new IndexResponse("attempted to index permissions for DvObject " + dvObjectId + " and updatePermissionTimeSuccessful was " + updatePermissionTimeSuccessful);
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    private void persistToSolr(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        if (docs.isEmpty()) {
            // This method is routinely called with an empty list of docs.
            logger.fine("nothing to persist");
            return;
        }
        logger.fine("persisting to Solr...");
        /**
         * @todo Do something with these responses from Solr.
         */
        solrClientService.getSolrClient().add(docs);
    }

    /**
     * We use the database to determine direct children since there is no
     * inheritance. This implementation uses smaller transactions to avoid memory issues.
     */
    public IndexResponse indexPermissionsOnSelfAndChildren(DvObject definitionPoint) {

        if (definitionPoint == null) {
            logger.log(Level.WARNING, "Cannot perform indexPermissionsOnSelfAndChildren with a definitionPoint null");
            return null;
        }
        int fileQueryMin = JvmSettings.MIN_FILES_TO_USE_PROXY.lookupOptional(Integer.class).orElse(Integer.MAX_VALUE);
        final int[] counter = { 0 };
        int numObjects = 0;
        long globalStartTime = System.currentTimeMillis();

        // Handle the definition point itself in its own transaction
        if (definitionPoint instanceof Dataverse dataverse) {
            // We don't create a Solr "primary/content" doc for the root dataverse
            // so don't create a Solr "permission" doc either.
            if (!dataverse.equals(dataverseService.findRootDataverse())) {
                indexPermissionsForOneDvObject(definitionPoint);
                numObjects++;
            }

            // Process datasets in batches
            List<Long> datasetIds = datasetService.findIdsByOwnerId(dataverse.getId());
            int batchSize = 10; // Process 10 datasets per transaction

            for (int i = 0; i < datasetIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, datasetIds.size());
                List<Long> batchIds = datasetIds.subList(i, endIndex);

                // Process this batch of datasets in a new transaction
                self.indexDatasetBatchInNewTransaction(batchIds, counter, fileQueryMin);
                numObjects += batchIds.size();

                logger.fine("Permission reindexing: Processed batch " + (i/batchSize + 1) + " of " + 
                        (int) Math.ceil(datasetIds.size() / (double) batchSize) +
                        " dataset batches for dataverse " + dataverse.getId());
            }
        } else if (definitionPoint instanceof Dataset dataset) {
            // For a single dataset, process it in its own transaction
            indexPermissionsForOneDvObject(definitionPoint);
            numObjects++;

            /**
             * Prepare the data needed for the new transaction. For performance reasons, indexDatasetFilesInNewTransaction does not merge the dataset or versions into the new transaction (we only read info, there
             * are no changes to write). However, there are two ways the code here is used. In one case, indexing content and permissions, the versions and fileMetadatas in them are already loaded. In the other
             * case, indexing permissions only, the fileMetadatas are not yet loaded, and we may need them, but only if there are fewer than fileQueryMin. For each version that will get reindexed (at most two of
             * them), the code below does a lightweight query to see how many fileMetadatas exist in it and, if it is equal to or below fileQueryMin, calls getFileMetadatas().size() to assure they are loaded
             * (before we pass the version into a new transaction where it will be detached and fileMetadatas can't be loaded). Calling getFileMetadas.size() should be lightweight when the fileMetadatas are
             * loaded (first case) and done only when needed for the second case.
             * 
             **/
            List<DatasetVersion> versionsToIndex = new ArrayList<>();
            for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
                int fileCount = dataFileService.findCountByDatasetVersionId(version.getId()).intValue();
                if (fileCount >= fileQueryMin) {
                    // IMPORTANT: This triggers the loading of fileMetadatas within the current transaction
                    version.getFileMetadatas().size();
                }
                versionsToIndex.add(version);
            }

            // Process the dataset's files in a new transaction, passing the pre-loaded data
            self.indexDatasetFilesInNewTransaction(versionsToIndex, counter, fileQueryMin);
        } else {
            // For other types (like files), just index in a new transaction
            indexPermissionsForOneDvObject(definitionPoint);
            numObjects++;
        }

        logger.fine("Reindexed permissions for " + counter[0] + " files and " + numObjects +
                " datasets/collections in " + (System.currentTimeMillis() - globalStartTime) + " ms");

        return new IndexResponse("Number of dvObject permissions indexed for " + definitionPoint + ": " + numObjects);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void indexDatasetBatchInNewTransaction(List<Long> datasetIds, final int[] fileCounter, int fileQueryMin) {
        for (Long datasetId : datasetIds) {
            Dataset dataset = datasetService.find(datasetId);
            if (dataset != null) {
                indexPermissionsForOneDvObject(dataset);

                // Process files for this dataset
                List<DatasetVersion> versions = datasetVersionsToBuildCardsFor(dataset);
                final List<Long> changedFileIds = new ArrayList<>();
                if(versions.size()>1) {
                    Long releasedVersionId = versions.get(versions.get(0).isReleased() ? 0 : 1).getId();
                    Long draftVersionId = versions.get(versions.get(0).isReleased() ? 1 : 0).getId();
                    
                    populateChangedFileIds(
                            releasedVersionId, 
                            draftVersionId, 
                            changedFileIds
                        );
                }
                for (DatasetVersion version : versions) {
                    processDatasetVersionFiles(version, fileCounter, fileQueryMin, (versions.size()>1 && version.isDraft()) ? changedFileIds : null);
                    }
                }
            }
        }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void indexDatasetFilesInNewTransaction(List<DatasetVersion> versions, final int[] fileCounter, int fileQueryMin) {
        final List<Long> changedFileIds = new ArrayList<>();
        if(versions.size()>1) {
            Long releasedVersionId = versions.get(versions.get(0).isReleased() ? 0 : 1).getId();
            Long draftVersionId = versions.get(versions.get(0).isReleased() ? 1 : 0).getId();
            
            populateChangedFileIds(
                    releasedVersionId, 
                    draftVersionId, 
                    changedFileIds
                );
        }
        for (DatasetVersion version : versions) {
            // The version object is detached, but its fileMetadatas collection is already loaded.
            // We only need its ID and state, which are available.
            processDatasetVersionFiles(version, fileCounter, fileQueryMin, (versions.size()>1 && version.isDraft()) ? changedFileIds : null);
        }
    }

    /**
     * Retrieves the IDs of file metadatas that have changed between the released version
     * and the draft version of a dataset.
     * 
     * @param releasedVersionId the ID of the released dataset version
     * @param draftVersionId the ID of the draft dataset version
     * @param changedFileMetadataIds the list to populate with changed file metadata IDs
     */
    public void populateChangedFileIds(Long releasedVersionId, Long draftVersionId, List<Long> changedFileIds) {
        Query query = em.createNamedQuery("FileMetadata.getDatafilesWithChangedMetadata", Long.class);
        query.setParameter(1, releasedVersionId);
        query.setParameter(2, draftVersionId);

        /*
         * When the query was configured to return Long, it was returning Integer. 
         * The query has been changed to return Integer now. The code here is robust 
         * if that changes in the future.
         */
        List<Object> queryResults = query.getResultList();
        for (Object result : queryResults) {
            if (result != null) {
                // Ensure we're adding Long objects to the list
                if (result instanceof Integer intResult) {
                    logger.finest("Converted Integer result to Long: " + result);
                    changedFileIds.add(Long.valueOf(intResult));
                } else if (result instanceof Long longResult) {
                    // Already a Long, add directly
                    logger.finest("Added existing Long to list: " + result);
                    changedFileIds.add(longResult);
                } else {
                    // If it's not a Long, convert it to one via String
                    try {
                        changedFileIds.add(Long.valueOf(result.toString()));
                        logger.finest("Converted non-Long result to Long: " + result + " of type " + result.getClass().getName());
                    } catch (NumberFormatException e) {
                        logger.warning("Could not convert query result to Long: " + result);
                    }
                }
            }
        }
    }
    
    private void processDatasetVersionFiles(DatasetVersion version,
            final int[] fileCounter, int fileQueryMin, List<Long> changedFileIds) {
        List<String> cachedPerms = searchPermissionsService.findDatasetVersionPerms(version);
        String solrIdEnd = getDatasetOrDataFileSolrEnding(version.getVersionState());
        Long versionId = version.getId();
        List<DataFileProxy> filesToReindexAsBatch = new ArrayList<>();

        // If the version is draft and there is a released version, 
        // we only need perm docs for the files with filemetadata changes == those in changedFileMetadataIds
        
        // Process files in batches of 100
        int batchSize = 100;

        if (dataFileService.findCountByDatasetVersionId(version.getId()).intValue() > fileQueryMin) {
            // For large datasets, use a more efficient SQL query
            // ToDo - only get the ones in finalFileIdsToReindex
            try (Stream<DataFileProxy> fileStream = getDataFileInfoForPermissionIndexing(version.getId())) {

                // Process files in batches to avoid memory issues
                fileStream.forEach(fileInfo -> {
                    // Only add files that need reindexing
                if (changedFileIds == null || changedFileIds.contains(fileInfo.getFileId())) {
                        filesToReindexAsBatch.add(fileInfo);
                        fileCounter[0]++;

                        if (filesToReindexAsBatch.size() >= batchSize) {
                            reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                            filesToReindexAsBatch.clear();
                        }
                    }
                });
            }
        } else {
            // For smaller datasets, process files directly
            // We only call getFileMetadatas() in the case where we know they have already been loaded
            for (FileMetadata fmd : version.getFileMetadatas()) {
                // Only add files that need reindexing
                DataFileProxy fileProxy = new DataFileProxy(fmd);
                if (changedFileIds == null || changedFileIds.contains(fileProxy.getFileId())) {
                    filesToReindexAsBatch.add(fileProxy);
                    fileCounter[0]++;

                    if (filesToReindexAsBatch.size() >= batchSize) {
                        reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                        filesToReindexAsBatch.clear();
                    }
                }
            }
        }

        // Process any remaining files
        if (!filesToReindexAsBatch.isEmpty()) {
            reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
        }
    }

    private void reindexFilesInBatches(List<DataFileProxy> filesToReindexAsBatch, List<String> cachedPerms, Long versionId, String solrIdEnd) {
        List<SolrInputDocument> docs = new ArrayList<>();
        try {
            // Assume all files have the same owner
            if (filesToReindexAsBatch.isEmpty()) {
                logger.warning("reindexFilesInBatches called incorrectly with an empty file list");
            }

            for (DataFileProxy file : filesToReindexAsBatch) {

                DvObjectSolrDoc fileSolrDoc = constructDatafileSolrDoc(file, cachedPerms, versionId, solrIdEnd);
                SolrInputDocument solrDoc = SearchUtil.createSolrDoc(fileSolrDoc);
                docs.add(solrDoc);
            }
            persistToSolr(docs);
            logger.fine("Indexed " + filesToReindexAsBatch.size() + " files across " + docs.size() + " Solr documents");
        } catch (SolrServerException | IOException ex) {
            logger.log(Level.WARNING, "Failed to reindex " + filesToReindexAsBatch.size() +
                    " files across " + docs.size() + " Solr documents", ex);
        }
    }

    public IndexResponse deleteMultipleSolrIds(List<String> solrIdsToDelete) {
        if (solrIdsToDelete.isEmpty()) {
            return new IndexResponse("nothing to delete");
        }
        try {
            solrClientService.getSolrClient().deleteById(solrIdsToDelete);
        } catch (SolrServerException | IOException ex) {
            /**
             * @todo mark these for re-deletion
             */
            return new IndexResponse("problem deleting the following documents from Solr: " + solrIdsToDelete);
        }
        return new IndexResponse("no known problem deleting the following documents from Solr:" + solrIdsToDelete);
    }

    public JsonObjectBuilder deleteAllFromSolrAndResetIndexTimes() throws SolrServerException, IOException {
        JsonObjectBuilder response = Json.createObjectBuilder();
        logger.fine("attempting to delete all Solr documents before a complete re-index");
        solrClientService.getSolrClient().deleteByQuery("*:*");
        int numRowsAffected = dvObjectService.clearAllIndexTimes();
        response.add(numRowsClearedByClearAllIndexTimes, numRowsAffected);
        response.add(messageString, "Solr index and database index timestamps cleared.");
        return response;
    }

    /**
     * @return A list of dvobject ids that should have their permissions
     * re-indexed because Solr was down when a permission was added. The
     * permission should be added to Solr. The id of the permission contains the
     * type of DvObject and the primary key of the dvObject. DvObjects of type
     * DataFile are currently skipped because their index time isn't stored in
     * the database, since they are indexed along with their parent dataset
     * (this may change).
     */
    public List<Long> findPermissionsInDatabaseButStaleInOrMissingFromSolr() {
        List<Long> indexingRequired = new ArrayList<>();
        long rootDvId = dataverseService.findRootDataverse().getId();
        List<Long> missingDataversePermissionIds = dataverseService.findIdStalePermission();
        List<Long> missingDatasetPermissionIds = datasetService.findIdStalePermission();
        for (Long id : missingDataversePermissionIds) {
            if (!id.equals(rootDvId)) {
                indexingRequired.add(id);
            }
        }
        indexingRequired.addAll(missingDatasetPermissionIds);
        return indexingRequired;
    }

    public Stream<DataFileProxy> getDataFileInfoForPermissionIndexing(Long id) {
        return em.createNamedQuery("DataFile.getDataFileInfoForPermissionIndexing", DataFileProxy.class)
                .setParameter(1, id)
                .getResultStream();
    }

    /**
     * A lightweight proxy for DataFile objects used during permission indexing. This class avoids loading the full DataFile entity from the database when only basic properties are needed for indexing,
     * improving performance for large datasets.
     */
    public static class DataFileProxy {

        private final Long id;
        private final String name;
        private final boolean released;

        /**
         * Creates a new DataFileProxy with the specified properties.
         * 
         * @param id
         *            The ID of the data file
         * @param label
         *            The label/name of the data file
         * @param restricted
         *            Whether the file is restricted
         * @param released
         *            Whether the file is released
         */
        public DataFileProxy(FileMetadata fmd) {
            DataFile df = fmd.getDataFile();
            this.id = df.getId();
            this.name = fmd.getLabel();
            this.released = df.isReleased();
        }

        public DataFileProxy(String label, Long id, Date publicationDate) {
            this.id = id;
            this.name = label;
            this.released = publicationDate != null;
        }

        public boolean isReleased() {
            return released;
        }

        public Long getFileId() {
            return id;
        }

        public String getName() {
            return name;
        }
        
        public DataFile getMinimalDataFile() {
            DataFile df = new DataFile();
            df.setId(id);
            return df;
        }
    }
}
