package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.search.IndexableDataset;
import edu.harvard.iq.dataverse.search.IndexableObject;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

@Stateless
@Named
public class IndexServiceBean {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseUserServiceBean dataverseUserServiceBean;

    private final String solrDocIdentifierDataverse = "dataverse_";
    public static final String solrDocIdentifierFile = "datafile_";
    public static final String solrDocIdentifierDataset = "dataset_";
    public static final String draftSuffix = "_draft";
    public static final String deaccessionedSuffix = "_deaccessioned";
    private static final String groupPrefix = "group_";
    private static final String groupPerUserPrefix = "group_user";
    private static final Long publicGroupId = 1L;
    private static final String publicGroupString = groupPrefix + "public";
    /**
     * @todo: remove this fake "has access to all data" group
     */
    private static final Long tmpNsaGroupId = 2L;
    private static final String PUBLISHED_STRING = "Published";
    private static final String UNPUBLISHED_STRING = "Unpublished";
    private static final String DRAFT_STRING = "Draft";
    private static final String DEACCESSIONED_STRING = "Deaccessioned";
    private Dataverse rootDataverseCached;

    public String indexAll() {
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
        logger.info("deleting all Solr documents before a complete re-index");
        try {
            server.deleteByQuery("*:*");// CAUTION: deletes everything!
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        /**
         * @todo: replace hard-coded groups with real groups
         */
        Map<Long, String> groups = new HashMap<>();
        groups.put(publicGroupId, publicGroupString);
        groups.put(tmpNsaGroupId, "nsa");
        groups.put(tmpNsaGroupId + 1, "flappybird");
        groups.put(tmpNsaGroupId + 2, "2048");

        int groupIndexCount = 0;
        for (Map.Entry<Long, String> group : groups.entrySet()) {
            groupIndexCount++;
            logger.info("indexing group " + groupIndexCount + " of " + groups.size() + ": " + indexGroup(group));
        }

        int userIndexCount = 0;
        for (DataverseUser user : dataverseUserServiceBean.findAll()) {
            userIndexCount++;
            logger.info("indexing user " + userIndexCount + " of several: " + indexUser(user));
        }
        List<Dataverse> dataverses = dataverseService.findAll();
        int dataverseIndexCount = 0;
        for (Dataverse dataverse : dataverses) {
            logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverses.size() + ": " + indexDataverse(dataverse));
            dataverseIndexCount++;
        }

        int datasetIndexCount = 0;
        List<Dataset> datasets = datasetService.findAll();
        for (Dataset dataset : datasets) {
            datasetIndexCount++;
            logger.info("indexing dataset " + datasetIndexCount + " of " + datasets.size() + ": " + indexDataset(dataset));
        }
//        logger.info("advanced search fields: " + advancedSearchFields);
//        logger.info("not advanced search fields: " + notAdvancedSearchFields);
        logger.info("done iterating through all datasets");

        return dataverseIndexCount + " dataverses, " + datasetIndexCount + " datasets, " + groupIndexCount + " groups, and " + userIndexCount + " users indexed\n";
    }

    public String indexDataverse(Dataverse dataverse) {
        logger.info("indexDataverse called on dataverse id " + dataverse.getId() + "(" + dataverse.getAlias() + ")");
        if (dataverse.getId() == null) {
            String msg = "unable to index dataverse. id was null (alias: " + dataverse.getAlias() + ")";
            logger.info(msg);
            return msg;
        }
        Dataverse rootDataverse = findRootDataverseCached();
        if (dataverse.getId() == rootDataverse.getId()) {
            /**
             * @todo: replace hard-coded groups with real groups
             */
            Map<Long, String> groups = new HashMap<>();
            groups.put(publicGroupId, publicGroupString);
            groups.put(tmpNsaGroupId, "nsa");
            groups.put(tmpNsaGroupId + 1, "flappybird");
            groups.put(tmpNsaGroupId + 2, "2048");

            int groupIndexCount = 0;
            for (Map.Entry<Long, String> group : groups.entrySet()) {
                groupIndexCount++;
                logger.info("indexing group " + groupIndexCount + " of " + groups.size() + ": " + indexGroup(group));
            }
            return "The root dataverse shoud not be indexed. Indexed temporary groups instead.";
        }
        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, solrDocIdentifierDataverse + dataverse.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataverse.getId());
        solrInputDocument.addField(SearchFields.TYPE, "dataverses");
        solrInputDocument.addField(SearchFields.NAME, dataverse.getName());
        solrInputDocument.addField(SearchFields.NAME_SORT, dataverse.getName());
        solrInputDocument.addField(SearchFields.DATAVERSE_NAME, dataverse.getName());
        if (dataverse.isReleased()) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getPublicationDate());
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT, convertToFriendlyDate(dataverse.getPublicationDate()));
            solrInputDocument.addField(SearchFields.PERMS, publicGroupString);
        } else if (dataverse.getCreator() != null) { //@todo: do we need this check still
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getCreateDate());
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT, convertToFriendlyDate(dataverse.getCreateDate()));
        }

        if (dataverse.getCreator() != null) {
            solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + dataverse.getCreator().getId());
            /**
             * @todo: replace this fake version of granting users access to
             * dataverses with the real thing, when it's available in the app
             */
            if (dataverse.getCreator().getUserName().equals("pete")) {
                // figure out if cathy is around
                DataverseUser cathy = dataverseUserServiceBean.findByUserName("cathy");
                if (cathy != null) {
                    // let cathy see all of pete's dataverses
                    solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + cathy.getId());
                }
            }
        }

        /**
         * @todo: remove this fake "has access to all data" group
         */
        solrInputDocument.addField(SearchFields.PERMS, groupPrefix + tmpNsaGroupId);

        addDataverseReleaseDateToSolrDoc(solrInputDocument, dataverse);
//        if (dataverse.getOwner() != null) {
//            solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataverse.getOwner().getName());
//        }
        solrInputDocument.addField(SearchFields.DESCRIPTION, dataverse.getDescription());
        solrInputDocument.addField(SearchFields.DATAVERSE_DESCRIPTION, dataverse.getDescription());
//        logger.info("dataverse affiliation: " + dataverse.getAffiliation());
        if (dataverse.getAffiliation() != null && !dataverse.getAffiliation().isEmpty()) {
            /**
             * @todo: stop using affiliation as category
             */
//            solrInputDocument.addField(SearchFields.CATEGORY, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.AFFILIATION, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.DATAVERSE_AFFILIATION, dataverse.getAffiliation());
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
        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
        docs.add(solrInputDocument);

        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            if (dataverse.getId() != null) {
                server.add(docs);
            } else {
                logger.info("WARNING: indexing of a dataverse with no id attempted");
            }
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed dataverse " + dataverse.getId() + ":" + dataverse.getAlias();

    }

    public String indexDataset(Dataset dataset) {
        logger.info("indexing dataset " + dataset.getId());
        /**
         * @todo should we use solrDocIdentifierDataset or
         * IndexableObject.IndexableTypes.DATASET.getName() + "_" ?
         */
//        String solrIdPublished = solrDocIdentifierDataset + dataset.getId();
        String solrIdPublished = determinePublishedDatasetSolrDocId(dataset);
        String solrIdDraftDataset = IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.WORKING_COPY.getSuffix();
//        String solrIdDeaccessioned = IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.DEACCESSIONED.getSuffix();
        String solrIdDeaccessioned = determineDeaccesionedDatasetId(dataset);
        StringBuilder debug = new StringBuilder();
        debug.append("\ndebug:\n");
        int numReleasedVersions = 0;
        List<DatasetVersion> versions = dataset.getVersions();
        for (DatasetVersion datasetVersion : versions) {
            Long versionDatabaseId = datasetVersion.getId();
            String versionTitle = datasetVersion.getTitle();
            String semanticVersion = datasetVersion.getSemanticVersion();
            DatasetVersion.VersionState versionState = datasetVersion.getVersionState();
            if (versionState.equals(DatasetVersion.VersionState.RELEASED)) {
                /**
                 * @todo for performance, should just query this rather than
                 * iterating. Would need a new SQL query/method
                 */
                numReleasedVersions += 1;
            }
            debug.append("version found with database id " + versionDatabaseId + "\n");
            debug.append("- title: " + versionTitle + "\n");
            debug.append("- semanticVersion-VersionState: " + semanticVersion + "-" + versionState + "\n");
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            List<String> fileInfo = new ArrayList<>();
            for (FileMetadata fileMetadata : fileMetadatas) {
                fileInfo.add(fileMetadata.getDataFile().getId() + ":" + fileMetadata.getLabel());
            }
            int numFiles = 0;
            if (fileMetadatas != null) {
                numFiles = fileMetadatas.size();
            }
            debug.append("- files: " + numFiles + " " + fileInfo.toString() + "\n");
        }
        DatasetVersion latestVersion = dataset.getLatestVersion();
        String latestVersionStateString = latestVersion.getVersionState().name();
        DatasetVersion.VersionState latestVersionState = latestVersion.getVersionState();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        if (releasedVersion != null) {
            if (releasedVersion.getVersionState().equals(DatasetVersion.VersionState.DEACCESSIONED)) {
                DatasetVersion lookupAttempt2 = releasedVersion.getMostRecentlyReleasedVersion();
                String message = "WARNING: called dataset.getReleasedVersion() but version returned was deaccessioned (database id "
                        + releasedVersion.getId()
                        + "). (releasedVersion.getMostRecentlyReleasedVersion() returns database id "
                        + lookupAttempt2.getId() + " so that method may be better?). Look out for strange indexing results.";
                logger.severe(message);
                debug.append(message);
            }
        }
        Map<DatasetVersion.VersionState, Boolean> desiredCards = new LinkedHashMap<>();
        /**
         * @todo refactor all of this below and have a single method that takes
         * the map of desired cards (which correspond to Solr documents) as one
         * of the arguments and does all the operations necessary to achieve the
         * desired state.
         */
        StringBuilder results = new StringBuilder();
        if (numReleasedVersions == 0) {
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
                String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                results.append("Draft exists, no need for deaccessioned version. Deletion attempted for ")
                        .append(solrIdDeaccessioned).append(" (and files). Result: ").append(deleteDeaccessionedResult);

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                /**
                 * @todo delete published?
                 */
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
                 * {responseHeader={status=0,QTime=0}}Attempted to delete
                 * datafile_18_deaccessioned from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=0}}
                 */
                String result = getDesiredCardState(desiredCards) + results.toString() + debug.toString();
                logger.info(result);
                return result;
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, true);
                IndexableDataset indexableDeaccessionedVersion = new IndexableDataset(latestVersion);
                String indexDeaccessionedVersionResult = addOrUpdateDataset(indexableDeaccessionedVersion);
                results.append("No draft version. Attempting to index as deaccessioned. Result: ").append(indexDeaccessionedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                String deletePublishedResults = removePublished(dataset);
                results.append("No published version. Attempting to delete traces of published version from index. Result: ").append(deletePublishedResults);

                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
                /**
                 * @todo delete drafts?
                 */
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
                 * {responseHeader={status=0,QTime=1}}Attempted to delete
                 * datafile_18 from Solr index. updateReponse was:
                 * {responseHeader={status=0,QTime=0}}
                 */
                String result = getDesiredCardState(desiredCards) + results.toString() + debug.toString();
                logger.info(result);
                return result;
            } else {
                return "No-op. Unexpected condition reached: No released version and latest version is neither draft nor deaccesioned";
            }
        } else if (numReleasedVersions > 0) {
            results.append("Released versions found: ").append(numReleasedVersions)
                    .append(". Will attempt to index as ").append(solrIdPublished).append(" (discoverable by anonymous)\n");
            if (latestVersionState.equals(DatasetVersion.VersionState.RELEASED)
                    || latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {

                desiredCards.put(DatasetVersion.VersionState.RELEASED, true);
                IndexableDataset indexableReleasedVersion = new IndexableDataset(releasedVersion);
                String indexReleasedVersionResult = addOrUpdateDataset(indexableReleasedVersion);
                results.append("Attempted to index " + solrIdPublished).append(". Result: ").append(indexReleasedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
                List<String> solrDocIdsForDraftFilesToDelete = findSolrDocIdsForDraftFilesToDelete(dataset);
                String deleteDraftDatasetVersionResult = removeSolrDocFromIndex(solrIdDraftDataset);
                StringBuilder deleteDraftFilesResults = new StringBuilder();
                for (String doomed : solrDocIdsForDraftFilesToDelete) {
                    String result = removeSolrDocFromIndex(doomed);
                    deleteDraftFilesResults.append(result);
                }
                results.append("The latest version is published. Attempting to delete drafts. Result: ")
                        .append(deleteDraftDatasetVersionResult).append(deleteDraftFilesResults).append("\n");

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                results.append("No need for deaccessioned version. Deletion attempted for ")
                        .append(solrIdDeaccessioned).append(". Result: ").append(deleteDeaccessionedResult);
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
                logger.info(result);
                return result;
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
                String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                results.append("No need for deaccessioned version. Deletion attempted for ")
                        .append(solrIdDeaccessioned).append(". Result: ").append(deleteDeaccessionedResult);
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
                logger.info(result);
                return result;
            } else {
                return "No-op. Unexpected condition reached: There is at least one published version but the latest version is neither published nor draft";
            }
        } else {
            return "No-op. Unexpected condition reached: Negative number of released versions? Count was: " + numReleasedVersions;
        }
    }

    private String addOrUpdateDataset(IndexableDataset indexableDataset) {
        IndexableDataset.DatasetState state = indexableDataset.getDatasetState();
        Dataset dataset = indexableDataset.getDatasetVersion().getDataset();
        logger.info("adding or updating Solr document for dataset id " + dataset.getId());
        Collection<SolrInputDocument> docs = new ArrayList<>();
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = new ArrayList<>();
        try {
            dataverseSegments = findPathSegments(dataset.getOwner(), dataversePathSegmentsAccumulator);
        } catch (Exception ex) {
            logger.info("failed to find dataverseSegments for dataversePaths for " + SearchFields.SUBTREE + ": " + ex);
        }
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        String datasetSolrDocId = indexableDataset.getSolrDocId();
        solrInputDocument.addField(SearchFields.ID, datasetSolrDocId);
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataset.getId());
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
                // uncomment this if we change our mind and want a deaccessioned facet after all
//                solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DEACCESSIONED_STRING);
            }
            Date createDate = dataset.getCreateDate();
            if (createDate != null) {
                if (true) {
                    String msg = "can't find major release date, using create date: " + createDate;
                    logger.info(msg);
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
            solrInputDocument.addField(SearchFields.PERMS, publicGroupString);
        } else if (state.equals(indexableDataset.getDatasetState().WORKING_COPY)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DRAFT_STRING);
        }

        DataverseUser creator = dataset.getCreator();
        if (creator != null) {
            solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + creator.getId());
            /**
             * @todo: replace this fake version of granting users access to
             * dataverses with the real thing, when it's available in the app
             */
            if (creator.getUserName().equals("pete")) {
                // figure out if cathy is around
                DataverseUser cathy = dataverseUserServiceBean.findByUserName("cathy");
                if (cathy != null) {
                    // let cathy see all of pete's dataverses
                    solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + cathy.getId());
                }
            }
        }

        /**
         * @todo: remove this fake "has access to all data" group
         */
        solrInputDocument.addField(SearchFields.PERMS, groupPrefix + tmpNsaGroupId);

        addDatasetReleaseDateToSolrDoc(solrInputDocument, dataset);

        DatasetVersion datasetVersion = indexableDataset.getDatasetVersion();
        String parentDatasetTitle = "TBD";
        if (datasetVersion != null) {

            solrInputDocument.addField(SearchFields.DATASET_VERSION_ID, datasetVersion.getId());

            for (DatasetField dsf : datasetVersion.getFlatDatasetFields()) {

                DatasetFieldType dsfType = dsf.getDatasetFieldType();
                String solrFieldSearchable = dsfType.getSolrField().getNameSearchable();
                String solrFieldFacetable = dsfType.getSolrField().getNameFacetable();

                if (dsf.getValues() != null && !dsf.getValues().isEmpty() && dsf.getValues().get(0) != null && solrFieldSearchable != null) {
                    logger.fine("indexing " + dsf.getDatasetFieldType().getName() + ":" + dsf.getValues() + " into " + solrFieldSearchable + " and maybe " + solrFieldFacetable);
//                    if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.INTEGER)) {
                    if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.DATE)) {
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
                            solrInputDocument.addField(SearchFields.AFFILIATION, dsf.getValues());
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
                                solrInputDocument.addField(solrFieldSearchable, controlledVocabularyValue.getStrValue());
                                if (dsfType.getSolrField().isFacetable()) {
                                    solrInputDocument.addField(solrFieldFacetable, controlledVocabularyValue.getStrValue());
                                }
                            }
                        } else {
                            solrInputDocument.addField(solrFieldSearchable, dsf.getValues());
                            if (dsfType.getSolrField().isFacetable()) {
                                solrInputDocument.addField(solrFieldFacetable, dsf.getValues());
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

        docs.add(solrInputDocument);

        List<String> filesIndexed = new ArrayList<>();
        if (datasetVersion != null) {
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            for (FileMetadata fileMetadata : fileMetadatas) {
                SolrInputDocument datafileSolrInputDocument = new SolrInputDocument();
                Long fileEntityId = fileMetadata.getDataFile().getId();
                datafileSolrInputDocument.addField(SearchFields.ENTITY_ID, fileEntityId);
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
                            logger.info("problem with filename '" + filenameComplete + "': no extension? empty string as filename?");
                            filenameWithoutExtension = filenameComplete;
                        }
                        filenameCompleteFinal = filenameComplete;
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
                        logger.info("indexing file with filePublicationTimestamp. " + fileMetadata.getId() + " (file id " + datafile.getId() + ")");
                        Timestamp filePublicationTimestamp = datafile.getPublicationDate();
                        if (filePublicationTimestamp != null) {
                            fileSortByDate = filePublicationTimestamp;
                        } else {
                            String msg = "filePublicationTimestamp was null for fileMetadata id " + fileMetadata.getId() + " (file id " + datafile.getId() + ")";
                            logger.info(msg);
                        }
                    } else {
                        logger.info("indexing file with fileCreateTimestamp. " + fileMetadata.getId() + " (file id " + datafile.getId() + ")");
                        Timestamp fileCreateTimestamp = datafile.getCreateDate();
                        if (fileCreateTimestamp != null) {
                            fileSortByDate = fileCreateTimestamp;
                        } else {
                            String msg = "fileCreateTimestamp was null for fileMetadata id " + fileMetadata.getId() + " (file id " + datafile.getId() + ")";
                            logger.info(msg);
                        }
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

                if (majorVersionReleaseDate == null) {
                    datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
                }

                String fileSolrDocId = solrDocIdentifierFile + fileEntityId;
                if (indexableDataset.getDatasetState().equals(indexableDataset.getDatasetState().PUBLISHED)) {
                    fileSolrDocId = solrDocIdentifierFile + fileEntityId;
                    datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
                    datafileSolrInputDocument.addField(SearchFields.PERMS, publicGroupString);
                    addDatasetReleaseDateToSolrDoc(datafileSolrInputDocument, dataset);
                } else if (indexableDataset.getDatasetState().equals(indexableDataset.getDatasetState().WORKING_COPY)) {
                    fileSolrDocId = solrDocIdentifierFile + fileEntityId + indexableDataset.getDatasetState().getSuffix();
                    datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DRAFT_STRING);
                }
                datafileSolrInputDocument.addField(SearchFields.ID, fileSolrDocId);

                if (creator != null) {
                    datafileSolrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + creator.getId());
                    /**
                     * @todo: replace this fake version of granting users access
                     * to dataverses with the real thing, when it's available in
                     * the app
                     */
                    if (creator.getUserName().equals("pete")) {
                        // figure out if cathy is around
                        DataverseUser cathy = dataverseUserServiceBean.findByUserName("cathy");
                        if (cathy != null) {
                            // let cathy see all of pete's dataverses
                            datafileSolrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + cathy.getId());
                        }
                    }
                }

                /**
                 * @todo: remove this fake "has access to all data" group
                 */
                datafileSolrInputDocument.addField(SearchFields.PERMS, groupPrefix + tmpNsaGroupId);

                // For the mime type, we are going to index the "friendly" version, e.g., 
                // "PDF File" instead of "application/pdf", "MS Excel" instead of 
                // "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" (!), etc., 
                // if available:
                datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_MIME, fileMetadata.getDataFile().getFriendlyType());
                datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, fileMetadata.getDataFile().getFriendlyType());
                // For the file type facets, we have a property file that maps mime types 
                // to facet-friendly names; "application/fits" should become "FITS", etc.:
                datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, FileUtil.getFacetFileType(fileMetadata.getDataFile()));
                datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, FileUtil.getFacetFileType(fileMetadata.getDataFile()));
                datafileSolrInputDocument.addField(SearchFields.DESCRIPTION, fileMetadata.getDescription());
                datafileSolrInputDocument.addField(SearchFields.FILE_DESCRIPTION, fileMetadata.getDescription());
                datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
//            datafileSolrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataFile.getOwner().getOwner().getName());
                // datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, dataFile.getDataset().getTitle());
                datafileSolrInputDocument.addField(SearchFields.PARENT_ID, fileMetadata.getDataFile().getOwner().getId());

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
                }

                if (indexableDataset.isFilesShouldBeIndexed()) {
                    filesIndexed.add(fileSolrDocId);
                    docs.add(datafileSolrInputDocument);
                }
            }
        }

        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            server.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

//        return "indexed dataset " + dataset.getId() + " as " + solrDocId + "\nindexFilesResults for " + solrDocId + ":" + fileInfo.toString();
        return "indexed dataset " + dataset.getId() + " as " + datasetSolrDocId + ". filesIndexed: " + filesIndexed;
    }

    public String indexGroup(Map.Entry<Long, String> group) {

        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();

        String id = groupPrefix + group.getKey();

        solrInputDocument.addField(SearchFields.TYPE, "groups");
        solrInputDocument.addField(SearchFields.ID, id);
        solrInputDocument.addField(SearchFields.ENTITY_ID, group.getKey());
        solrInputDocument.addField(SearchFields.NAME_SORT, group.getValue());
        solrInputDocument.addField(SearchFields.GROUPS, id);

        docs.add(solrInputDocument);
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            server.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed group " + group;
    }

    public String indexUser(DataverseUser user) {

        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();

        String userid = groupPerUserPrefix + user.getId();
        if (user.isGuest()) {
            userid = publicGroupString;
        }

        solrInputDocument.addField(SearchFields.TYPE, "groups");
        solrInputDocument.addField(SearchFields.ID, userid);
        solrInputDocument.addField(SearchFields.ENTITY_ID, user.getId());
        solrInputDocument.addField(SearchFields.NAME_SORT, user.getUserName());
        solrInputDocument.addField(SearchFields.GROUPS, userid);

        docs.add(solrInputDocument);
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            server.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed user " + user.getId() + ":" + user.getUserName();
    }

    public List<String> findPathSegments(Dataverse dataverse, List<String> segments) {
        Dataverse rootDataverse = findRootDataverseCached();
        if (!dataverse.equals(rootDataverse)) {
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                findPathSegments(dataverse.getOwner(), segments);
            }
            segments.add(dataverse.getAlias());
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

    public static Long getTmpNsaGroupId() {
        return tmpNsaGroupId;
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

    public static String getDEACCESSIONED_STRING() {
        return DEACCESSIONED_STRING;
    }

    public String delete(Dataverse doomed) {
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
        logger.info("deleting Solr document for dataverse " + doomed.getId());
        UpdateResponse updateResponse;
        try {
            updateResponse = server.deleteById(solrDocIdentifierDataverse + doomed.getId());
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Successfully deleted dataverse " + doomed.getId() + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.info(response);
        return response;
    }

    public String removeSolrDocFromIndex(String doomed) {
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
        logger.info("deleting Solr document: " + doomed);
        UpdateResponse updateResponse;
        try {
            updateResponse = server.deleteById(doomed);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Attempted to delete " + doomed + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.info(response);
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
        Long datasetId = datasetWithDraftFilesToDelete.getId();
        SolrServer solrServer = new HttpSolrServer("http://localhost:8983/solr");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.setQuery(SearchFields.PARENT_ID + ":" + datasetId);
        solrQuery.addFilterQuery(SearchFields.ID + ":" + "*" + draftSuffix);
        List<String> solrIdsOfFilesToDelete = new ArrayList<>();
        try {
            // i.e. rows=2147483647&q=parentid%3A16&fq=id%3A*_draft
            logger.info("passing this Solr query to find draft files to delete: " + solrQuery);
            QueryResponse queryResponse = solrServer.query(solrQuery);
            SolrDocumentList results = queryResponse.getResults();
            for (SolrDocument solrDocument : results) {
                String id = (String) solrDocument.getFieldValue(SearchFields.ID);
                if (id != null) {
                    solrIdsOfFilesToDelete.add(id);
                }
            }
        } catch (SolrServerException ex) {
            logger.info("error in findSolrDocIdsForDraftFilesToDelete method: " + ex.toString());
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
        StringBuilder deleteMultipleResult = new StringBuilder();
        for (String doomed : docIds) {
            String result = removeSolrDocFromIndex(doomed);
            deleteMultipleResult.append(result);
        }
        return deleteMultipleResult.toString();
    }

    private String determinePublishedDatasetSolrDocId(Dataset dataset) {
        return IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.PUBLISHED.getSuffix();
    }

    private String determineDeaccesionedDatasetId(Dataset dataset) {
        return IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.DEACCESSIONED.getSuffix();
    }

    private String removeDeaccessioned(Dataset dataset) {
        StringBuilder result = new StringBuilder();
        String deleteDeaccessionedResult = removeSolrDocFromIndex(determineDeaccesionedDatasetId(dataset));
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

}
