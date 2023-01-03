package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
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
    DataverseRoleServiceBean rolesSvc;
    @EJB
    IndexServiceBean indexService;
    @EJB
    SolrClientService solrClientService;

    public static String numRowsClearedByClearAllIndexTimes = "numRowsClearedByClearAllIndexTimes";
    public static String messageString = "message";

    /**
     * @deprecated Now that MyData has shipped in 4.1 we have no plans to change
     * the unpublishedDataRelatedToMeModeEnabled boolean to false. We should
     * probably remove the boolean altogether to simplify the code.
     *
     * This non-default mode changes the behavior of the "Data Related To Me"
     * feature to be more like "**Unpublished** Data Related to Me" after you
     * have changed this boolean to true and run "index all".
     *
     * The "Data Related to Me" feature relies on *always* indexing permissions
     * regardless of if the DvObject is published or not.
     *
     * In "Unpublished Data Related to Me" mode, we first check if the DvObject
     * is published. If it's published, we set the search permissions to *only*
     * contain "group_public", which is quick and cheap to do. If the DvObject
     * in question is *not* public, we perform the expensive operation of
     * rooting around in the system to determine who should be able to
     * "discover" the unpublished version of DvObject. By default this mode is
     * *not* enabled. If you want to enable it, change the boolean to true and
     * run "index all".
     *
     * See also https://github.com/IQSS/dataverse/issues/50
     */
    @Deprecated
    private boolean unpublishedDataRelatedToMeModeEnabled = true;

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
            Map<Long, List<String>> permStringByDatasetVersion = new HashMap<>();
            List<DvObjectSolrDoc> fileSolrDocs = constructDatafileSolrDocs((DataFile) dvObject, permStringByDatasetVersion);
            solrDocs.addAll(fileSolrDocs);
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
        if (unpublishedDataRelatedToMeModeEnabled) {
            if (dataverse.isReleased()) {
                perms.add(IndexServiceBean.getPublicGroupString());
            } else {
                perms = searchPermissionsService.findDataversePerms(dataverse);
            }
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

//    private List<DvObjectSolrDoc> constructDatafileSolrDocs(DataFile dataFile) {
    private List<DvObjectSolrDoc> constructDatafileSolrDocs(DataFile dataFile, Map<Long, List<String>> permStringByDatasetVersion) {
        List<DvObjectSolrDoc> datafileSolrDocs = new ArrayList<>();
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataFile.getOwner());
        for (DatasetVersion datasetVersionFileIsAttachedTo : datasetVersionsToBuildCardsFor(dataFile.getOwner())) {
            boolean cardShouldExist = desiredCards.get(datasetVersionFileIsAttachedTo.getVersionState());
            if (cardShouldExist) {
                String solrIdStart = IndexServiceBean.solrDocIdentifierFile + dataFile.getId();
                String solrIdEnd = getDatasetOrDataFileSolrEnding(datasetVersionFileIsAttachedTo.getVersionState());
                String solrId = solrIdStart + solrIdEnd;
                List<String> perms = new ArrayList<>();
                if (unpublishedDataRelatedToMeModeEnabled) {
                    List<String> cachedPerms = null;
                    if (permStringByDatasetVersion != null) {
                        cachedPerms = permStringByDatasetVersion.get(datasetVersionFileIsAttachedTo.getId());
                    }
                    if (cachedPerms != null) {
                        logger.fine("reusing cached perms for file " + dataFile.getId());
                        perms = cachedPerms;
                    } else if (datasetVersionFileIsAttachedTo.isReleased()) {
                        logger.fine("no cached perms, file is public/discoverable/searchable for file " + dataFile.getId());
                        perms.add(IndexServiceBean.getPublicGroupString());
                    } else {
                        // go to the well (slow)
                        logger.fine("no cached perms, file is not public, finding perms for file " + dataFile.getId());
                        perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
                    }
                } else {
                    // This should never be executed per the deprecation notice on the boolean.
                    perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
                }
                DvObjectSolrDoc dataFileSolrDoc = new DvObjectSolrDoc(dataFile.getId().toString(), solrId, datasetVersionFileIsAttachedTo.getId(), dataFile.getDisplayName(), perms);
                datafileSolrDocs.add(dataFileSolrDoc);
            }
        }

        return datafileSolrDocs;
    }

    private List<DvObjectSolrDoc> constructDatafileSolrDocsFromDataset(Dataset dataset) {
        List<DvObjectSolrDoc> datafileSolrDocs = new ArrayList<>();
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion datasetVersionFileIsAttachedTo : datasetVersionsToBuildCardsFor(dataset)) {
            boolean cardShouldExist = desiredCards.get(datasetVersionFileIsAttachedTo.getVersionState());
            if (cardShouldExist) {
                List<String> perms = new ArrayList<>();
                if (unpublishedDataRelatedToMeModeEnabled) {
                    if (datasetVersionFileIsAttachedTo.isReleased()) {
                        perms.add(IndexServiceBean.getPublicGroupString());
                    } else {
                        perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
                    }
                } else {
                    perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
                }
                for (FileMetadata fileMetadata : datasetVersionFileIsAttachedTo.getFileMetadatas()) {
                    Long fileId = fileMetadata.getDataFile().getId();
                    String solrIdStart = IndexServiceBean.solrDocIdentifierFile + fileId;
                    String solrIdEnd = getDatasetOrDataFileSolrEnding(datasetVersionFileIsAttachedTo.getVersionState());
                    String solrId = solrIdStart + solrIdEnd;
                    DvObjectSolrDoc dataFileSolrDoc = new DvObjectSolrDoc(fileId.toString(), solrId, datasetVersionFileIsAttachedTo.getId(), fileMetadata.getLabel(), perms);
                    logger.fine("adding fileid " + fileId);
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
        if (unpublishedDataRelatedToMeModeEnabled) {
            if (version.isReleased()) {
                perms.add(IndexServiceBean.getPublicGroupString());
            } else {
                perms = searchPermissionsService.findDatasetVersionPerms(version);
            }
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
            logger.info("determining definition points for dvobject id " + dvObject.getId());
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
            logger.info("creating solr doc in memory for " + dvObjectSolrDoc.getSolrId());
            SolrInputDocument solrInputDocument = SearchUtil.createSolrDoc(dvObjectSolrDoc);
            logger.info("adding to list of docs to index " + dvObjectSolrDoc.getSolrId());
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
        UpdateResponse addResponse = solrClientService.getSolrClient().add(docs);
        UpdateResponse commitResponse = solrClientService.getSolrClient().commit();
    }

    public IndexResponse indexPermissionsOnSelfAndChildren(long definitionPointId) {
        DvObject definitionPoint = dvObjectService.findDvObject(definitionPointId);
        if ( definitionPoint == null ) {
            logger.log(Level.WARNING, "Cannot find a DvOpbject with id of {0}", definitionPointId);
            return null;
        } else {
            return indexPermissionsOnSelfAndChildren(definitionPoint);
        }
    }
    
    /**
     * We use the database to determine direct children since there is no
     * inheritance
     */
    public IndexResponse indexPermissionsOnSelfAndChildren(DvObject definitionPoint) {
        List<DvObject> dvObjectsToReindexPermissionsFor = new ArrayList<>();
        List<DataFile> filesToReindexAsBatch = new ArrayList<>();
        /**
         * @todo Re-indexing the definition point itself seems to be necessary
         * for revoke but not necessarily grant.
         */

        // We don't create a Solr "primary/content" doc for the root dataverse
        // so don't create a Solr "permission" doc either.
        if (definitionPoint.isInstanceofDataverse()) {
            Dataverse selfDataverse = (Dataverse) definitionPoint;
            if (!selfDataverse.equals(dataverseService.findRootDataverse())) {
                dvObjectsToReindexPermissionsFor.add(definitionPoint);
            }
            List<Dataset> directChildDatasetsOfDvDefPoint = datasetService.findByOwnerId(selfDataverse.getId());
            for (Dataset dataset : directChildDatasetsOfDvDefPoint) {
                dvObjectsToReindexPermissionsFor.add(dataset);
                for (DataFile datafile : filesToReIndexPermissionsFor(dataset)) {
                    filesToReindexAsBatch.add(datafile);
                }
            }
        } else if (definitionPoint.isInstanceofDataset()) {
            dvObjectsToReindexPermissionsFor.add(definitionPoint);
            // index files
            Dataset dataset = (Dataset) definitionPoint;
            for (DataFile datafile : filesToReIndexPermissionsFor(dataset)) {
                filesToReindexAsBatch.add(datafile);
            }
        } else {
            dvObjectsToReindexPermissionsFor.add(definitionPoint);
        }

        /**
         * @todo Error handling? What to do with response?
         *
         * @todo Should update timestamps, probably, even thought these are
         * files, see https://github.com/IQSS/dataverse/issues/2421
         */
        String response = reindexFilesInBatches(filesToReindexAsBatch);

        for (DvObject dvObject : dvObjectsToReindexPermissionsFor) {
            /**
             * @todo do something with this response
             */
            IndexResponse indexResponse = indexPermissionsForOneDvObject(dvObject);
        }
        
        return new IndexResponse("Number of dvObject permissions indexed for " + definitionPoint
                + ": " + dvObjectsToReindexPermissionsFor.size()
        );
    }

    private String reindexFilesInBatches(List<DataFile> filesToReindexPermissionsFor) {
        List<SolrInputDocument> docs = new ArrayList<>();
        Map<Long, List<Long>> byParentId = new HashMap<>();
        Map<Long, List<String>> permStringByDatasetVersion = new HashMap<>();
        for (DataFile file : filesToReindexPermissionsFor) {
            Dataset dataset = (Dataset) file.getOwner();
            Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
            for (DatasetVersion datasetVersionFileIsAttachedTo : datasetVersionsToBuildCardsFor(dataset)) {
                boolean cardShouldExist = desiredCards.get(datasetVersionFileIsAttachedTo.getVersionState());
                if (cardShouldExist) {
                    List<String> cachedPermission = permStringByDatasetVersion.get(datasetVersionFileIsAttachedTo.getId());
                    if (cachedPermission == null) {
                        logger.fine("no cached permission! Looking it up...");
                        List<DvObjectSolrDoc> fileSolrDocs = constructDatafileSolrDocs((DataFile) file, permStringByDatasetVersion);
                        for (DvObjectSolrDoc fileSolrDoc : fileSolrDocs) {
                            Long datasetVersionId = fileSolrDoc.getDatasetVersionId();
                            if (datasetVersionId != null) {
                                permStringByDatasetVersion.put(datasetVersionId, fileSolrDoc.getPermissions());
                                SolrInputDocument solrDoc = SearchUtil.createSolrDoc(fileSolrDoc);
                                docs.add(solrDoc);
                            }
                        }
                    } else {
                        logger.fine("cached permission is " + cachedPermission);
                        List<DvObjectSolrDoc> fileSolrDocsBasedOnCachedPermissions = constructDatafileSolrDocs((DataFile) file, permStringByDatasetVersion);
                        for (DvObjectSolrDoc fileSolrDoc : fileSolrDocsBasedOnCachedPermissions) {
                            SolrInputDocument solrDoc = SearchUtil.createSolrDoc(fileSolrDoc);
                            docs.add(solrDoc);
                        }
                    }
                }
            }
            Long parent = file.getOwner().getId();
            List<Long> existingList = byParentId.get(parent);
            if (existingList == null) {
                List<Long> empty = new ArrayList<>();
                byParentId.put(parent, empty);
            } else {
                List<Long> updatedList = existingList;
                updatedList.add(file.getId());
                byParentId.put(parent, updatedList);
            }
        }
        try {
            persistToSolr(docs);
            return " " + filesToReindexPermissionsFor.size() + " files indexed across " + docs.size() + " Solr documents ";
        } catch (SolrServerException | IOException ex) {
            return " tried to reindex " + filesToReindexPermissionsFor.size() + " files indexed across " + docs.size() + " Solr documents but caught exception: " + ex;
        }
    }

    private List<DataFile> filesToReIndexPermissionsFor(Dataset dataset) {
        List<DataFile> filesToReindexPermissionsFor = new ArrayList<>();
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
            boolean cardShouldExist = desiredCards.get(version.getVersionState());
            if (cardShouldExist) {
                for (FileMetadata fileMetadata : version.getFileMetadatas()) {
                    filesToReindexPermissionsFor.add(fileMetadata.getDataFile());
                }
            }
        }
        return filesToReindexPermissionsFor;
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
        try {
            solrClientService.getSolrClient().commit();
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem committing deletion of the following documents from Solr: " + solrIdsToDelete);
        }
        return new IndexResponse("no known problem deleting the following documents from Solr:" + solrIdsToDelete);
    }

    public JsonObjectBuilder deleteAllFromSolrAndResetIndexTimes() throws SolrServerException, IOException {
        JsonObjectBuilder response = Json.createObjectBuilder();
        logger.info("attempting to delete all Solr documents before a complete re-index");
        solrClientService.getSolrClient().deleteByQuery("*:*");
        solrClientService.getSolrClient().commit();
        int numRowsAffected = dvObjectService.clearAllIndexTimes();
        response.add(numRowsClearedByClearAllIndexTimes, numRowsAffected);
        response.add(messageString, "Solr index and database index timestamps cleared.");
        return response;
    }

    /**
     * 
     *
     * @return A list of dvobject ids that should have their permissions
     * re-indexed because Solr was down when a permission was added. The permission
     * should be added to Solr. The id of the permission contains the type of
     * DvObject and the primary key of the dvObject.
     * DvObjects of type DataFile are currently skipped because their index
     * time isn't stored in the database, since they are indexed along 
     * with their parent dataset (this may change).
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

  
}
