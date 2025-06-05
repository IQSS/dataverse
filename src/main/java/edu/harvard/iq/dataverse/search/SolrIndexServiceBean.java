package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
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
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

@Named
@Stateless
public class SolrIndexServiceBean {

    private static final Logger logger = Logger.getLogger(SolrIndexServiceBean.class.getCanonicalName());

    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SearchPermissionsServiceBean searchPermissionsService;
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
            Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(datafile.getOwner());
            Set<DatasetVersion> datasetVersions = datasetVersionsToBuildCardsFor(datafile.getOwner());
            for (DatasetVersion version : datasetVersions) {
                if(desiredCards.containsKey(version.getVersionState()) && desiredCards.get(version.getVersionState()) && datafile.isInDatasetVersion(version)) {
                            List<String> cachedPerms = searchPermissionsService.findDatasetVersionPerms(version);
                            String solrIdEnd = getDatasetOrDataFileSolrEnding(version.getVersionState());
                            Long versionId = version.getId();
                            DvObjectSolrDoc fileSolrDoc = constructDatafileSolrDoc(new DataFileProxy(datafile.getFileMetadata()),  cachedPerms, versionId, solrIdEnd);
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
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
            boolean cardShouldExist = desiredCards.get(version.getVersionState());
            if (cardShouldExist) {
                DvObjectSolrDoc datasetSolrDoc = makeDatasetSolrDoc(version);
                solrDocs.add(datasetSolrDoc);
            }
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
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion datasetVersionFileIsAttachedTo : datasetVersionsToBuildCardsFor(dataset)) {
            boolean cardShouldExist = desiredCards.get(datasetVersionFileIsAttachedTo.getVersionState());
            if (cardShouldExist) {
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
        }
        return datafileSolrDocs;
    }

    private Set<DatasetVersion> datasetVersionsToBuildCardsFor(Dataset dataset) {
        Set<DatasetVersion> datasetVersions = new HashSet<>();
        DatasetVersion latest = dataset.getLatestVersion();
        if (latest != null) {
            datasetVersions.add(latest);
        }
        DatasetVersion released = dataset.getReleasedVersion();
        if (released != null) {
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
     * inheritance
     */
    public IndexResponse indexPermissionsOnSelfAndChildren(DvObject definitionPoint) {

        if (definitionPoint == null) {
            logger.log(Level.WARNING, "Cannot perform indexPermissionsOnSelfAndChildren with a definitionPoint null");
            return null;
        }
        int fileQueryMin= JvmSettings.MIN_FILES_TO_USE_PROXY.lookupOptional(Integer.class).orElse(Integer.MAX_VALUE);
        List<DataFileProxy> filesToReindexAsBatch = new ArrayList<>();
        /**
         * @todo Re-indexing the definition point itself seems to be necessary
         * for revoke but not necessarily grant.
         */

        // We don't create a Solr "primary/content" doc for the root dataverse
        // so don't create a Solr "permission" doc either.
        final int[] counter = {0};
        int numObjects = 0;
        long globalStartTime = System.currentTimeMillis();
        if (definitionPoint.isInstanceofDataverse()) {
            Dataverse selfDataverse = (Dataverse) definitionPoint;
            if (!selfDataverse.equals(dataverseService.findRootDataverse())) {
                indexPermissionsForOneDvObject(definitionPoint);
                numObjects++;
            }
            List<Dataset> directChildDatasetsOfDvDefPoint = datasetService.findByOwnerId(selfDataverse.getId());
            for (Dataset dataset : directChildDatasetsOfDvDefPoint) {
                indexPermissionsForOneDvObject(dataset);
                numObjects++;

                Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
                long startTime = System.currentTimeMillis();
                for (DatasetVersion version : versionsToReIndexPermissionsFor(dataset)) {
                    if (desiredCards.get(version.getVersionState())) {
                        List<String> cachedPerms = searchPermissionsService.findDatasetVersionPerms(version);
                        String solrIdEnd = getDatasetOrDataFileSolrEnding(version.getVersionState());
                        Long versionId = version.getId();
                        for (FileMetadata fmd : version.getFileMetadatas()) {
                            DataFileProxy fileProxy = new DataFileProxy(fmd);
                            // Since reindexFilesInBatches() re-indexes a file in all versions needed, we should not send a file already in the released version twice
                            filesToReindexAsBatch.add(fileProxy);
                            counter[0]++;
                            if (counter[0] % 100 == 0) {
                                reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                                filesToReindexAsBatch.clear();
                            }
                            if (counter[0] % 1000 == 0) {
                                logger.fine("Progress: " + counter[0] + "files permissions reindexed");
                            }
                        }

                        // Re-index any remaining files in the datasetversion (so that verionId, etc. remain constants for all files in the batch)
                        reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                        logger.info("Progress : dataset " + dataset.getId() + " permissions reindexed in " + (System.currentTimeMillis() - startTime) + " ms");
                    }
                }
            }
        } else if (definitionPoint.isInstanceofDataset()) {
            indexPermissionsForOneDvObject(definitionPoint);
            numObjects++;
            // index files
            Dataset dataset = (Dataset) definitionPoint;
            Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
            for (DatasetVersion version : versionsToReIndexPermissionsFor(dataset)) {
                if (desiredCards.get(version.getVersionState())) {
                    List<String> cachedPerms = searchPermissionsService.findDatasetVersionPerms(version);
                    String solrIdEnd = getDatasetOrDataFileSolrEnding(version.getVersionState());
                    Long versionId = version.getId();
                    if (version.getFileMetadatas().size() > fileQueryMin) {
                        // For large datasets, use a more efficient SQL query instead of loading all file metadata objects
                        getDataFileInfoForPermissionIndexing(version.getId()).forEach(fileInfo -> {
                            filesToReindexAsBatch.add(fileInfo);
                            counter[0]++;

                            if (counter[0] % 100 == 0) {
                                long startTime = System.currentTimeMillis();
                                reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                                filesToReindexAsBatch.clear();
                                logger.fine("Progress: 100 file permissions at " + counter[0] + " files reindexed in " + (System.currentTimeMillis() - startTime) + " ms");
                            }
                        });
                    } else {
                        version.getFileMetadatas().stream()
                                .forEach(fmd -> {
                                    DataFileProxy fileProxy = new DataFileProxy(fmd);
                                    filesToReindexAsBatch.add(fileProxy);
                                    counter[0]++;
                                    if (counter[0] % 100 == 0) {
                                        long startTime = System.currentTimeMillis();
                                        reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                                        filesToReindexAsBatch.clear();
                                        logger.fine("Progress: 100 file permissions at  " + counter[0] + "files reindexed in " + (System.currentTimeMillis() - startTime) + " ms");
                                    }
                                });
                    }
                    // Re-index any remaining files in the dataset version (versionId, etc. remain constants for all files in the batch)
                    reindexFilesInBatches(filesToReindexAsBatch, cachedPerms, versionId, solrIdEnd);
                    filesToReindexAsBatch.clear();
                }

            }
        } else {
            indexPermissionsForOneDvObject(definitionPoint);
            numObjects++;
        }

        /**
         * @todo Error handling? What to do with response?
         *
         * @todo Should update timestamps, probably, even thought these are files, see
         *       https://github.com/IQSS/dataverse/issues/2421
         */
        logger.fine("Reindexed permissions for " + counter[0] + " files and " + numObjects + "datasets/collections in " + (System.currentTimeMillis() - globalStartTime) + " ms");
        return new IndexResponse("Number of dvObject permissions indexed for " + definitionPoint
                + ": " + numObjects);
    }

    private String reindexFilesInBatches(List<DataFileProxy> filesToReindexAsBatch, List<String> cachedPerms, Long versionId, String solrIdEnd) {
        List<SolrInputDocument> docs = new ArrayList<>();
        try {
            // Assume all files have the same owner
            if (filesToReindexAsBatch.isEmpty()) {
                return "No files to reindex";
            }

                    for (DataFileProxy file : filesToReindexAsBatch) {

                            DvObjectSolrDoc fileSolrDoc = constructDatafileSolrDoc(file, cachedPerms, versionId, solrIdEnd);
                            SolrInputDocument solrDoc = SearchUtil.createSolrDoc(fileSolrDoc);
                            docs.add(solrDoc);
                    }
            persistToSolr(docs);
            return " " + filesToReindexAsBatch.size() + " files indexed across " + docs.size() + " Solr documents ";
        } catch (SolrServerException | IOException ex) {
            return " tried to reindex " + filesToReindexAsBatch.size() + " files indexed across " + docs.size() + " Solr documents but caught exception: " + ex;
        }
    }

    private List<DatasetVersion> versionsToReIndexPermissionsFor(Dataset dataset) {
        List<DatasetVersion> versionsToReindexPermissionsFor = new ArrayList<>();
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
            boolean cardShouldExist = desiredCards.get(version.getVersionState());
            if (cardShouldExist) {
                    versionsToReindexPermissionsFor.add(version);
            }
        }
        return versionsToReindexPermissionsFor;
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
