package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.AsyncResult;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

@Stateless
@Named
public class IndexServiceBean {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());

    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    BuiltinUserServiceBean dataverseUserServiceBean;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    AuthenticationServiceBean userServiceBean;
    @EJB
    SystemConfig systemConfig;
    @EJB
    SearchPermissionsServiceBean searchPermissionsService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DatasetLinkingServiceBean dsLinkingService;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;

    public static final String solrDocIdentifierDataverse = "dataverse_";
    public static final String solrDocIdentifierFile = "datafile_";
    public static final String solrDocIdentifierDataset = "dataset_";
    public static final String draftSuffix = "_draft";
    public static final String deaccessionedSuffix = "_deaccessioned";
    public static final String discoverabilityPermissionSuffix = "_permission";
    private static final String groupPrefix = "group_";
    private static final String groupPerUserPrefix = "group_user";
    private static final String publicGroupIdString = "public";
    private static final String publicGroupString = groupPrefix + "public";
    public static final String PUBLISHED_STRING = "Published";
    private static final String UNPUBLISHED_STRING = "Unpublished";
    private static final String DRAFT_STRING = "Draft";
    private static final String IN_REVIEW_STRING = "In Review";
    private static final String DEACCESSIONED_STRING = "Deaccessioned";
    public static final String HARVESTED = "Harvested";
    private String rootDataverseName;
    private Dataverse rootDataverseCached; 
    private SolrServer solrServer;
    
    @PostConstruct
    public void init(){
        solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
        rootDataverseName = findRootDataverseCached().getName() + " " + BundleUtil.getStringFromBundle("dataverse");
    }
    
    @PreDestroy
    public void close(){
        if(solrServer != null){
            solrServer.shutdown();
            solrServer = null;
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Future<String> indexDataverseInNewTransaction(Dataverse dataverse) {
        return indexDataverse(dataverse);
    }

    public Future<String> indexDataverse(Dataverse dataverse) {
        logger.fine("indexDataverse called on dataverse id " + dataverse.getId() + "(" + dataverse.getAlias() + ")");
        if (dataverse.getId() == null) {
            String msg = "unable to index dataverse. id was null (alias: " + dataverse.getAlias() + ")";
            logger.info(msg);
            return new AsyncResult<>(msg);
        }
        Dataverse rootDataverse = findRootDataverseCached();
        if (rootDataverse == null) {
            String msg = "Could not find root dataverse and the root dataverse should not be indexed. Returning.";
            return new AsyncResult<>(msg);
        } else if (dataverse.getId() == rootDataverse.getId()) {
            String msg = "The root dataverse should not be indexed. Returning.";
            return new AsyncResult<>(msg);
        }
        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, solrDocIdentifierDataverse + dataverse.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataverse.getId());
        solrInputDocument.addField(SearchFields.DATAVERSE_VERSION_INDEXED_BY, systemConfig.getVersion());
        solrInputDocument.addField(SearchFields.IDENTIFIER, dataverse.getAlias());
        solrInputDocument.addField(SearchFields.TYPE, "dataverses");
        solrInputDocument.addField(SearchFields.NAME, dataverse.getName());
        solrInputDocument.addField(SearchFields.NAME_SORT, dataverse.getName());
        solrInputDocument.addField(SearchFields.DATAVERSE_NAME, dataverse.getName());
        solrInputDocument.addField(SearchFields.DATAVERSE_CATEGORY, dataverse.getIndexableCategoryName());
        if (dataverse.isReleased()) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getPublicationDate());
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT, convertToFriendlyDate(dataverse.getPublicationDate()));
        } else {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getCreateDate());
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT, convertToFriendlyDate(dataverse.getCreateDate()));
        }
        /* We don't really have harvested dataverses yet; 
           (I have in fact just removed the isHarvested() method from the Dataverse object) -- L.A.
        if (dataverse.isHarvested()) {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, true);
            solrInputDocument.addField(SearchFields.SOURCE, HARVESTED);
        } else { (this means that all dataverses are "local" - should this be removed? */
            solrInputDocument.addField(SearchFields.IS_HARVESTED, false);
            solrInputDocument.addField(SearchFields.METADATA_SOURCE, findRootDataverseCached().getName() + " " + BundleUtil.getStringFromBundle("dataverse")); //rootDataverseName);
        /*}*/

        addDataverseReleaseDateToSolrDoc(solrInputDocument, dataverse);
//        if (dataverse.getOwner() != null) {
//            solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataverse.getOwner().getName());
//        }
        solrInputDocument.addField(SearchFields.DESCRIPTION, StringUtil.html2text(dataverse.getDescription()));
        solrInputDocument.addField(SearchFields.DATAVERSE_DESCRIPTION, StringUtil.html2text(dataverse.getDescription()));
//        logger.info("dataverse affiliation: " + dataverse.getAffiliation());
        if (dataverse.getAffiliation() != null && !dataverse.getAffiliation().isEmpty()) {
            /**
             * @todo: stop using affiliation as category
             */
//            solrInputDocument.addField(SearchFields.CATEGORY, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.AFFILIATION, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.DATAVERSE_AFFILIATION, dataverse.getAffiliation());
        }
        for (ControlledVocabularyValue dataverseSubject : dataverse.getDataverseSubjects()) {
            String subject = dataverseSubject.getStrValue();
            if (!subject.equals(DatasetField.NA_VALUE)) {
                solrInputDocument.addField(SearchFields.DATAVERSE_SUBJECT, subject);
                // collapse into shared "subject" field used as a facet
                solrInputDocument.addField(SearchFields.SUBJECT, subject);
            }
        }
        // checking for NPE is important so we can create the root dataverse
        if (rootDataverse != null && !dataverse.equals(rootDataverse)) {
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                solrInputDocument.addField(SearchFields.PARENT_ID, dataverse.getOwner().getId());
                solrInputDocument.addField(SearchFields.PARENT_NAME, dataverse.getOwner().getName());
            }
        }
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = findPathSegments(dataverse, dataversePathSegmentsAccumulator);
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        if (dataversePaths.size() > 0) {
            // don't show yourself while indexing or in search results: https://redmine.hmdc.harvard.edu/issues/3613
//            logger.info(dataverse.getName() + " size " + dataversePaths.size());
            dataversePaths.remove(dataversePaths.size() - 1);
        }
        //Add paths for linking dataverses
        for (Dataverse linkingDataverse : dvLinkingService.findLinkingDataverses(dataverse.getId())) {
            List<String> linkingDataversePathSegmentsAccumulator = new ArrayList<>();
            List<String> linkingdataverseSegments = findPathSegments(linkingDataverse, linkingDataversePathSegmentsAccumulator);
            List<String> linkingDataversePaths = getDataversePathsFromSegments(linkingdataverseSegments);
            for (String dvPath : linkingDataversePaths) {
                dataversePaths.add(dvPath);
            }
        }
        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
        docs.add(solrInputDocument);

        String status;
        try {
            if (dataverse.getId() != null) {
                solrServer.add(docs);
            } else {
                logger.info("WARNING: indexing of a dataverse with no id attempted");
            }
        } catch (SolrServerException | IOException ex) {
            status = ex.toString();
            logger.info(status);
            return new AsyncResult<>(status);
        }
        try {
            solrServer.commit();
        } catch (SolrServerException | IOException ex) {
            status = ex.toString();
            logger.info(status);
            return new AsyncResult<>(status);
        }

        dvObjectService.updateContentIndexTime(dataverse);
        IndexResponse indexResponse = solrIndexService.indexPermissionsForOneDvObject(dataverse);
        String msg = "indexed dataverse " + dataverse.getId() + ":" + dataverse.getAlias() + ". Response from permission indexing: " + indexResponse.getMessage();
        return new AsyncResult<>(msg);

    }

    @TransactionAttribute(REQUIRES_NEW)
    public Future<String> indexDatasetInNewTransaction(Dataset dataset) {
        boolean doNormalSolrDocCleanUp = false;
        return indexDataset(dataset, doNormalSolrDocCleanUp);
    }

    public Future<String> indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
        logger.fine("indexing dataset " + dataset.getId());
        /**
         * @todo should we use solrDocIdentifierDataset or
         * IndexableObject.IndexableTypes.DATASET.getName() + "_" ?
         */
//        String solrIdPublished = solrDocIdentifierDataset + dataset.getId();
        String solrIdPublished = determinePublishedDatasetSolrDocId(dataset);
        String solrIdDraftDataset = IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.WORKING_COPY.getSuffix();
//        String solrIdDeaccessioned = IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.DEACCESSIONED.getSuffix();
        String solrIdDeaccessioned = determineDeaccessionedDatasetId(dataset);
        StringBuilder debug = new StringBuilder();
        debug.append("\ndebug:\n");
        int numPublishedVersions = 0;
        List<DatasetVersion> versions = dataset.getVersions();
        List<String> solrIdsOfFilesToDelete = new ArrayList<>();
        for (DatasetVersion datasetVersion : versions) {
            Long versionDatabaseId = datasetVersion.getId();
            String versionTitle = datasetVersion.getTitle();
            String semanticVersion = datasetVersion.getSemanticVersion();
            DatasetVersion.VersionState versionState = datasetVersion.getVersionState();
            if (versionState.equals(DatasetVersion.VersionState.RELEASED)) {
                numPublishedVersions += 1;
            }
            debug.append("version found with database id " + versionDatabaseId + "\n");
            debug.append("- title: " + versionTitle + "\n");
            debug.append("- semanticVersion-VersionState: " + semanticVersion + "-" + versionState + "\n");
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            List<String> fileInfo = new ArrayList<>();
            for (FileMetadata fileMetadata : fileMetadatas) {
                String solrIdOfPublishedFile = solrDocIdentifierFile + fileMetadata.getDataFile().getId();
                /**
                 * It sounds weird but the first thing we'll do is preemptively
                 * delete the Solr documents of all published files. Don't
                 * worry, published files will be re-indexed later along with
                 * the dataset. We do this so users can delete files from
                 * published versions of datasets and then re-publish a new
                 * version without fear that their old published files (now
                 * deleted from the latest published version) will be
                 * searchable. See also
                 * https://github.com/IQSS/dataverse/issues/762
                 */
                solrIdsOfFilesToDelete.add(solrIdOfPublishedFile);
                fileInfo.add(fileMetadata.getDataFile().getId() + ":" + fileMetadata.getLabel());
            }
            try {
                /**
                 * Preemptively delete *all* Solr documents for files associated
                 * with the dataset based on a Solr query.
                 *
                 * We must query Solr for this information because the file has
                 * been deleted from the database ( perhaps when Solr was down,
                 * as reported in https://github.com/IQSS/dataverse/issues/2086
                 * ) so the database doesn't even know about the file. It's an
                 * orphan.
                 *
                 * @todo This Solr query should make the iteration above based
                 * on the database unnecessary because it the Solr query should
                 * find all files for the dataset. We can probably remove the
                 * iteration above after an "index all" has been performed.
                 * Without an "index all" we won't be able to find files based
                 * on parentId because that field wasn't searchable in 4.0.
                 *
                 * @todo We should also delete the corresponding Solr
                 * "permission" documents for the files.
                 */
                List<String> allFilesForDataset = findFilesOfParentDataset(dataset.getId());
                solrIdsOfFilesToDelete.addAll(allFilesForDataset);
            } catch (SearchException | NullPointerException ex) {
                logger.fine("could not run search of files to delete: " + ex);
            }
            int numFiles = 0;
            if (fileMetadatas != null) {
                numFiles = fileMetadatas.size();
            }
            debug.append("- files: " + numFiles + " " + fileInfo.toString() + "\n");
        }
        debug.append("numPublishedVersions: " + numPublishedVersions + "\n");
        if (doNormalSolrDocCleanUp) {
            IndexResponse resultOfAttemptToPremptivelyDeletePublishedFiles = solrIndexService.deleteMultipleSolrIds(solrIdsOfFilesToDelete);
            debug.append("result of attempt to premptively deleted published files before reindexing: " + resultOfAttemptToPremptivelyDeletePublishedFiles + "\n");
        }
        DatasetVersion latestVersion = dataset.getLatestVersion();
        String latestVersionStateString = latestVersion.getVersionState().name();
        DatasetVersion.VersionState latestVersionState = latestVersion.getVersionState();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        boolean atLeastOnePublishedVersion = false;
        if (releasedVersion != null) {
            atLeastOnePublishedVersion = true;
        } else {
            atLeastOnePublishedVersion = false;
        }
        Map<DatasetVersion.VersionState, Boolean> desiredCards = new LinkedHashMap<>();
        /**
         * @todo refactor all of this below and have a single method that takes
         * the map of desired cards (which correspond to Solr documents) as one
         * of the arguments and does all the operations necessary to achieve the
         * desired state.
         */
        StringBuilder results = new StringBuilder();
        if (atLeastOnePublishedVersion == false) {
            results.append("No published version, nothing will be indexed as ")
                    .append(solrIdPublished).append("\n");
            if (latestVersionState.equals(DatasetVersion.VersionState.DRAFT)) {

                desiredCards.put(DatasetVersion.VersionState.DRAFT, true);
                IndexableDataset indexableDraftVersion = new IndexableDataset(latestVersion);
                String indexDraftResult = addOrUpdateDataset(indexableDraftVersion);
                results.append("The latest version is a working copy (latestVersionState: ")
                        .append(latestVersionStateString).append(") and indexing was attempted for ")
                        .append(solrIdDraftDataset).append(" (limited discoverability). Result: ")
                        .append(indexDraftResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                if (doNormalSolrDocCleanUp) {
                    String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                    results.append("Draft exists, no need for deaccessioned version. Deletion attempted for ")
                            .append(solrIdDeaccessioned).append(" (and files). Result: ")
                            .append(deleteDeaccessionedResult).append("\n");
                }

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                if (doNormalSolrDocCleanUp) {
                    String deletePublishedResults = removePublished(dataset);
                    results.append("No published version. Attempting to delete traces of published version from index. Result: ").
                            append(deletePublishedResults).append("\n");
                }

                /**
                 * Desired state for existence of cards: {DRAFT=true,
                 * DEACCESSIONED=false, RELEASED=false}
                 *
                 * No published version, nothing will be indexed as dataset_17
                 *
                 * The latest version is a working copy (latestVersionState:
                 * DRAFT) and indexing was attempted for dataset_17_draft
                 * (limited discoverability). Result: indexed dataset 17 as
                 * dataset_17_draft. filesIndexed: [datafile_18_draft]
                 *
                 * Draft exists, no need for deaccessioned version. Deletion
                 * attempted for dataset_17_deaccessioned (and files). Result:
                 * Attempted to delete dataset_17_deaccessioned from Solr index.
                 * updateReponse was:
                 * {responseHeader={status=0,QTime=1}}Attempted to delete
                 * datafile_18_deaccessioned from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=1}}
                 *
                 * No published version. Attempting to delete traces of
                 * published version from index. Result: Attempted to delete
                 * dataset_17 from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=1}}Attempted to delete
                 * datafile_18 from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=0}}
                 */
                String result = getDesiredCardState(desiredCards) + results.toString() + debug.toString();
                logger.fine(result);
                indexDatasetPermissions(dataset);
                return new AsyncResult<>(result);
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, true);
                IndexableDataset indexableDeaccessionedVersion = new IndexableDataset(latestVersion);
                String indexDeaccessionedVersionResult = addOrUpdateDataset(indexableDeaccessionedVersion);
                results.append("No draft version. Attempting to index as deaccessioned. Result: ").append(indexDeaccessionedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                if (doNormalSolrDocCleanUp) {
                    String deletePublishedResults = removePublished(dataset);
                    results.append("No published version. Attempting to delete traces of published version from index. Result: ").
                            append(deletePublishedResults).append("\n");
                }

                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
                if (doNormalSolrDocCleanUp) {
                    List<String> solrDocIdsForDraftFilesToDelete = findSolrDocIdsForDraftFilesToDelete(dataset);
                    String deleteDraftDatasetVersionResult = removeSolrDocFromIndex(solrIdDraftDataset);
                    String deleteDraftFilesResults = deleteDraftFiles(solrDocIdsForDraftFilesToDelete);
                    results.append("Attempting to delete traces of drafts. Result: ")
                            .append(deleteDraftDatasetVersionResult).append(deleteDraftFilesResults).append("\n");
                }

                /**
                 * Desired state for existence of cards: {DEACCESSIONED=true,
                 * RELEASED=false, DRAFT=false}
                 *
                 * No published version, nothing will be indexed as dataset_17
                 *
                 * No draft version. Attempting to index as deaccessioned.
                 * Result: indexed dataset 17 as dataset_17_deaccessioned.
                 * filesIndexed: []
                 *
                 * No published version. Attempting to delete traces of
                 * published version from index. Result: Attempted to delete
                 * dataset_17 from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=0}}Attempted to delete
                 * datafile_18 from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=3}}
                 *
                 * Attempting to delete traces of drafts. Result: Attempted to
                 * delete dataset_17_draft from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=1}}
                 */
                String result = getDesiredCardState(desiredCards) + results.toString() + debug.toString();
                logger.fine(result);
                indexDatasetPermissions(dataset);
                return new AsyncResult<>(result);
            } else {
                String result = "No-op. Unexpected condition reached: No released version and latest version is neither draft nor deaccessioned";
                logger.fine(result);
                return new AsyncResult<>(result);
            }
        } else if (atLeastOnePublishedVersion == true) {
            results.append("Published versions found. ")
                    .append("Will attempt to index as ").append(solrIdPublished).append(" (discoverable by anonymous)\n");
            if (latestVersionState.equals(DatasetVersion.VersionState.RELEASED)
                    || latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {

                desiredCards.put(DatasetVersion.VersionState.RELEASED, true);
                IndexableDataset indexableReleasedVersion = new IndexableDataset(releasedVersion);
                String indexReleasedVersionResult = addOrUpdateDataset(indexableReleasedVersion);
                results.append("Attempted to index " + solrIdPublished).append(". Result: ").append(indexReleasedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
                if (doNormalSolrDocCleanUp) {
                    List<String> solrDocIdsForDraftFilesToDelete = findSolrDocIdsForDraftFilesToDelete(dataset);
                    String deleteDraftDatasetVersionResult = removeSolrDocFromIndex(solrIdDraftDataset);
                    String deleteDraftFilesResults = deleteDraftFiles(solrDocIdsForDraftFilesToDelete);
                    results.append("The latest version is published. Attempting to delete drafts. Result: ")
                            .append(deleteDraftDatasetVersionResult).append(deleteDraftFilesResults).append("\n");
                }

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                if (doNormalSolrDocCleanUp) {
                    String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                    results.append("No need for deaccessioned version. Deletion attempted for ")
                            .append(solrIdDeaccessioned).append(". Result: ").append(deleteDeaccessionedResult);
                }

                /**
                 * Desired state for existence of cards: {RELEASED=true,
                 * DRAFT=false, DEACCESSIONED=false}
                 *
                 * Released versions found: 1. Will attempt to index as
                 * dataset_17 (discoverable by anonymous)
                 *
                 * Attempted to index dataset_17. Result: indexed dataset 17 as
                 * dataset_17. filesIndexed: [datafile_18]
                 *
                 * The latest version is published. Attempting to delete drafts.
                 * Result: Attempted to delete dataset_17_draft from Solr index.
                 * updateReponse was: {responseHeader={status=0,QTime=1}}
                 *
                 * No need for deaccessioned version. Deletion attempted for
                 * dataset_17_deaccessioned. Result: Attempted to delete
                 * dataset_17_deaccessioned from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=1}}Attempted to delete
                 * datafile_18_deaccessioned from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=0}}
                 */
                String result = getDesiredCardState(desiredCards) + results.toString() + debug.toString();
                logger.fine(result);
                indexDatasetPermissions(dataset);
                return new AsyncResult<>(result);
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DRAFT)) {

                IndexableDataset indexableDraftVersion = new IndexableDataset(latestVersion);
                desiredCards.put(DatasetVersion.VersionState.DRAFT, true);
                String indexDraftResult = addOrUpdateDataset(indexableDraftVersion);
                results.append("The latest version is a working copy (latestVersionState: ")
                        .append(latestVersionStateString).append(") and will be indexed as ")
                        .append(solrIdDraftDataset).append(" (limited visibility). Result: ").append(indexDraftResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.RELEASED, true);
                IndexableDataset indexableReleasedVersion = new IndexableDataset(releasedVersion);
                String indexReleasedVersionResult = addOrUpdateDataset(indexableReleasedVersion);
                results.append("There is a published version we will attempt to index. Result: ").append(indexReleasedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                if (doNormalSolrDocCleanUp) {
                    String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                    results.append("No need for deaccessioned version. Deletion attempted for ")
                            .append(solrIdDeaccessioned).append(". Result: ").append(deleteDeaccessionedResult);
                }

                /**
                 * Desired state for existence of cards: {DRAFT=true,
                 * RELEASED=true, DEACCESSIONED=false}
                 *
                 * Released versions found: 1. Will attempt to index as
                 * dataset_17 (discoverable by anonymous)
                 *
                 * The latest version is a working copy (latestVersionState:
                 * DRAFT) and will be indexed as dataset_17_draft (limited
                 * visibility). Result: indexed dataset 17 as dataset_17_draft.
                 * filesIndexed: [datafile_18_draft]
                 *
                 * There is a published version we will attempt to index.
                 * Result: indexed dataset 17 as dataset_17. filesIndexed:
                 * [datafile_18]
                 *
                 * No need for deaccessioned version. Deletion attempted for
                 * dataset_17_deaccessioned. Result: Attempted to delete
                 * dataset_17_deaccessioned from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=1}}Attempted to delete
                 * datafile_18_deaccessioned from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=0}}
                 */
                String result = getDesiredCardState(desiredCards) + results.toString() + debug.toString();
                logger.fine(result);
                indexDatasetPermissions(dataset);
                return new AsyncResult<>(result);
            } else {
                String result = "No-op. Unexpected condition reached: There is at least one published version but the latest version is neither published nor draft";
                logger.fine(result);
                return new AsyncResult<>(result);
            }
        } else {
            String result = "No-op. Unexpected condition reached: Has a version been published or not?";
            logger.fine(result);
            return new AsyncResult<>(result);
        }
    }

    private String deleteDraftFiles(List<String> solrDocIdsForDraftFilesToDelete) {
        String deleteDraftFilesResults = "";
        IndexResponse indexResponse = solrIndexService.deleteMultipleSolrIds(solrDocIdsForDraftFilesToDelete);
        deleteDraftFilesResults = indexResponse.toString();
        return deleteDraftFilesResults;
    }

    private IndexResponse indexDatasetPermissions(Dataset dataset) {
        boolean disabledForDebugging = false;
        if (disabledForDebugging) {
            /**
             * Performance problems indexing permissions in
             * https://github.com/IQSS/dataverse/issues/50 and
             * https://github.com/IQSS/dataverse/issues/2036
             */
            return new IndexResponse("permissions indexing disabled for debugging");
        }
        IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(dataset);
        return indexResponse;
    }

    private String addOrUpdateDataset(IndexableDataset indexableDataset) {
        IndexableDataset.DatasetState state = indexableDataset.getDatasetState();
        Dataset dataset = indexableDataset.getDatasetVersion().getDataset();
        logger.fine("adding or updating Solr document for dataset id " + dataset.getId());
        Collection<SolrInputDocument> docs = new ArrayList<>();
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = new ArrayList<>();
        try {
            dataverseSegments = findPathSegments(dataset.getOwner(), dataversePathSegmentsAccumulator);
        } catch (Exception ex) {
            logger.info("failed to find dataverseSegments for dataversePaths for " + SearchFields.SUBTREE + ": " + ex);
        }
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        //Add Paths for linking dataverses
        for (Dataverse linkingDataverse : dsLinkingService.findLinkingDataverses(dataset.getId())) {
            List<String> linkingDataversePathSegmentsAccumulator = new ArrayList<>();
            List<String> linkingdataverseSegments = findPathSegments(linkingDataverse, linkingDataversePathSegmentsAccumulator);
            List<String> linkingDataversePaths = getDataversePathsFromSegments(linkingdataverseSegments);
            for (String dvPath : linkingDataversePaths) {
                dataversePaths.add(dvPath);
            }
        }
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        String datasetSolrDocId = indexableDataset.getSolrDocId();
        solrInputDocument.addField(SearchFields.ID, datasetSolrDocId);
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataset.getId());
        String dataverseVersion = systemConfig.getVersion();
        solrInputDocument.addField(SearchFields.DATAVERSE_VERSION_INDEXED_BY, dataverseVersion);
        solrInputDocument.addField(SearchFields.IDENTIFIER, dataset.getGlobalId());
        solrInputDocument.addField(SearchFields.DATASET_PERSISTENT_ID, dataset.getGlobalId());
        solrInputDocument.addField(SearchFields.PERSISTENT_URL, dataset.getPersistentURL());
        solrInputDocument.addField(SearchFields.TYPE, "datasets");

        Date datasetSortByDate = new Date();
        Date majorVersionReleaseDate = dataset.getMostRecentMajorVersionReleaseDate();
        if (majorVersionReleaseDate != null) {
            if (true) {
                String msg = "major release date found: " + majorVersionReleaseDate.toString();
                logger.fine(msg);
            }
            datasetSortByDate = majorVersionReleaseDate;
        } else {
            if (indexableDataset.getDatasetState().equals(IndexableDataset.DatasetState.WORKING_COPY)) {
                solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
            } else if (indexableDataset.getDatasetState().equals(IndexableDataset.DatasetState.DEACCESSIONED)) {
                solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DEACCESSIONED_STRING);
            }
            Date createDate = dataset.getCreateDate();
            if (createDate != null) {
                if (true) {
                    String msg = "can't find major release date, using create date: " + createDate;
                    logger.fine(msg);
                }
                datasetSortByDate = createDate;
            } else {
                String msg = "can't find major release date or create date, using \"now\"";
                logger.info(msg);
                datasetSortByDate = new Date();
            }
        }
        solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, datasetSortByDate);
        solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT, convertToFriendlyDate(datasetSortByDate));

        if (state.equals(indexableDataset.getDatasetState().PUBLISHED)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
//            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataset.getPublicationDate());
        } else if (state.equals(indexableDataset.getDatasetState().WORKING_COPY)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DRAFT_STRING);
        }

        addDatasetReleaseDateToSolrDoc(solrInputDocument, dataset);

        if (dataset.isHarvested()) {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, true);
            solrInputDocument.addField(SearchFields.METADATA_SOURCE, HARVESTED);
        } else {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, false);
            solrInputDocument.addField(SearchFields.METADATA_SOURCE, findRootDataverseCached().getName() + " " + BundleUtil.getStringFromBundle("dataverse")); //rootDataverseName);
        }

        DatasetVersion datasetVersion = indexableDataset.getDatasetVersion();
        String parentDatasetTitle = "TBD";
        if (datasetVersion != null) {

            solrInputDocument.addField(SearchFields.DATASET_VERSION_ID, datasetVersion.getId());
            solrInputDocument.addField(SearchFields.DATASET_CITATION, datasetVersion.getCitation(false));
            solrInputDocument.addField(SearchFields.DATASET_CITATION_HTML, datasetVersion.getCitation(true));

            if (datasetVersion.isInReview()) {
                solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, IN_REVIEW_STRING);
            }

            for (DatasetField dsf : datasetVersion.getFlatDatasetFields()) {

                DatasetFieldType dsfType = dsf.getDatasetFieldType();
                String solrFieldSearchable = dsfType.getSolrField().getNameSearchable();
                String solrFieldFacetable = dsfType.getSolrField().getNameFacetable();

                if (dsf.getValues() != null && !dsf.getValues().isEmpty() && dsf.getValues().get(0) != null && solrFieldSearchable != null) {
                    logger.fine("indexing " + dsf.getDatasetFieldType().getName() + ":" + dsf.getValues() + " into " + solrFieldSearchable + " and maybe " + solrFieldFacetable);
//                    if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.INTEGER)) {
                    if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.EMAIL)) {
                        //no-op. we want to keep email address out of Solr per https://github.com/IQSS/dataverse/issues/759
                    } else if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.DATE)) {
                        String dateAsString = dsf.getValues().get(0);
                        logger.fine("date as string: " + dateAsString);
                        if (dateAsString != null && !dateAsString.isEmpty()) {
                            SimpleDateFormat inputDateyyyy = new SimpleDateFormat("yyyy", Locale.ENGLISH);
                            try {
                                /**
                                 * @todo when bean validation is working we
                                 * won't have to convert strings into dates
                                 */
                                logger.fine("Trying to convert " + dateAsString + " to a YYYY date from dataset " + dataset.getId());
                                Date dateAsDate = inputDateyyyy.parse(dateAsString);
                                SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
                                String datasetFieldFlaggedAsDate = yearOnly.format(dateAsDate);
                                logger.fine("YYYY only: " + datasetFieldFlaggedAsDate);
//                                solrInputDocument.addField(solrFieldSearchable, Integer.parseInt(datasetFieldFlaggedAsDate));
                                solrInputDocument.addField(solrFieldSearchable, datasetFieldFlaggedAsDate);
                                if (dsfType.getSolrField().isFacetable()) {
//                                    solrInputDocument.addField(solrFieldFacetable, Integer.parseInt(datasetFieldFlaggedAsDate));
                                    solrInputDocument.addField(solrFieldFacetable, datasetFieldFlaggedAsDate);
                                }
                            } catch (Exception ex) {
                                logger.info("unable to convert " + dateAsString + " into YYYY format and couldn't index it (" + dsfType.getName() + ")");
                            }
                        }
                    } else {
                        // _s (dynamic string) and all other Solr fields

                        if (dsf.getDatasetFieldType().getName().equals("authorAffiliation")) {
                            /**
                             * @todo think about how to tie the fact that this
                             * needs to be multivalued (_ss) because a
                             * multivalued facet (authorAffilition_ss) is being
                             * collapsed into here at index time. The business
                             * logic to determine if a data-driven metadata
                             * field should be indexed into Solr as a single or
                             * multiple value lives in the getSolrField() method
                             * of DatasetField.java
                             */
                            solrInputDocument.addField(SearchFields.AFFILIATION, dsf.getValuesWithoutNaValues());
                        } else if (dsf.getDatasetFieldType().getName().equals("title")) {
                            // datasets have titles not names but index title under name as well so we can sort datasets by name along dataverses and files
                            List<String> possibleTitles = dsf.getValues();
                            String firstTitle = possibleTitles.get(0);
                            if (firstTitle != null) {
                                parentDatasetTitle = firstTitle;
                            }
                            solrInputDocument.addField(SearchFields.NAME_SORT, dsf.getValues());
                        }
                        if (dsfType.isControlledVocabulary()) {
                            for (ControlledVocabularyValue controlledVocabularyValue : dsf.getControlledVocabularyValues()) {
                                if (controlledVocabularyValue.getStrValue().equals(DatasetField.NA_VALUE)) {
                                    continue;
                                }
                                solrInputDocument.addField(solrFieldSearchable, controlledVocabularyValue.getStrValue());
                                if (dsfType.getSolrField().isFacetable()) {
                                    solrInputDocument.addField(solrFieldFacetable, controlledVocabularyValue.getStrValue());
                                }
                            }
                        } else if (dsfType.getFieldType().equals(DatasetFieldType.FieldType.TEXTBOX)) {
                            // strip HTML
                            List<String> htmlFreeText = StringUtil.htmlArray2textArray(dsf.getValuesWithoutNaValues());
                            solrInputDocument.addField(solrFieldSearchable, htmlFreeText);
                            if (dsfType.getSolrField().isFacetable()) {
                                solrInputDocument.addField(solrFieldFacetable, htmlFreeText);
                            }
                        } else {
                            // do not strip HTML
                            solrInputDocument.addField(solrFieldSearchable, dsf.getValuesWithoutNaValues());
                            if (dsfType.getSolrField().isFacetable()) {
                                if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.topicClassValue)) {
                                    String topicClassificationTerm = getTopicClassificationTermOrTermAndVocabulary(dsf);
                                    if (topicClassificationTerm != null) {
                                        logger.fine(solrFieldFacetable + " gets " + topicClassificationTerm);
                                        solrInputDocument.addField(solrFieldFacetable, topicClassificationTerm);
                                    }
                                } else {
                                    solrInputDocument.addField(solrFieldFacetable, dsf.getValuesWithoutNaValues());
                                }
                            }
                        }
                    }
                }
            }
        }

        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
//        solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataset.getOwner().getName());
        solrInputDocument.addField(SearchFields.PARENT_ID, dataset.getOwner().getId());
        solrInputDocument.addField(SearchFields.PARENT_NAME, dataset.getOwner().getName());

        if (state.equals(indexableDataset.getDatasetState().DEACCESSIONED)) {
            String deaccessionNote = datasetVersion.getVersionNote();
            if (deaccessionNote != null) {
                solrInputDocument.addField(SearchFields.DATASET_DEACCESSION_REASON, deaccessionNote);
            }
        }

        docs.add(solrInputDocument);

        List<String> filesIndexed = new ArrayList<>();
        if (datasetVersion != null) {
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            boolean checkForDuplicateMetadata = false;
            if (datasetVersion.isDraft() && dataset.isReleased() && dataset.getReleasedVersion() != null) {
                checkForDuplicateMetadata = true;
                logger.fine("We are indexing a draft version of a dataset that has a released version. We'll be checking file metadatas if they are exact clones of the released versions.");
            }

            for (FileMetadata fileMetadata : fileMetadatas) {
                boolean indexThisMetadata = true;
                if (checkForDuplicateMetadata) {
                    logger.fine("Checking if this file metadata is a duplicate.");
                    for (FileMetadata releasedFileMetadata : dataset.getReleasedVersion().getFileMetadatas()) {
                        if (fileMetadata.getDataFile() != null && fileMetadata.getDataFile().equals(releasedFileMetadata.getDataFile())) {
                            if (fileMetadata.contentEquals(releasedFileMetadata)) {
                                indexThisMetadata = false;
                                logger.fine("This file metadata hasn't changed since the released version; skipping indexing.");
                            } else {
                                logger.fine("This file metadata has changed since the released version; we want to index it!");
                            }
                            break;
                        }
                    }
                }
                if (indexThisMetadata) {
                    SolrInputDocument datafileSolrInputDocument = new SolrInputDocument();
                    Long fileEntityId = fileMetadata.getDataFile().getId();
                    datafileSolrInputDocument.addField(SearchFields.ENTITY_ID, fileEntityId);
                    datafileSolrInputDocument.addField(SearchFields.DATAVERSE_VERSION_INDEXED_BY, dataverseVersion);
                    datafileSolrInputDocument.addField(SearchFields.IDENTIFIER, fileEntityId);
                    datafileSolrInputDocument.addField(SearchFields.PERSISTENT_URL, dataset.getPersistentURL());
                    datafileSolrInputDocument.addField(SearchFields.TYPE, "files");

                    String filenameCompleteFinal = "";
                    if (fileMetadata != null) {
                        String filenameComplete = fileMetadata.getLabel();
                        if (filenameComplete != null) {
                            String filenameWithoutExtension = "";
                            // String extension = "";
                            int i = filenameComplete.lastIndexOf('.');
                            if (i > 0) {
                                // extension = filenameComplete.substring(i + 1);
                                try {
                                    filenameWithoutExtension = filenameComplete.substring(0, i);
                                    datafileSolrInputDocument.addField(SearchFields.FILENAME_WITHOUT_EXTENSION, filenameWithoutExtension);
                                    datafileSolrInputDocument.addField(SearchFields.FILE_NAME, filenameWithoutExtension);
                                } catch (IndexOutOfBoundsException ex) {
                                    filenameWithoutExtension = "";
                                }
                            } else {
                                logger.fine("problem with filename '" + filenameComplete + "': no extension? empty string as filename?");
                                filenameWithoutExtension = filenameComplete;
                            }
                            filenameCompleteFinal = filenameComplete;
                        }
                        for (String tag : fileMetadata.getCategoriesByName()) {
                            datafileSolrInputDocument.addField(SearchFields.FILE_TAG, tag);
                            datafileSolrInputDocument.addField(SearchFields.FILE_TAG_SEARCHABLE, tag);
                        }
                    }
                    datafileSolrInputDocument.addField(SearchFields.NAME, filenameCompleteFinal);
                    datafileSolrInputDocument.addField(SearchFields.NAME_SORT, filenameCompleteFinal);
                    datafileSolrInputDocument.addField(SearchFields.FILE_NAME, filenameCompleteFinal);

                    datafileSolrInputDocument.addField(SearchFields.DATASET_VERSION_ID, datasetVersion.getId());

                    /**
                     * for rules on sorting files see
                     * https://docs.google.com/a/harvard.edu/document/d/1DWsEqT8KfheKZmMB3n_VhJpl9nIxiUjai_AIQPAjiyA/edit?usp=sharing
                     * via https://redmine.hmdc.harvard.edu/issues/3701
                     */
                    Date fileSortByDate = new Date();
                    DataFile datafile = fileMetadata.getDataFile();
                    if (datafile != null) {
                        boolean fileHasBeenReleased = datafile.isReleased();
                        if (fileHasBeenReleased) {
                            logger.fine("indexing file with filePublicationTimestamp. " + fileMetadata.getId() + " (file id " + datafile.getId() + ")");
                            Timestamp filePublicationTimestamp = datafile.getPublicationDate();
                            if (filePublicationTimestamp != null) {
                                fileSortByDate = filePublicationTimestamp;
                            } else {
                                String msg = "filePublicationTimestamp was null for fileMetadata id " + fileMetadata.getId() + " (file id " + datafile.getId() + ")";
                                logger.info(msg);
                            }
                            datafileSolrInputDocument.addField(SearchFields.ACCESS, datafile.isRestricted() ? SearchConstants.RESTRICTED : SearchConstants.PUBLIC);
                        } else {
                            logger.fine("indexing file with fileCreateTimestamp. " + fileMetadata.getId() + " (file id " + datafile.getId() + ")");
                            Timestamp fileCreateTimestamp = datafile.getCreateDate();
                            if (fileCreateTimestamp != null) {
                                fileSortByDate = fileCreateTimestamp;
                            } else {
                                String msg = "fileCreateTimestamp was null for fileMetadata id " + fileMetadata.getId() + " (file id " + datafile.getId() + ")";
                                logger.info(msg);
                            }
                            datafileSolrInputDocument.addField(SearchFields.ACCESS, fileMetadata.isRestricted() ? SearchConstants.RESTRICTED : SearchConstants.PUBLIC);
                        }
                        if (datafile.isHarvested()) {
                            datafileSolrInputDocument.addField(SearchFields.IS_HARVESTED, true);
                            datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE, HARVESTED);
                        } else {
                            datafileSolrInputDocument.addField(SearchFields.IS_HARVESTED, false);
                            datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE, findRootDataverseCached().getName() + " " + BundleUtil.getStringFromBundle("dataverse"));
                        }
                    }
                    if (fileSortByDate == null) {
                        if (datasetSortByDate != null) {
                            logger.info("fileSortByDate was null, assigning datasetSortByDate");
                            fileSortByDate = datasetSortByDate;
                        } else {
                            logger.info("fileSortByDate and datasetSortByDate were null, assigning 'now'");
                            fileSortByDate = new Date();
                        }
                    }
                    datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, fileSortByDate);
                    datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT, convertToFriendlyDate(fileSortByDate));

                    if (majorVersionReleaseDate == null && !datafile.isHarvested()) {
                        datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
                    }

                    if (datasetVersion.isInReview()) {
                        datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, IN_REVIEW_STRING);
                    }

                    String fileSolrDocId = solrDocIdentifierFile + fileEntityId;
                    if (indexableDataset.getDatasetState().equals(indexableDataset.getDatasetState().PUBLISHED)) {
                        fileSolrDocId = solrDocIdentifierFile + fileEntityId;
                        datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
//                    datafileSolrInputDocument.addField(SearchFields.PERMS, publicGroupString);
                        addDatasetReleaseDateToSolrDoc(datafileSolrInputDocument, dataset);
                    } else if (indexableDataset.getDatasetState().equals(indexableDataset.getDatasetState().WORKING_COPY)) {
                        fileSolrDocId = solrDocIdentifierFile + fileEntityId + indexableDataset.getDatasetState().getSuffix();
                        datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DRAFT_STRING);
                    }
                    datafileSolrInputDocument.addField(SearchFields.ID, fileSolrDocId);

                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_FRIENDLY, fileMetadata.getDataFile().getFriendlyType());
                    datafileSolrInputDocument.addField(SearchFields.FILE_CONTENT_TYPE, fileMetadata.getDataFile().getContentType());
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, fileMetadata.getDataFile().getFriendlyType());
                    // For the file type facets, we have a property file that maps mime types 
                    // to facet-friendly names; "application/fits" should become "FITS", etc.:
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, FileUtil.getFacetFileType(fileMetadata.getDataFile()));
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, FileUtil.getFacetFileType(fileMetadata.getDataFile()));
                    datafileSolrInputDocument.addField(SearchFields.FILE_SIZE_IN_BYTES, fileMetadata.getDataFile().getFilesize());
                    if (DataFile.ChecksumType.MD5.equals(fileMetadata.getDataFile().getChecksumType())) {
                        /**
                         * @todo Someday we should probably deprecate this
                         * FILE_MD5 in favor of a combination of
                         * FILE_CHECKSUM_TYPE and FILE_CHECKSUM_VALUE.
                         */
                        datafileSolrInputDocument.addField(SearchFields.FILE_MD5, fileMetadata.getDataFile().getChecksumValue());
                    }
                    datafileSolrInputDocument.addField(SearchFields.FILE_CHECKSUM_TYPE, fileMetadata.getDataFile().getChecksumType().toString());
                    datafileSolrInputDocument.addField(SearchFields.FILE_CHECKSUM_VALUE, fileMetadata.getDataFile().getChecksumValue());
                    datafileSolrInputDocument.addField(SearchFields.DESCRIPTION, fileMetadata.getDescription());
                    datafileSolrInputDocument.addField(SearchFields.FILE_DESCRIPTION, fileMetadata.getDescription());
                    datafileSolrInputDocument.addField(SearchFields.UNF, fileMetadata.getDataFile().getUnf());
                    datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
//            datafileSolrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataFile.getOwner().getOwner().getName());
                    // datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, dataFile.getDataset().getTitle());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_ID, fileMetadata.getDataFile().getOwner().getId());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_IDENTIFIER, fileMetadata.getDataFile().getOwner().getGlobalId());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_CITATION, fileMetadata.getDataFile().getOwner().getCitation());

                    datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, parentDatasetTitle);

                    // If this is a tabular data file -- i.e., if there are data
                    // variables associated with this file, we index the variable 
                    // names and labels: 
                    if (fileMetadata.getDataFile().isTabularData()) {
                        List<DataVariable> variables = fileMetadata.getDataFile().getDataTable().getDataVariables();
                        for (DataVariable var : variables) {
                            // Hard-coded search fields, for now: 
                            // TODO: eventually: review, decide how datavariables should
                            // be handled for indexing purposes. (should it be a fixed
                            // setup, defined in the code? should it be flexible? unlikely
                            // that this needs to be domain-specific... since these data
                            // variables are quite specific to tabular data, which in turn
                            // is something social science-specific...
                            // anyway -- needs to be reviewed. -- L.A. 4.0alpha1 

                            if (var.getName() != null && !var.getName().equals("")) {
                                datafileSolrInputDocument.addField(SearchFields.VARIABLE_NAME, var.getName());
                            }
                            if (var.getLabel() != null && !var.getLabel().equals("")) {
                                datafileSolrInputDocument.addField(SearchFields.VARIABLE_LABEL, var.getLabel());
                            }
                        }
                        // TABULAR DATA TAGS:
                        // (not to be confused with the file categories, indexed above!)
                        for (DataFileTag tag : fileMetadata.getDataFile().getTags()) {
                            String tagLabel = tag.getTypeLabel();
                            datafileSolrInputDocument.addField(SearchFields.TABDATA_TAG, tagLabel);
                        }
                    }

                    if (indexableDataset.isFilesShouldBeIndexed()) {
                        filesIndexed.add(fileSolrDocId);
                        docs.add(datafileSolrInputDocument);
                    }
                }
            }
        }

        try {
            solrServer.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            solrServer.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        dvObjectService.updateContentIndexTime(dataset);

//        return "indexed dataset " + dataset.getId() + " as " + solrDocId + "\nindexFilesResults for " + solrDocId + ":" + fileInfo.toString();
        return "indexed dataset " + dataset.getId() + " as " + datasetSolrDocId + ". filesIndexed: " + filesIndexed;
    }

    /**
     * If the "Topic Classification" has a "Vocabulary", return both the "Term"
     * and the "Vocabulary" with the latter in parentheses. For example, the
     * Murray Research Archive uses "1 (Generations)" and "yes (Follow-up
     * permitted)".
     */
    private String getTopicClassificationTermOrTermAndVocabulary(DatasetField topicClassDatasetField) {
        String finalValue = null;
        String topicClassVocab = null;
        String topicClassValue = null;
        for (DatasetField sibling : topicClassDatasetField.getParentDatasetFieldCompoundValue().getChildDatasetFields()) {
            DatasetFieldType datasetFieldType = sibling.getDatasetFieldType();
            String name = datasetFieldType.getName();
            if (name.equals(DatasetFieldConstant.topicClassVocab)) {
                topicClassVocab = sibling.getDisplayValue();
            } else if (name.equals(DatasetFieldConstant.topicClassValue)) {
                topicClassValue = sibling.getDisplayValue();
            }
            if (topicClassValue != null) {
                if (topicClassVocab != null) {
                    finalValue = topicClassValue + " (" + topicClassVocab + ")";
                } else {
                    finalValue = topicClassValue;
                }
            }
        }
        return finalValue;
    }

    public List<String> findPathSegments(Dataverse dataverse, List<String> segments) {
        Dataverse rootDataverse = findRootDataverseCached();
        if (!dataverse.equals(rootDataverse)) {
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                findPathSegments(dataverse.getOwner(), segments);
            }
            segments.add(dataverse.getId().toString());
            return segments;
        } else {
            // base case
            return segments;
        }
    }

    List<String> getDataversePathsFromSegments(List<String> dataversePathSegments) {
        List<String> subtrees = new ArrayList<>();
        for (int i = 0; i < dataversePathSegments.size(); i++) {
            StringBuilder pathBuilder = new StringBuilder();
            int numSegments = dataversePathSegments.size();
            for (int j = 0; j < numSegments; j++) {
                if (j <= i) {
                    pathBuilder.append("/" + dataversePathSegments.get(j));
                }
            }
            subtrees.add(pathBuilder.toString());
        }
        return subtrees;
    }

    private void addDataverseReleaseDateToSolrDoc(SolrInputDocument solrInputDocument, Dataverse dataverse) {
        if (dataverse.getPublicationDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dataverse.getPublicationDate().getTime());
            int YYYY = calendar.get(Calendar.YEAR);
            solrInputDocument.addField(SearchFields.PUBLICATION_DATE, YYYY);
        }
    }

    private void addDatasetReleaseDateToSolrDoc(SolrInputDocument solrInputDocument, Dataset dataset) {
        if (dataset.getPublicationDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dataset.getPublicationDate().getTime());
            int YYYY = calendar.get(Calendar.YEAR);
            solrInputDocument.addField(SearchFields.PUBLICATION_DATE, YYYY);
            solrInputDocument.addField(SearchFields.DATASET_PUBLICATION_DATE, YYYY);
        }
    }

    public static String getGroupPrefix() {
        return groupPrefix;
    }

    public static String getGroupPerUserPrefix() {
        return groupPerUserPrefix;
    }

    public static String getPublicGroupString() {
        return publicGroupString;
    }

    public static String getPUBLISHED_STRING() {
        return PUBLISHED_STRING;
    }

    public static String getUNPUBLISHED_STRING() {
        return UNPUBLISHED_STRING;
    }

    public static String getDRAFT_STRING() {
        return DRAFT_STRING;
    }

    public static String getIN_REVIEW_STRING() {
        return IN_REVIEW_STRING;
    }

    public static String getDEACCESSIONED_STRING() {
        return DEACCESSIONED_STRING;
    }

    public String delete(Dataverse doomed) {
        logger.fine("deleting Solr document for dataverse " + doomed.getId());
        UpdateResponse updateResponse;
        try {
            updateResponse = solrServer.deleteById(solrDocIdentifierDataverse + doomed.getId());
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            solrServer.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Successfully deleted dataverse " + doomed.getId() + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.fine(response);
        return response;
    }

    /**
     * @todo call this in fewer places, favoring
     * SolrIndexServiceBeans.deleteMultipleSolrIds instead to operate in batches
     *
     * https://github.com/IQSS/dataverse/issues/142
     */
    public String removeSolrDocFromIndex(String doomed) {
        
        logger.fine("deleting Solr document: " + doomed);
        UpdateResponse updateResponse;
        try {
            updateResponse = solrServer.deleteById(doomed);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            solrServer.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Attempted to delete " + doomed + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.fine(response);
        return response;
    }

    public String convertToFriendlyDate(Date dateAsDate) {
        if (dateAsDate == null) {
            dateAsDate = new Date();
        }
        //  using DateFormat.MEDIUM for May 5, 2014 to match what's in DVN 3.x
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String friendlyDate = format.format(dateAsDate);
        return friendlyDate;
    }

    private List<String> findSolrDocIdsForDraftFilesToDelete(Dataset datasetWithDraftFilesToDelete) {
        List<String> solrIdsOfFilesToDelete = new ArrayList<>();
        for (DatasetVersion datasetVersion : datasetWithDraftFilesToDelete.getVersions()) {
            for (FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
                DataFile datafile = fileMetadata.getDataFile();
                if (datafile != null) {
                    solrIdsOfFilesToDelete.add(solrDocIdentifierFile + datafile.getId() + draftSuffix);
                }
            }

        }
        return solrIdsOfFilesToDelete;
    }

    private List<String> findSolrDocIdsForFilesToDelete(Dataset dataset, IndexableDataset.DatasetState state) {
        List<String> solrIdsOfFilesToDelete = new ArrayList<>();
        for (DataFile file : dataset.getFiles()) {
            solrIdsOfFilesToDelete.add(solrDocIdentifierFile + file.getId() + state.getSuffix());
        }
        return solrIdsOfFilesToDelete;
    }

    private String removeMultipleSolrDocs(List<String> docIds) {
        IndexResponse indexResponse = solrIndexService.deleteMultipleSolrIds(docIds);
        return indexResponse.toString();
    }

    private String determinePublishedDatasetSolrDocId(Dataset dataset) {
        return IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.PUBLISHED.getSuffix();
    }

    private String determineDeaccessionedDatasetId(Dataset dataset) {
        return IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.DEACCESSIONED.getSuffix();
    }

    private String removeDeaccessioned(Dataset dataset) {
        StringBuilder result = new StringBuilder();
        String deleteDeaccessionedResult = removeSolrDocFromIndex(determineDeaccessionedDatasetId(dataset));
        result.append(deleteDeaccessionedResult);
        List<String> docIds = findSolrDocIdsForFilesToDelete(dataset, IndexableDataset.DatasetState.DEACCESSIONED);
        String deleteFilesResult = removeMultipleSolrDocs(docIds);
        result.append(deleteFilesResult);
        return result.toString();
    }

    private String removePublished(Dataset dataset) {
        StringBuilder result = new StringBuilder();
        String deletePublishedResult = removeSolrDocFromIndex(determinePublishedDatasetSolrDocId(dataset));
        result.append(deletePublishedResult);
        List<String> docIds = findSolrDocIdsForFilesToDelete(dataset, IndexableDataset.DatasetState.PUBLISHED);
        String deleteFilesResult = removeMultipleSolrDocs(docIds);
        result.append(deleteFilesResult);
        return result.toString();
    }

    private Dataverse findRootDataverseCached() {
        if (true) {
            /**
             * @todo Is the code below working at all? We don't want the root
             * dataverse to be indexed into Solr. Specifically, we don't want a
             * dataverse "card" to show up while browsing.
             *
             * Let's just find the root dataverse and be done with it. We'll
             * figure out the caching later.
             */
            try {
                Dataverse rootDataverse = dataverseService.findRootDataverse();
                return rootDataverse;
            } catch (EJBException ex) {
                logger.info("caught " + ex);
                Throwable cause = ex.getCause();
                while (cause.getCause() != null) {
                    logger.info("caused by... " + cause);
                    cause = cause.getCause();
                }
                return null;
            }
        }

        /**
         * @todo Why isn't this code working?
         */
        if (rootDataverseCached != null) {
            return rootDataverseCached;
        } else {
            rootDataverseCached = dataverseService.findRootDataverse();
            if (rootDataverseCached != null) {
                return rootDataverseCached;
            } else {
                throw new RuntimeException("unable to determine root dataverse");
            }
        }
    }

    private String getDesiredCardState(Map<DatasetVersion.VersionState, Boolean> desiredCards) {
        /**
         * @todo make a JVM option to enforce sanity checks? Call it dev=true?
         */
        boolean sanityCheck = true;
        if (sanityCheck) {
            Set<DatasetVersion.VersionState> expected = new HashSet<>();
            expected.add(DatasetVersion.VersionState.DRAFT);
            expected.add(DatasetVersion.VersionState.RELEASED);
            expected.add(DatasetVersion.VersionState.DEACCESSIONED);
            if (!desiredCards.keySet().equals(expected)) {
                throw new RuntimeException("Mismatch between expected version states (" + expected + ") and version states passed in (" + desiredCards.keySet() + ")");
            }
        }
        return "Desired state for existence of cards: " + desiredCards + "\n";
    }

    /**
     * @return Dataverses that should be reindexed either because they have
     * never been indexed or their index time is before their modification time.
     */
    public List<Dataverse> findStaleOrMissingDataverses() {
        List<Dataverse> staleDataverses = new ArrayList<>();
        for (Dataverse dataverse : dataverseService.findAll()) {
            if (dataverse.equals(dataverseService.findRootDataverse())) {
                continue;
            }
            if (stale(dataverse)) {
                staleDataverses.add(dataverse);
            }
        }
        return staleDataverses;
    }

    /**
     * @return Datasets that should be reindexed either because they have never
     * been indexed or their index time is before their modification time.
     */
    public List<Dataset> findStaleOrMissingDatasets() {
        List<Dataset> staleDatasets = new ArrayList<>();
        for (Dataset dataset : datasetService.findAll()) {
            if (stale(dataset)) {
                staleDatasets.add(dataset);
            }
        }
        return staleDatasets;
    }

    private boolean stale(DvObject dvObject) {
        Timestamp indexTime = dvObject.getIndexTime();
        Timestamp modificationTime = dvObject.getModificationTime();
        if (indexTime == null) {
            return true;
        } else if (indexTime.before(modificationTime)) {
            return true;
        }
        return false;
    }

    public List<Long> findDataversesInSolrOnly() throws SearchException {
        try {
            /**
             * @todo define this centrally and statically
             */
            return findDvObjectInSolrOnly("dataverses");
        } catch (SearchException ex) {
            throw ex;
        }
    }

    public List<Long> findDatasetsInSolrOnly() throws SearchException {
        try {
            /**
             * @todo define this centrally and statically
             */
            return findDvObjectInSolrOnly("datasets");
        } catch (SearchException ex) {
            throw ex;
        }
    }

    public List<Long> findFilesInSolrOnly() throws SearchException {
        try {
            /**
             * @todo define this centrally and statically
             */
            return findDvObjectInSolrOnly("files");
        } catch (SearchException ex) {
            throw ex;
        }
    }

    private List<Long> findDvObjectInSolrOnly(String type) throws SearchException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*");
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.addFilterQuery(SearchFields.TYPE + ":" + type);
        List<Long> dvObjectInSolrOnly = new ArrayList<>();
        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (SolrServerException ex) {
            throw new SearchException("Error searching Solr for " + type, ex);
        }
        SolrDocumentList results = queryResponse.getResults();
        for (SolrDocument solrDocument : results) {
            Object idObject = solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            if (idObject != null) {
                try {
                    long id = (Long) idObject;
                    DvObject dvobject = dvObjectService.findDvObject(id);
                    if (dvobject == null) {
                        dvObjectInSolrOnly.add(id);
                    }
                } catch (ClassCastException ex) {
                    throw new SearchException("Found " + SearchFields.ENTITY_ID + " but error casting " + idObject + " to long", ex);
                }
            }
        }
        return dvObjectInSolrOnly;
    }

    private List<String> findFilesOfParentDataset(long parentDatasetId) throws SearchException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*");
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.addFilterQuery(SearchFields.PARENT_ID + ":" + parentDatasetId);
        /**
         * @todo "files" should be a constant
         */
        solrQuery.addFilterQuery(SearchFields.TYPE + ":" + "files");
        List<String> dvObjectInSolrOnly = new ArrayList<>();
        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (SolrServerException ex) {
            throw new SearchException("Error searching Solr for dataset parent id " + parentDatasetId, ex);
        }
        SolrDocumentList results = queryResponse.getResults();
        for (SolrDocument solrDocument : results) {
            Object idObject = solrDocument.getFieldValue(SearchFields.ID);
            if (idObject != null) {
                String id = (String) idObject;
                dvObjectInSolrOnly.add(id);
            }
        }
        return dvObjectInSolrOnly;
    }
    
    // This is a convenience method for deleting all the SOLR documents
    // (Datasets and DataFiles) harvested by a specific HarvestingClient.
    // The delete logic is a bit simpler, than when deleting "real", local 
    // datasets and files - for example, harvested datasets are never Drafts, etc.
    // We are also less concerned with the diagnostics; if any of it fails, 
    // we don't need to treat it as a fatal condition. 
    public void deleteHarvestedDocuments(HarvestingClient harvestingClient) {
        List<String> solrIdsOfDatasetsToDelete = new ArrayList<>();
        
        // I am going to make multiple solrIndexService.deleteMultipleSolrIds() calls;
        // one call for the list of datafiles in each dataset; then one more call to 
        // delete all the dataset documents. 
        // I'm *assuming* this is safer than to try and make one complete list of 
        // all the documents (datasets and datafiles), and then attempt to delete 
        // them all at once... (is there a limit??) The list can be huge - if the 
        // harvested archive is on the scale of Odum or ICPSR, with thousands of 
        // datasets and tens of thousands of files. 
        // 
        
        for (Dataset harvestedDataset : harvestingClient.getHarvestedDatasets()) {
            solrIdsOfDatasetsToDelete.add(solrDocIdentifierDataset + harvestedDataset.getId());
            
            List<String> solrIdsOfDatafilesToDelete = new ArrayList<>();
            for (DataFile datafile : harvestedDataset.getFiles()) {
                solrIdsOfDatafilesToDelete.add(solrDocIdentifierFile + datafile.getId());
            }
            logger.fine("attempting to delete the following datafiles from the index: " + StringUtils.join(solrIdsOfDatafilesToDelete, ","));
            IndexResponse resultOfAttemptToDeleteFiles = solrIndexService.deleteMultipleSolrIds(solrIdsOfDatafilesToDelete);
            logger.fine("result of an attempted delete of the harvested files associated with the dataset "+harvestedDataset.getId()+": "+resultOfAttemptToDeleteFiles);
            
        }

        logger.fine("attempting to delete the following datasets from the index: " + StringUtils.join(solrIdsOfDatasetsToDelete, ","));
        IndexResponse resultOfAttemptToDeleteDatasets = solrIndexService.deleteMultipleSolrIds(solrIdsOfDatasetsToDelete);
        logger.fine("result of attempt to delete harvested datasets associated with the client: " + resultOfAttemptToDeleteDatasets + "\n");

    }
    
    // Another convenience method, for deleting all the SOLR documents (dataset_
    // and datafile_s) associated with a harveste dataset. The comments for the 
    // method above apply here too.
    public void deleteHarvestedDocuments(Dataset harvestedDataset) {
        List<String> solrIdsOfDocumentsToDelete = new ArrayList<>();   
        solrIdsOfDocumentsToDelete.add(solrDocIdentifierDataset + harvestedDataset.getId());
        
        for (DataFile datafile : harvestedDataset.getFiles()) {
                solrIdsOfDocumentsToDelete.add(solrDocIdentifierFile + datafile.getId());
        }
        
        logger.fine("attempting to delete the following documents from the index: " + StringUtils.join(solrIdsOfDocumentsToDelete, ","));
        IndexResponse resultOfAttemptToDeleteDocuments = solrIndexService.deleteMultipleSolrIds(solrIdsOfDocumentsToDelete);
        logger.fine("result of attempt to delete harvested documents: " + resultOfAttemptToDeleteDocuments + "\n");
    }
    

}
