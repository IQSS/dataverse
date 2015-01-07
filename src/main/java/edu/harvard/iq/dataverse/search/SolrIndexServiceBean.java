package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

@Named
@Stateless
public class SolrIndexServiceBean {

    private static final Logger logger = Logger.getLogger(SolrIndexServiceBean.class.getCanonicalName());

    @EJB
    SystemConfig systemConfig;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SearchPermissionsServiceBean searchPermissionsService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DataverseRoleServiceBean rolesSvc;

    public List<DvObjectSolrDoc> determineSolrDocs(Long dvObjectId) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
        if (dvObject == null) {
            return emptyList;
        }
        if (dvObject.isInstanceofDataverse()) {
            DvObjectSolrDoc dataverseSolrDoc = constructDataverseSolrDoc((Dataverse) dvObject);
            solrDocs.add(dataverseSolrDoc);
        } else if (dvObject.isInstanceofDataset()) {
            List<DvObjectSolrDoc> datasetSolrDocs = constructDatasetSolrDocs((Dataset) dvObject);
            solrDocs.addAll(datasetSolrDocs);
        } else if (dvObject.isInstanceofDataFile()) {
            List<DvObjectSolrDoc> fileSolrDocs = constructDatafileSolrDocs((DataFile) dvObject);
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
        List<String> perms = searchPermissionsService.findDataversePerms(dataverse);
        DvObjectSolrDoc dvDoc = new DvObjectSolrDoc(dataverse.getId().toString(), IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId(), dataverse.getName(), perms);
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

    /**
     * @todo Try to make this method faster. It should be fine if you need to
     * figure out the permission documents for a single datafile but will be
     * slow if you call it over and over in a loop.
     */
    private List<DvObjectSolrDoc> constructDatafileSolrDocs(DataFile dataFile) {
        List<DvObjectSolrDoc> datafileSolrDocs = new ArrayList<>();
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataFile.getOwner());
        for (DatasetVersion datasetVersionFileIsAttachedTo : datasetVersionsToBuildCardsFor(dataFile.getOwner())) {
            boolean cardShouldExist = desiredCards.get(datasetVersionFileIsAttachedTo.getVersionState());
            if (cardShouldExist) {
                String solrIdStart = IndexServiceBean.solrDocIdentifierFile + dataFile.getId();
                String solrIdEnd = getDatasetOrDataFileSolrEnding(datasetVersionFileIsAttachedTo.getVersionState());
                String solrId = solrIdStart + solrIdEnd;
                List<String> perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
                DvObjectSolrDoc dataFileSolrDoc = new DvObjectSolrDoc(dataFile.getId().toString(), solrId, dataFile.getDisplayName(), perms);
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
                List<String> perms = searchPermissionsService.findDatasetVersionPerms(datasetVersionFileIsAttachedTo);
                for (FileMetadata fileMetadata : datasetVersionFileIsAttachedTo.getFileMetadatas()) {
                    Long fileId = fileMetadata.getDataFile().getId();
                    String solrIdStart = IndexServiceBean.solrDocIdentifierFile + fileId;
                    String solrIdEnd = getDatasetOrDataFileSolrEnding(datasetVersionFileIsAttachedTo.getVersionState());
                    String solrId = solrIdStart + solrIdEnd;
                    DvObjectSolrDoc dataFileSolrDoc = new DvObjectSolrDoc(fileId.toString(), solrId, fileMetadata.getLabel(), perms);
                    logger.fine("adding fileid " + fileId);
                    datafileSolrDocs.add(dataFileSolrDoc);
                }
            }
        }
        return datafileSolrDocs;
    }

    private List<DatasetVersion> datasetVersionsToBuildCardsFor(Dataset dataset) {
        List<DatasetVersion> datasetVersions = new ArrayList<>();
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
        List<String> perms = searchPermissionsService.findDatasetVersionPerms(version);
        return new DvObjectSolrDoc(version.getDataset().getId().toString(), solrId, name, perms);
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
                definitionPoints.addAll(determineSolrDocs(dvObject.getId()));
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
            SolrInputDocument solrInputDocument = createSolrDoc(dvObjectSolrDoc);
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

    public IndexResponse indexPermissionsForOneDvObject(long dvObjectId) {
        Collection<SolrInputDocument> docs = new ArrayList<>();

        List<DvObjectSolrDoc> definitionPoints = determineSolrDocs(dvObjectId);

        for (DvObjectSolrDoc dvObjectSolrDoc : definitionPoints) {
            SolrInputDocument solrInputDocument = createSolrDoc(dvObjectSolrDoc);
            docs.add(solrInputDocument);
        }
        try {
            persistToSolr(docs);
            DvObject savedDvObject = dvObjectService.updatePermissionIndexTime(dvObjectService.findDvObject(dvObjectId));
            boolean updatePermissionTimeSuccessful = false;
            if (savedDvObject != null) {
                updatePermissionTimeSuccessful = true;
            }
            return new IndexResponse("attempted to index permissions for DvObject " + dvObjectId + " and updatePermissionTimeSuccessful was " + updatePermissionTimeSuccessful);
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    private SolrInputDocument createSolrDoc(DvObjectSolrDoc dvObjectSolrDoc) {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, dvObjectSolrDoc.getSolrId() + IndexServiceBean.discoverabilityPermissionSuffix);
        solrInputDocument.addField(SearchFields.DEFINITION_POINT, dvObjectSolrDoc.getSolrId());
        solrInputDocument.addField(SearchFields.DEFINITION_POINT_DVOBJECT_ID, dvObjectSolrDoc.getDvObjectId());
        solrInputDocument.addField(SearchFields.DISCOVERABLE_BY, dvObjectSolrDoc.getPermissions());
        return solrInputDocument;
    }

    private void persistToSolr(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        if (docs.isEmpty()) {
            /**
             * @todo Throw an exception here? "DvObject id 9999 does not exist."
             */
            logger.info("nothing to persist");
            return;
        }
        logger.fine("persisting to Solr...");
        SolrServer solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
        /**
         * @todo Do something with these responses from Solr.
         */
        UpdateResponse addResponse = solrServer.add(docs);
        UpdateResponse commitResponse = solrServer.commit();
    }

    public IndexResponse indexPermissionsOnSelfAndChildren(DvObject definitionPoint) {
        List<Long> dvObjectsToReindexPermissionsFor = new ArrayList<>();
        /**
         * @todo Re-indexing the definition point itself seems to be necessary
         * for revoke but not necessarily grant.
         */
        dvObjectsToReindexPermissionsFor.add(definitionPoint.getId());
        SolrServer solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*");
        solrQuery.setRows(Integer.SIZE);
        if (definitionPoint.isInstanceofDataverse()) {
            Dataverse dataverse = (Dataverse) definitionPoint;

            String dataversePath = dataverseService.determineDataversePath(dataverse);
            String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";

            solrQuery.addFilterQuery(filterDownToSubtree);
        } else if (definitionPoint.isInstanceofDataset()) {
            // index the dataset itself
            indexPermissionsForOneDvObject(definitionPoint.getId());
            // index files
            /**
             * @todo make this faster, index files in batches
             */
            Dataset dataset = (Dataset) definitionPoint;
            Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
            for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
                boolean cardShouldExist = desiredCards.get(version.getVersionState());
                if (cardShouldExist) {
                    for (FileMetadata fileMetadata : version.getFileMetadatas()) {
                        dvObjectsToReindexPermissionsFor.add(fileMetadata.getDataFile().getId());
                    }
                }
            }

        }

        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServer.query(solrQuery);
            if (queryResponse != null) {
                for (SolrDocument solrDoc : queryResponse.getResults()) {
                    Object entityIdObject = solrDoc.getFieldValue(SearchFields.ENTITY_ID);
                    if (entityIdObject != null) {
                        dvObjectsToReindexPermissionsFor.add((Long) entityIdObject);
                    }
                }
            }

        } catch (SolrServerException | HttpSolrServer.RemoteSolrException ex) {
            return new IndexResponse("Unable to execute indexPermissionsOnSelfAndChildren on " + definitionPoint.getId() + ":" + definitionPoint.getDisplayName() + ". Exception: " + ex.toString());
        }

        List<String> updatePermissionTimeSuccessStatus = new ArrayList<>();
        for (Long dvObjectId : dvObjectsToReindexPermissionsFor) {
            /**
             * @todo do something with this response
             */
            IndexResponse indexResponse = indexPermissionsForOneDvObject(dvObjectId);
            DvObject managedDefinitionPoint = dvObjectService.updatePermissionIndexTime(definitionPoint);
            boolean updatePermissionTimeSuccessful = false;
            if (managedDefinitionPoint != null) {
                updatePermissionTimeSuccessful = true;
            }
            updatePermissionTimeSuccessStatus.add(dvObjectId + ":" + updatePermissionTimeSuccessful);
        }
        return new IndexResponse("Number of dvObject permissions indexed for " + definitionPoint
                + " (updatePermissionTimeSuccessful:" + updatePermissionTimeSuccessStatus
                + "): " + dvObjectsToReindexPermissionsFor.size()
        );
    }

    public IndexResponse deleteMultipleSolrIds(List<String> solrIdsToDelete) {
        if (solrIdsToDelete.isEmpty()) {
            return new IndexResponse("nothing to delete");
        }
        SolrServer solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
        try {
            solrServer.deleteById(solrIdsToDelete);
        } catch (SolrServerException | IOException ex) {
            /**
             * @todo mark these for re-deletion
             */
            return new IndexResponse("problem deleting the following documents from Solr: " + solrIdsToDelete);
        }
        return new IndexResponse("no known problem deleting the following documents from Solr:" + solrIdsToDelete);
    }

    /**
     * @return A list of dvobject ids that should have their permissions
     * re-indexed Solr was down when a permission was added. The permission
     * should be added to Solr.
     */
    public List<Long> findPermissionsMissingFromSolr() throws Exception {
        List<Long> indexingRequired = new ArrayList<>();
        for (DvObject dvObject : dvObjectService.findAll()) {
//            logger.info("examining dvObjectId " + dvObject.getId() + "...");
            Timestamp permissionModificationTime = dvObject.getPermissionModificationTime();
            Timestamp permissionIndexTime = dvObject.getPermissionIndexTime();
            if (permissionIndexTime == null) {
                indexingRequired.add(dvObject.getId());
            } else {
                if (permissionModificationTime == null) {
                    /**
                     * @todo What should we do here? Permissions should always
                     * be there. They are assigned at create time. For now,
                     * we'll throw an exception. Set this to true and figure it
                     * out!
                     */
                    if (false) {
                        throw new Exception("Problem finding missing Solr permissions. No permission modification time for dvObject id " + dvObject.getId());
                    }
                } else {
                    if (permissionIndexTime.before(permissionModificationTime)) {
                        indexingRequired.add(dvObject.getId());
                    }
                }
            }
        }
        return indexingRequired;
    }

    /**
     * @return A list of dvobject ids that should have their permissions
     * re-indexed because Solr was down when a permission was revoked. The
     * permission should be removed from Solr.
     */
    public List<Long> findPermissionsInSolrNoLongerInDatabase() {
        /**
         * @todo Implement this!
         */
        return new ArrayList<>();
    }
}
