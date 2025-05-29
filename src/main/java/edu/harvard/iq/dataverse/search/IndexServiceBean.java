package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetFieldValueValidator;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.DatasetVersionFilesServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObject.DType;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.Retention;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataAccessRequest;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataUtil;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.search.IndexableDataset.DatasetState;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import static jakarta.ejb.TransactionAttributeType.REQUIRES_NEW;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.xml.sax.ContentHandler;

@Stateless
@Named
public class IndexServiceBean {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());
    private static final Config config = ConfigProvider.getConfig();

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
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
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    SolrClientService solrClientService; // only for query index on Solr
    @EJB
    SolrClientIndexService solrClientIndexService; // only for add, update, or remove index on Solr
    @EJB
    DataFileServiceBean dataFileService;

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @Inject
    DatasetVersionFilesServiceBean datasetVersionFilesServiceBean;

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
    private Dataverse rootDataverseCached;

    private VariableMetadataUtil variableMetadataUtil;

    @TransactionAttribute(REQUIRES_NEW)
    public Future<String> indexDataverseInNewTransaction(Dataverse dataverse) throws SolrServerException, IOException{
        return indexDataverse(dataverse, false);
    }
    
    public Future<String> indexDataverse(Dataverse dataverse) throws SolrServerException, IOException {
       return  indexDataverse(dataverse, true);
    }

    public Future<String> indexDataverse(Dataverse dataverse, boolean processPaths) throws SolrServerException, IOException {
        logger.fine("indexDataverse called on dataverse id " + dataverse.getId() + "(" + dataverse.getAlias() + ")");
        if (dataverse.getId() == null) {
            // TODO: Investigate the root cause of this "unable to index dataverse"
            // error showing up in the logs. Try running the API test suite?
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
        solrInputDocument.addField(SearchFields.DATAVERSE_ALIAS, dataverse.getAlias());
        solrInputDocument.addField(SearchFields.DATAVERSE_CATEGORY, dataverse.getIndexableCategoryName());
        if (dataverse.isReleased()) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
            if (FeatureFlags.ADD_PUBLICOBJECT_SOLR_FIELD.enabled()) {
                solrInputDocument.addField(SearchFields.PUBLIC_OBJECT, true);
            }
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getPublicationDate());
        } else {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getCreateDate());
        }
        /* We don't really have harvested dataverses yet; 
           (I have in fact just removed the isHarvested() method from the Dataverse object) -- L.A.
        if (dataverse.isHarvested()) {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, true);
            solrInputDocument.addField(SearchFields.SOURCE, HARVESTED);
        } else { (this means that all dataverses are "local" - should this be removed? */
        solrInputDocument.addField(SearchFields.IS_HARVESTED, false);
        solrInputDocument.addField(SearchFields.METADATA_SOURCE, rootDataverse.getName()); //rootDataverseName);
        /*}*/

        addDataverseReleaseDateToSolrDoc(solrInputDocument, dataverse);
        // if (dataverse.getOwner() != null) {
        // solrInputDocument.addField(SearchFields.HOST_DATAVERSE,
        // dataverse.getOwner().getName());
        // }
        solrInputDocument.addField(SearchFields.DESCRIPTION, StringUtil.html2text(dataverse.getDescription()));
        solrInputDocument.addField(SearchFields.DATAVERSE_DESCRIPTION, StringUtil.html2text(dataverse.getDescription()));
        // logger.info("dataverse affiliation: " + dataverse.getAffiliation());
        if (dataverse.getAffiliation() != null && !dataverse.getAffiliation().isEmpty()) {
            /**
             * @todo: stop using affiliation as category
             */
            // solrInputDocument.addField(SearchFields.CATEGORY,
            // dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.AFFILIATION, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.DATAVERSE_AFFILIATION, dataverse.getAffiliation());
        }
        Set<String> langs = settingsService.getConfiguredLanguages();
        for (ControlledVocabularyValue dataverseSubject : dataverse.getDataverseSubjects()) {
            String subject = dataverseSubject.getStrValue();
            if (!subject.equals(DatasetField.NA_VALUE)) {
             // Index in all used languages (display and metadata languages
                for(String locale: langs) {
                    solrInputDocument.addField(SearchFields.DATAVERSE_SUBJECT, dataverseSubject.getLocaleStrValue(locale));
                }
                if (langs.isEmpty()) {
                    solrInputDocument.addField(SearchFields.DATAVERSE_SUBJECT, dataverseSubject.getStrValue());
                }

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
                solrInputDocument.addField(SearchFields.DATAVERSE_PARENT_ALIAS, dataverse.getOwner().getAlias());
            }
        }
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = findPathSegments(dataverse, dataversePathSegmentsAccumulator);
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        if (dataversePaths.size() > 0) {
            // removing the dataverse's own id from the paths
            dataversePaths.remove(dataversePaths.size() - 1);
        }

        //Add paths for my linking dataverses
        List<Dataverse> linkingDataverses = findAllLinkingDataverses(dataverse);
        List<String> linkingDataversePaths = findLinkingDataversePaths(linkingDataverses);
        for (String dvPath:linkingDataversePaths ){
            dataversePaths.add(dvPath);
        }
        //only do this if we're indexing an individual dataverse ie not full re-index
        List<Long> dataverseChildrenIds = new ArrayList();
        List<Long> datasetChildrenIds = new ArrayList();
        if (processPaths) {
            //Get Linking Dataverses to see if I need to reindex my children
            if (hasAnyLinkingDataverses(dataverse)) {
                dataverseChildrenIds = dataverseService.findAllDataverseDataverseChildren(dataverse.getId());
                datasetChildrenIds = dataverseService.findAllDataverseDatasetChildren(dataverse.getId());
                for (Long id : datasetChildrenIds) {
                    updatePathForExistingSolrDocs(datasetService.find(id));
                }

                for (Long id : dataverseChildrenIds) {
                    updatePathForExistingSolrDocs(dataverseService.find(id));
                }
            }
        }
        
        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
        docs.add(solrInputDocument);

        String status;
        try {
            if (dataverse.getId() != null) {
                solrClientIndexService.getSolrClient().add(docs);
            } else {
                logger.info("WARNING: indexing of a dataverse with no id attempted");
            }
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
    public void indexDatasetInNewTransaction(Long datasetId) { //Dataset dataset) {
        boolean doNormalSolrDocCleanUp = false;
        asyncIndexDataset(datasetId, doNormalSolrDocCleanUp);
    }
    
    // The following two variables are only used in the synchronized getNextToIndex method and do not need to be synchronized themselves

    // nextToIndex contains datasets mapped by dataset id that were added for future indexing while the indexing was already ongoing for a given dataset
    // (if there already was a dataset scheduled for indexing, it is overwritten and only the most recently requested version is kept in the map)
    private static final Map<Long, Dataset> NEXT_TO_INDEX = new ConcurrentHashMap<>();
    // indexingNow is a set of dataset ids of datasets being indexed asynchronously right now
    private static final Map<Long, Boolean> INDEXING_NOW = new ConcurrentHashMap<>();
    // semaphore for async indexing
    private static final Semaphore ASYNC_INDEX_SEMAPHORE = new Semaphore(JvmSettings.MAX_ASYNC_INDEXES.lookupOptional(Integer.class).orElse(4), true);
    
    @Inject
    @Metric(name = "index_permit_wait_time", absolute = true, unit = MetricUnits.NANOSECONDS,
            description = "Displays how long does it take to receive a permit to index a dataset")
    Timer indexPermitWaitTimer;
    
    @Inject
    @Metric(name = "index_time", absolute = true, unit = MetricUnits.NANOSECONDS,
            description = "Displays how long does it take to index a dataset")
    Timer indexTimer;
    
    /**
     * Try to acquire a permit from the semaphore avoiding too many parallel indexes, potentially overwhelming Solr.
     * This method will time the duration waiting for the permit, allowing indexing performance to be measured.
     * @throws InterruptedException
     */
    private void acquirePermitFromSemaphore() throws InterruptedException {
        try (var timeContext = indexPermitWaitTimer.time()) {
            ASYNC_INDEX_SEMAPHORE.acquire();
        }
    }

    // When you pass null as Dataset parameter to this method, it indicates that the indexing of the dataset with "id" has finished
    // Pass non-null Dataset to schedule it for indexing
    synchronized private static Dataset getNextToIndex(Long id, Dataset d) {
        if (d == null) { // -> indexing of the dataset with id has finished
            Dataset next = NEXT_TO_INDEX.remove(id);
            if (next == null) { // -> no new indexing jobs were requested while indexing was ongoing
                // the job can be stopped now
                INDEXING_NOW.remove(id);
            }
            return next;
        }
        // index job is requested for a non-null dataset
        if (INDEXING_NOW.containsKey(id)) { // -> indexing job is already ongoing, and a new job should not be started by the current thread -> return null
            NEXT_TO_INDEX.put(id, d);
            return null;
        }
        // otherwise, start a new job
        INDEXING_NOW.put(id, true);
        return d;
    }

    /**
     * Indexes a dataset asynchronously.
     * 
     * Note that this method implement a synchronized skipping mechanism. When an
     * indexing job is already running for a given dataset in the background, the
     * new call will not index that dataset, but will delegate the execution to
     * the already running job. The running job will pick up the requested indexing
     * once that it is finished with the ongoing indexing. If another indexing is
     * requested before the ongoing indexing is finished, only the indexing that is
     * requested most recently will be picked up for the next indexing.
     * 
     * In other words: we can have at most one indexing ongoing for the given
     * dataset, and at most one (most recent) request for reindexing of the same
     * dataset. All requests that come between the most recent one and the ongoing
     * one are skipped for the optimization reasons. For a more in depth discussion,
     * see the pull request: https://github.com/IQSS/dataverse/pull/9558
     * 
     * @param dataset                The dataset to be indexed.
     * @param doNormalSolrDocCleanUp Flag for normal Solr doc clean up.
     */
    @Asynchronous
    public void asyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
        try {
            acquirePermitFromSemaphore();
            doAsyncIndexDataset(dataset, doNormalSolrDocCleanUp);
        } catch (InterruptedException e) {
            String failureLogText = "Indexing failed: interrupted. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);
        } finally {
            ASYNC_INDEX_SEMAPHORE.release();
        }
    }

    @Asynchronous
    public void asyncIndexDataset(Long datasetId, boolean doNormalSolrDocCleanUp) {
        //Initialize dataset here for logging (LoggingUtil) purposes
        Dataset dataset = null;
        try {
            acquirePermitFromSemaphore();
            dataset = datasetService.find(datasetId);
            doAsyncIndexDataset(dataset, doNormalSolrDocCleanUp);
        } catch (InterruptedException e) {
            String failureLogText = "Indexing failed: interrupted. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + datasetId.toString();
            failureLogText += "\r\n" + e.getLocalizedMessage();
            if(dataset==null) {
                dataset = new Dataset();
                dataset.setId(datasetId);
            }
            LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);
        } finally {
            ASYNC_INDEX_SEMAPHORE.release();
        }
    }
    private void doAsyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
        Long id = dataset.getId();
        Dataset next = getNextToIndex(id, dataset); // if there is an ongoing index job for this dataset, next is null (ongoing index job will reindex the newest version after current indexing finishes)
        while (next != null) {
            // Time context will automatically start on creation and stop when leaving the try block
            try (var timeContext = indexTimer.time()) {
                indexDataset(next, doNormalSolrDocCleanUp);
            } catch (Exception e) { // catch all possible exceptions; otherwise when something unexpected happes the dataset wold remain locked and impossible to reindex
                String failureLogText = "Indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
                failureLogText += "\r\n" + e.getLocalizedMessage();
                LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);
            }
            next = getNextToIndex(id, null); // if dataset was not changed during the indexing (and no new job was requested), next is null and loop can be stopped
        }
    }

    @Asynchronous
    public void asyncIndexDatasetList(List<Dataset> datasets, boolean doNormalSolrDocCleanUp) {
        for(Dataset dataset : datasets) {
            try {
                acquirePermitFromSemaphore();
                doAsyncIndexDataset(dataset, true);
            } catch (InterruptedException e) {
                String failureLogText = "Indexing failed: interrupted. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
                failureLogText += "\r\n" + e.getLocalizedMessage();
                LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);
            } finally {
                ASYNC_INDEX_SEMAPHORE.release();
            }
        }
    }
    
    public void indexDvObject(DvObject objectIn) throws  SolrServerException, IOException {
        if (objectIn.isInstanceofDataset() ){
            asyncIndexDataset((Dataset)objectIn, true);
        } else if (objectIn.isInstanceofDataverse() ){
            indexDataverse((Dataverse)objectIn);
        }
    }

    public void indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) throws  SolrServerException, IOException {
        doIndexDataset(dataset, doNormalSolrDocCleanUp);
        updateLastIndexedTime(dataset.getId());
    }
    
    private void doIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) throws  SolrServerException, IOException {
        logger.fine("indexing dataset " + dataset.getId());
        /**
         * @todo should we use solrDocIdentifierDataset or
         * IndexableObject.IndexableTypes.DATASET.getName() + "_" ?
         */
        String solrIdPublished = determinePublishedDatasetSolrDocId(dataset);
        String solrIdDraftDataset = IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.WORKING_COPY.getSuffix();
        String solrIdDeaccessioned = determineDeaccessionedDatasetId(dataset);
        StringBuilder debug = new StringBuilder();
        debug.append("\ndebug:\n");
        boolean reduceSolrDeletes = FeatureFlags.REDUCE_SOLR_DELETES.enabled();
        if (!reduceSolrDeletes) {
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
        }
        DatasetVersion latestVersion = dataset.getLatestVersion();
        DatasetVersion.VersionState latestVersionState = latestVersion.getVersionState();
        String latestVersionStateString = latestVersionState.name();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        boolean atLeastOnePublishedVersion = false;
        if (releasedVersion != null) {
            atLeastOnePublishedVersion = true;
        }
        if (reduceSolrDeletes) {
            List<String> solrIdsOfDocsToDelete = null;
            if (logger.isLoggable(Level.FINE)) {
                writeDebugInfo(debug, dataset);
            }
            if (doNormalSolrDocCleanUp) {
                try {
                    solrIdsOfDocsToDelete = findFilesOfParentDataset(dataset.getId());
                    logger.fine("Existing file docs: " + String.join(", ", solrIdsOfDocsToDelete));
                    if (!solrIdsOfDocsToDelete.isEmpty()) {
                        // We keep the latest version's docs unless it is deaccessioned and there is no
                        // published/released version
                        // So skip the loop removing those docs from the delete list except in that case
                        if ((!latestVersion.isDeaccessioned() || atLeastOnePublishedVersion)) {
                            List<FileMetadata> latestFileMetadatas = latestVersion.getFileMetadatas();
                            String suffix = (new IndexableDataset(latestVersion)).getDatasetState().getSuffix();
                            for (FileMetadata fileMetadata : latestFileMetadatas) {
                                String solrIdOfPublishedFile = solrDocIdentifierFile
                                        + fileMetadata.getDataFile().getId() + suffix;
                                solrIdsOfDocsToDelete.remove(solrIdOfPublishedFile);
                            }
                        }
                        if (releasedVersion != null && !releasedVersion.equals(latestVersion)) {
                            List<FileMetadata> releasedFileMetadatas = releasedVersion.getFileMetadatas();
                            for (FileMetadata fileMetadata : releasedFileMetadatas) {
                                String solrIdOfPublishedFile = solrDocIdentifierFile
                                        + fileMetadata.getDataFile().getId();
                                solrIdsOfDocsToDelete.remove(solrIdOfPublishedFile);
                            }
                        }
                    }
                    // Clear any unused dataset docs
                    if (!latestVersion.isDraft()) {
                        // The latest version is released, so should delete any draft docs for the
                        // dataset
                        solrIdsOfDocsToDelete.add(solrIdDraftDataset);
                    }
                    if (!atLeastOnePublishedVersion) {
                        // There's no released version, so should delete any normal state docs for the
                        // dataset
                        solrIdsOfDocsToDelete.add(solrIdPublished);
                    }
                    if (atLeastOnePublishedVersion || !latestVersion.isDeaccessioned()) {
                        // There's a released version or a draft, so should delete any deaccessioned
                        // state docs for the dataset
                        solrIdsOfDocsToDelete.add(solrIdDeaccessioned);
                    }
                } catch (SearchException | NullPointerException ex) {
                    logger.fine("could not run search of files to delete: " + ex);
                }
                logger.fine("Solr docs to delete: " + String.join(", ", solrIdsOfDocsToDelete));

                if (!solrIdsOfDocsToDelete.isEmpty()) {
                    List<String> solrIdsOfPermissionDocsToDelete = new ArrayList<>();
                    for (String file : solrIdsOfDocsToDelete) {
                        // Also remove associated permission docs
                        solrIdsOfPermissionDocsToDelete.add(file + discoverabilityPermissionSuffix);
                    }
                    solrIdsOfDocsToDelete.addAll(solrIdsOfPermissionDocsToDelete);
                    logger.fine("Solr docs and perm docs to delete: " + String.join(", ", solrIdsOfDocsToDelete));

                    IndexResponse resultOfAttemptToPremptivelyDeletePublishedFiles = solrIndexService
                            .deleteMultipleSolrIds(solrIdsOfDocsToDelete);
                    debug.append("result of attempt to premptively deleted published files before reindexing: "
                            + resultOfAttemptToPremptivelyDeletePublishedFiles + "\n");
                }
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
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
                    String deleteDeaccessionedResult = removeDeaccessioned(dataset);
                    results.append("Draft exists, no need for deaccessioned version. Deletion attempted for ")
                            .append(solrIdDeaccessioned).append(" (and files). Result: ")
                            .append(deleteDeaccessionedResult).append("\n");
                }

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
                    String deletePublishedResults = removePublished(dataset);
                    results.append("No published version. Attempting to delete traces of published version from index. Result: ")
                            .append(deletePublishedResults).append("\n");
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
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, true);
                IndexableDataset indexableDeaccessionedVersion = new IndexableDataset(latestVersion);
                String indexDeaccessionedVersionResult = addOrUpdateDataset(indexableDeaccessionedVersion);
                results.append("No draft version. Attempting to index as deaccessioned. Result: ").append(indexDeaccessionedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
                    String deletePublishedResults = removePublished(dataset);
                    results.append("No published version. Attempting to delete traces of published version from index. Result: ").append(deletePublishedResults).append("\n");
                }

                desiredCards.put(DatasetVersion.VersionState.DRAFT, false);
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
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
            } else {
                String result = "No-op. Unexpected condition reached: No released version and latest version is neither draft nor deaccessioned";
                logger.fine(result);
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
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
                    List<String> solrDocIdsForDraftFilesToDelete = findSolrDocIdsForDraftFilesToDelete(dataset);
                    String deleteDraftDatasetVersionResult = removeSolrDocFromIndex(solrIdDraftDataset);
                    String deleteDraftFilesResults = deleteDraftFiles(solrDocIdsForDraftFilesToDelete);
                    results.append("The latest version is published. Attempting to delete drafts. Result: ")
                            .append(deleteDraftDatasetVersionResult).append(deleteDraftFilesResults).append("\n");
                }

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
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
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DRAFT)) {

                IndexableDataset indexableDraftVersion = new IndexableDataset(latestVersion);
                desiredCards.put(DatasetVersion.VersionState.DRAFT, true);
                Set<Long> datafilesInDraftVersion = new HashSet<>();
                for (FileMetadata fm : latestVersion.getFileMetadatas()) {
                    datafilesInDraftVersion.add(fm.getDataFile().getId());
                }


                desiredCards.put(DatasetVersion.VersionState.RELEASED, true);
                IndexableDataset indexableReleasedVersion = new IndexableDataset(releasedVersion);
                String indexReleasedVersionResult = addOrUpdateDataset(indexableReleasedVersion, datafilesInDraftVersion);
                results.append("There is a published version we will attempt to index. Result: ").append(indexReleasedVersionResult).append("\n");

                String indexDraftResult = addOrUpdateDataset(indexableDraftVersion);
                results.append("The latest version is a working copy (latestVersionState: ")
                        .append(latestVersionStateString).append(") and will be indexed as ")
                        .append(solrIdDraftDataset).append(" (limited visibility). Result: ").append(indexDraftResult).append("\n");
                
                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, false);
                if (!reduceSolrDeletes && doNormalSolrDocCleanUp) {
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
            } else {
                String result = "No-op. Unexpected condition reached: There is at least one published version but the latest version is neither published nor draft";
                logger.fine(result);
            }
        } else {
            String result = "No-op. Unexpected condition reached: Has a version been published or not?";
            logger.fine(result);
        }
    }
    
    private void writeDebugInfo(StringBuilder debug, Dataset dataset) {
        List<DatasetVersion> versions = dataset.getVersions();
        int numPublishedVersions = 0;
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
            List<String> fileInfo = new ArrayList<>();
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();

            for (FileMetadata fileMetadata : fileMetadatas) {
                /**
                 * It sounds weird but the first thing we'll do is preemptively delete the Solr
                 * documents of all published files. Don't worry, published files will be
                 * re-indexed later along with the dataset. We do this so users can delete files
                 * from published versions of datasets and then re-publish a new version without
                 * fear that their old published files (now deleted from the latest published
                 * version) will be searchable. See also
                 * https://github.com/IQSS/dataverse/issues/762
                 */
                fileInfo.add(fileMetadata.getDataFile().getId() + ":" + fileMetadata.getLabel());
            }
            int numFiles = 0;
            if (fileMetadatas != null) {
                numFiles = fileMetadatas.size();
            }
            debug.append("- files: " + numFiles + " " + fileInfo.toString() + "\n");
        }
        debug.append("numPublishedVersions: " + numPublishedVersions + "\n");
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

    private String addOrUpdateDataset(IndexableDataset indexableDataset) throws  SolrServerException, IOException {
        String result = addOrUpdateDataset(indexableDataset, null);
        return result;
    }

    public SolrInputDocuments toSolrDocs(IndexableDataset indexableDataset, Set<Long> datafilesInDraftVersion) throws  SolrServerException, IOException {
        IndexableDataset.DatasetState state = indexableDataset.getDatasetState();
        Dataset dataset = indexableDataset.getDatasetVersion().getDataset();
        logger.fine("adding or updating Solr document for dataset id " + dataset.getId());
        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        String datasetSolrDocId = indexableDataset.getSolrDocId();
        solrInputDocument.addField(SearchFields.ID, datasetSolrDocId);
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataset.getId());
        String dataverseVersion = systemConfig.getVersion();
        solrInputDocument.addField(SearchFields.DATAVERSE_VERSION_INDEXED_BY, dataverseVersion);
        solrInputDocument.addField(SearchFields.IDENTIFIER, dataset.getGlobalId().toString());
        solrInputDocument.addField(SearchFields.DATASET_PERSISTENT_ID, dataset.getGlobalId().toString());
        solrInputDocument.addField(SearchFields.PERSISTENT_URL, dataset.getPersistentURL());
        solrInputDocument.addField(SearchFields.TYPE, "datasets");
        solrInputDocument.addField(SearchFields.DATASET_VALID, indexableDataset.getDatasetVersion().isValid());

        final Dataverse dataverse = dataset.getDataverseContext();
        final String dvIndexableCategoryName = dataverse.getIndexableCategoryName();
        final String dvAlias = dataverse.getAlias();
        final String dvDisplayName = dataverse.getDisplayName();
        final String rdvName = findRootDataverseCached().getName();
        //This only grabs the immediate parent dataverse's category. We do the same for dataverses themselves.
        solrInputDocument.addField(SearchFields.CATEGORY_OF_DATAVERSE, dvIndexableCategoryName);
        solrInputDocument.addField(SearchFields.IDENTIFIER_OF_DATAVERSE, dvAlias);
        solrInputDocument.addField(SearchFields.DATAVERSE_NAME, dvDisplayName);

        Date datasetSortByDate;
        // For now, drafts are indexed using to their last update time, and published versions are indexed using their
        // most recent major version release date.
        // This means that newly created or edited drafts will show up on the top when sorting by newest, newly
        // published major versions will also show up on the top, and newly published minor versions will be shown
        // next to their corresponding major version.
        if (state.equals(DatasetState.WORKING_COPY)) {
            Date lastUpdateTime = indexableDataset.getDatasetVersion().getLastUpdateTime();
            if (lastUpdateTime != null) {
                logger.fine("using last update time of indexed dataset version: " + lastUpdateTime);
                datasetSortByDate = lastUpdateTime;
            } else {
                logger.fine("can't find last update time, using \"now\"");
                datasetSortByDate = new Date();
            }
        } else {
            Date majorVersionReleaseDate = dataset.getMostRecentMajorVersionReleaseDate();
            if (majorVersionReleaseDate != null) {
                logger.fine("major release date found: " + majorVersionReleaseDate.toString());
                datasetSortByDate = majorVersionReleaseDate;
            } else {
                Date createDate = dataset.getCreateDate();
                if (createDate != null) {
                    logger.fine("can't find major release date, using create date: " + createDate);
                    datasetSortByDate = createDate;
                } else {
                    logger.fine("can't find major release date or create date, using \"now\"");
                    datasetSortByDate = new Date();
                }
            }
        }
        solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, datasetSortByDate);

        if (state.equals(DatasetState.PUBLISHED)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
            if (FeatureFlags.ADD_PUBLICOBJECT_SOLR_FIELD.enabled()) {
                solrInputDocument.addField(SearchFields.PUBLIC_OBJECT, true);
            }
            // solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE,
            // dataset.getPublicationDate());
        } else if (state.equals(DatasetState.WORKING_COPY)) {
            if (dataset.getReleasedVersion() == null) {
                solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, UNPUBLISHED_STRING);
            }
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DRAFT_STRING);
        } else if (state.equals(IndexableDataset.DatasetState.DEACCESSIONED)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DEACCESSIONED_STRING);
        }

        addDatasetReleaseDateToSolrDoc(solrInputDocument, dataset);

        if (dataset.isHarvested()) {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, true);
            if (FeatureFlags.INDEX_HARVESTED_METADATA_SOURCE.enabled()) {
                // New - as of 6.3 - option of indexing the actual origin of 
                // harvested objects as the metadata source:
                solrInputDocument.addField(SearchFields.METADATA_SOURCE,
                                        dataset.getHarvestedFrom() != null ? dataset.getHarvestedFrom().getMetadataSource() : HARVESTED);
            } else {
                solrInputDocument.addField(SearchFields.METADATA_SOURCE, HARVESTED);
            }
        } else {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, false);
            solrInputDocument.addField(SearchFields.METADATA_SOURCE, rdvName); //rootDataverseName);
        }

        DatasetType datasetType = dataset.getDatasetType();
        solrInputDocument.addField(SearchFields.DATASET_TYPE, datasetType.getName());

        DatasetVersion datasetVersion = indexableDataset.getDatasetVersion();
        String parentDatasetTitle = "TBD";
        if (datasetVersion != null) {

            addLicenseToSolrDoc(solrInputDocument, datasetVersion);

            solrInputDocument.addField(SearchFields.DATASET_VERSION_ID, datasetVersion.getId());
            solrInputDocument.addField(SearchFields.DATASET_CITATION, datasetVersion.getCitation(false));
            solrInputDocument.addField(SearchFields.DATASET_CITATION_HTML, datasetVersion.getCitation(true));

            solrInputDocument.addField(SearchFields.FILE_COUNT, datasetVersionFilesServiceBean.getFileMetadataCount(datasetVersion));

            if (datasetVersion.isInReview()) {
                solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, IN_REVIEW_STRING);
            }
            if(datasetVersion.getExternalStatusLabel()!=null) {
                solrInputDocument.addField(SearchFields.EXTERNAL_STATUS, datasetVersion.getExternalStatusLabel());
            }

            Set<String> langs = settingsService.getConfiguredLanguages();
            Map<Long, JsonObject> cvocMap = datasetFieldService.getCVocConf(true);
            Map<Long, Set<String>> cvocManagedFieldMap = new HashMap<>();
            for (Map.Entry<Long, JsonObject> cvocEntry : cvocMap.entrySet()) {
                if(cvocEntry.getValue().containsKey("managed-fields")) {
                    JsonObject managedFields = cvocEntry.getValue().getJsonObject("managed-fields");
                    Set<String> managedFieldValues = new HashSet<>();
                    for (String s : managedFields.keySet()) {
                        managedFieldValues.add(managedFields.getString(s));
                    }
                    cvocManagedFieldMap.put(cvocEntry.getKey(), managedFieldValues);
                }
            }



            Set<String> metadataBlocksWithValue = new HashSet<>();
            for (DatasetField dsf : datasetVersion.getFlatDatasetFields()) {

                DatasetFieldType dsfType = dsf.getDatasetFieldType();
                String solrFieldSearchable = dsfType.getSolrField().getNameSearchable();
                String solrFieldFacetable = dsfType.getSolrField().getNameFacetable();

                if (dsf.getValues() != null && !dsf.getValues().isEmpty() && dsf.getValues().get(0) != null && solrFieldSearchable != null) {
                    // Index all metadata blocks that have a value - To show in new facet category SearchFields.METADATA_TYPES
                    if (dsfType.getMetadataBlock() != null) {
                        metadataBlocksWithValue.add(dsfType.getMetadataBlock().getName());
                    }

                    logger.fine("indexing " + dsf.getDatasetFieldType().getName() + ":" + dsf.getValues() + " into " + solrFieldSearchable + " and maybe " + solrFieldFacetable);
                    // if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.INTEGER))
                    // {
                    if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.EMAIL)) {
                        // no-op. we want to keep email address out of Solr per
                        // https://github.com/IQSS/dataverse/issues/759
                    } else if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.INTEGER)) {
                        // we need to filter invalid integer values, because otherwise the whole document will
                        // fail to be indexed
                        Pattern intPattern = Pattern.compile("^-?\\d+$");
                        List<String> indexableValues = dsf.getValuesWithoutNaValues().stream()
                                .filter(s -> intPattern.matcher(s).find())
                                .collect(Collectors.toList());
                        solrInputDocument.addField(solrFieldSearchable, indexableValues);
                        if (dsfType.getSolrField().isFacetable()) {
                            solrInputDocument.addField(solrFieldFacetable, indexableValues);
                        }
                    } else if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.FLOAT)) {
                        // same as for integer values, we need to filter invalid float values
                        List<String> indexableValues = dsf.getValuesWithoutNaValues().stream()
                                .filter(s -> {
                                    try {
                                        Double.parseDouble(s);
                                        return true;
                                    } catch (NumberFormatException e) {
                                        return false;
                                    }
                                })
                                .collect(Collectors.toList());
                        solrInputDocument.addField(solrFieldSearchable, indexableValues);
                        if (dsfType.getSolrField().isFacetable()) {
                            solrInputDocument.addField(solrFieldFacetable, indexableValues);
                        }
                    } else if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.DATE)) {
                        // Solr accepts dates in the ISO-8601 format, e.g. YYYY-MM-DDThh:mm:ssZ, YYYYY-MM-DD, YYYY-MM, YYYY
                        // See: https://solr.apache.org/guide/solr/latest/indexing-guide/date-formatting-math.html
                        // If dates have been entered in other formats, we need to skip or convert them
                        // TODO at the moment we are simply skipping, but converting them would offer more value for search
                        // For use in facets, we index only the year (YYYY)
                        String dateAsString = "";
                        if (!dsf.getValues_nondisplay().isEmpty()) {
                            dateAsString = dsf.getValues_nondisplay().get(0).trim();
                        }

                        logger.fine("date as string: " + dateAsString);

                        if (dateAsString != null && !dateAsString.isEmpty()) {
                            boolean dateValid = false;

                            DateTimeFormatter[] possibleFormats = {
                                    DateTimeFormatter.ISO_INSTANT,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                                    DateTimeFormatter.ofPattern("yyyy-MM"),
                                    DateTimeFormatter.ofPattern("yyyy")
                            };
                            for (DateTimeFormatter format : possibleFormats){
                                try {
                                    format.parse(dateAsString);
                                    dateValid = true;
                                } catch (DateTimeParseException e) {
                                    // no-op, date is invalid
                                }
                            }

                            if (!dateValid) {
                                logger.fine("couldn't index " + dsf.getDatasetFieldType().getName() + ":" + dsf.getValues() + " because it's not a valid date format according to Solr");
                            } else {
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
                                    // solrInputDocument.addField(solrFieldSearchable,
                                    // Integer.parseInt(datasetFieldFlaggedAsDate));
                                    solrInputDocument.addField(solrFieldSearchable, dateAsString);
                                    if (dsfType.getSolrField().isFacetable()) {
                                        // solrInputDocument.addField(solrFieldFacetable,
                                        // Integer.parseInt(datasetFieldFlaggedAsDate));
                                        solrInputDocument.addField(solrFieldFacetable, datasetFieldFlaggedAsDate);
                                    }
                                } catch (Exception ex) {
                                    logger.info("unable to convert " + dateAsString + " into YYYY format and couldn't index it (" + dsfType.getName() + ")");
                                }
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
                            // datasets have titles not names but index title under name as well so we can
                            // sort datasets by name along dataverses and files
                            List<String> possibleTitles = dsf.getValues();
                            String firstTitle = possibleTitles.get(0);
                            if (firstTitle != null) {
                                parentDatasetTitle = firstTitle;
                            }
                            solrInputDocument.addField(SearchFields.NAME_SORT, dsf.getValues());
                        }

                        // If there is a CVOCConf for the field
                        if(cvocMap.containsKey(dsfType.getId())) {
                            List<String> vals = dsf.getValues_nondisplay();
                            Set<String> searchStrings = new HashSet<>();
                            for (String val: vals) {
                                searchStrings.add(val);
                                // Try to get string values from externalvocabularyvalue using val as termUri
                                searchStrings.addAll(datasetFieldService.getIndexableStringsByTermUri(val, cvocMap.get(dsfType.getId()), dsfType.getName()));

                                if(dsfType.getParentDatasetFieldType()!=null) {
                                    List<DatasetField> childDatasetFields = dsf.getParentDatasetFieldCompoundValue().getChildDatasetFields();
                                    for (DatasetField df : childDatasetFields) {
                                        if(cvocManagedFieldMap.containsKey(dsfType.getId()) && cvocManagedFieldMap.get(dsfType.getId()).contains(df.getDatasetFieldType().getName())) {
                                            String solrManagedFieldSearchable = df.getDatasetFieldType().getSolrField().getNameSearchable();
                                            // Try to get string values from externalvocabularyvalue but for a managed fields of the CVOCConf
                                            Set<String> stringsForManagedField = datasetFieldService.getIndexableStringsByTermUri(val, cvocMap.get(dsfType.getId()), df.getDatasetFieldType().getName());
                                            logger.fine(solrManagedFieldSearchable + " filled with externalvocabularyvalue : " + stringsForManagedField);
                                            //.addField works as addition of value not a replace of value
                                            // it allows to add mapped values by CVOCConf before or after indexing real DatasetField value(s) of solrManagedFieldSearchable
                                            solrInputDocument.addField(solrManagedFieldSearchable, stringsForManagedField);
                                        }
                                    }
                                }
                            }
                            logger.fine(solrFieldSearchable + " filled with externalvocabularyvalue : " + searchStrings);
                            solrInputDocument.addField(solrFieldSearchable, searchStrings);
                            if (dsfType.getSolrField().isFacetable()) {
                                logger.fine(solrFieldFacetable + " gets " + vals);
                                solrInputDocument.addField(solrFieldFacetable, vals);
                            }
                        } else if (dsfType.isControlledVocabulary()) {
                            /** If the cvv list is empty but the dfv list is not then it is assumed this was harvested
                             *  from an installation that had controlled vocabulary entries that don't exist in our this db
                             * @see <a href="https://github.com/IQSS/dataverse/issues/9992">Feature Request/Idea: Harvest metadata values that aren't from a list of controlled values #9992</a>
                             */
                            if (dsf.getControlledVocabularyValues().isEmpty()) {
                                for (DatasetFieldValue dfv : dsf.getDatasetFieldValues()) {
                                    if (dfv.getValue() == null || dfv.getValue().equals(DatasetField.NA_VALUE)) {
                                        continue;
                                    }
                                    solrInputDocument.addField(solrFieldSearchable, dfv.getValue());

                                    if (dsfType.getSolrField().isFacetable()) {
                                        solrInputDocument.addField(solrFieldFacetable, dfv.getValue());
                                    }
                                }
                            } else {
                                for (ControlledVocabularyValue controlledVocabularyValue : dsf.getControlledVocabularyValues()) {
                                    if (controlledVocabularyValue.getStrValue().equals(DatasetField.NA_VALUE)) {
                                        continue;
                                    }

                                    // Index in all used languages (display and metadata languages
                                    if (!dsfType.isAllowMultiples() || langs.isEmpty()) {
                                        solrInputDocument.addField(solrFieldSearchable, controlledVocabularyValue.getStrValue());
                                    } else {
                                        for(String locale: langs) {
                                            solrInputDocument.addField(solrFieldSearchable, controlledVocabularyValue.getLocaleStrValue(locale));
                                        }
                                    }

                                    if (dsfType.getSolrField().isFacetable()) {
                                        solrInputDocument.addField(solrFieldFacetable, controlledVocabularyValue.getStrValue());
                                    }
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
                
                //ToDo - define a geom/bbox type solr field and find those instead of just this one
                if(dsfType.getName().equals(DatasetFieldConstant.geographicBoundingBox)) {
                    String minWestLon=null;
                    String maxEastLon=null;
                    String maxNorthLat=null;
                    String minSouthLat=null;
                    for (DatasetFieldCompoundValue compoundValue : dsf.getDatasetFieldCompoundValues()) {
                        String westLon=null;
                        String eastLon=null;
                        String northLat=null;
                        String southLat=null;
                        for(DatasetField childDsf: compoundValue.getChildDatasetFields()) {
                            switch (childDsf.getDatasetFieldType().getName()) {
                            case DatasetFieldConstant.westLongitude:
                                westLon = childDsf.getRawValue();
                                break;
                            case DatasetFieldConstant.eastLongitude:
                                eastLon = childDsf.getRawValue();
                                break;
                            case DatasetFieldConstant.northLatitude:
                                northLat = childDsf.getRawValue();
                                break;
                            case DatasetFieldConstant.southLatitude:
                                southLat = childDsf.getRawValue();
                                break;
                            }
                        }
                        if ((eastLon != null || westLon != null) && (northLat != null || southLat != null)) {
                            // we have a point or a box, so proceed
                            if (eastLon == null) {
                                eastLon = westLon;
                            } else if (westLon == null) {
                                westLon = eastLon;
                            }
                            if (northLat == null) {
                                northLat = southLat;
                            } else if (southLat == null) {
                                southLat = northLat;
                            }
                            //Find the overall bounding box that includes all bounding boxes
                            if(minWestLon==null || Float.parseFloat(minWestLon) > Float.parseFloat(westLon)) {
                                minWestLon=westLon;
                            }
                            if(maxEastLon==null || Float.parseFloat(maxEastLon) < Float.parseFloat(eastLon)) {
                                maxEastLon=eastLon;
                            }
                            if(minSouthLat==null || Float.parseFloat(minSouthLat) > Float.parseFloat(southLat)) {
                                minSouthLat=southLat;
                            }
                            if(maxNorthLat==null || Float.parseFloat(maxNorthLat) < Float.parseFloat(northLat)) {
                                maxNorthLat=northLat;
                            }

                            if (DatasetFieldValueValidator.validateBoundingBox(westLon, eastLon, northLat, southLat)) {
                                //W, E, N, S
                                solrInputDocument.addField(SearchFields.GEOLOCATION, "ENVELOPE(" + westLon + "," + eastLon + "," + northLat + "," + southLat + ")");
                            }
                        }
                    }
                    //Only one bbox per dataset
                    //W, E, N, S
                    if (DatasetFieldValueValidator.validateBoundingBox(minWestLon, maxEastLon, maxNorthLat, minSouthLat) &&
                            (minWestLon != null || maxEastLon != null) && (maxNorthLat != null || minSouthLat != null)) {
                        solrInputDocument.addField(SearchFields.BOUNDING_BOX, "ENVELOPE(" + minWestLon + "," + maxEastLon + "," + maxNorthLat + "," + minSouthLat + ")");
                    }

                }
            }

            for(String metadataBlockName : metadataBlocksWithValue) {
                solrInputDocument.addField(SearchFields.METADATA_TYPES, metadataBlockName);
            }
        }
        
        List<String> dataversePaths = retrieveDVOPaths(dataset); 
        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
        // solrInputDocument.addField(SearchFields.HOST_DATAVERSE,
        // dataset.getOwner().getName());
        solrInputDocument.addField(SearchFields.PARENT_ID, dataset.getOwner().getId());
        solrInputDocument.addField(SearchFields.PARENT_NAME, dataset.getOwner().getName());

        if (state.equals(DatasetState.DEACCESSIONED)) {
            String deaccessionNote = datasetVersion.getDeaccessionNote();
            if (deaccessionNote != null) {
                solrInputDocument.addField(SearchFields.DATASET_DEACCESSION_REASON, deaccessionNote);
            }
        }
        String versionNote = datasetVersion.getVersionNote();
        if (versionNote != null) {
            solrInputDocument.addField(SearchFields.DATASET_VERSION_NOTE, versionNote);
        }
        docs.add(solrInputDocument);

        /**
         * File Indexing
         */
        boolean doFullTextIndexing = settingsService.isTrueForKey(SettingsServiceBean.Key.SolrFullTextIndexing, false);
        Long maxFTIndexingSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.SolrMaxFileSizeForFullTextIndexing);
        long maxSize = maxFTIndexingSize != null ? maxFTIndexingSize.longValue() : Long.MAX_VALUE;

        List<String> filesIndexed = new ArrayList<>();
        final List<Long> changedFileMetadataIds = new ArrayList<>();
        if (datasetVersion != null) {
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            List<FileMetadata> rfm = new ArrayList<>();
            Map<Long, FileMetadata> fileMap = new HashMap<>();
            if (datasetVersion.isDraft() && dataset.isReleased() && dataset.getReleasedVersion() != null) {
                rfm = dataset.getReleasedVersion().getFileMetadatas();
                for (FileMetadata released : rfm) {
                    fileMap.put(released.getDataFile().getId(), released);
                }

                Query query = em.createNamedQuery("FileMetadata.compareFileMetadata", Long.class);
                query.setParameter(1, dataset.getReleasedVersion().getId());
                query.setParameter(2, datasetVersion.getId());

                changedFileMetadataIds.addAll(query.getResultList());
                logger.fine(
                        "We are indexing a draft version of a dataset that has a released version. We'll be checking file metadatas if they are exact clones of the released versions.");
            } else if (datasetVersion.isDraft()) {
                // Add all file metadata ids to changedFileMetadataIds
                changedFileMetadataIds.addAll(
                    fileMetadatas.stream()
                        .map(FileMetadata::getId)
                        .collect(Collectors.toList())
                );
            }

            AtomicReference<LocalDate> embargoEndDateRef = new AtomicReference<>(null);
            AtomicReference<LocalDate> retentionEndDateRef = new AtomicReference<>(null);
            final String datasetCitation = (dataset.isReleased() && dataset.getReleasedVersion() != null) ? dataset.getCitation(dataset.getReleasedVersion()) : dataset.getCitation();
            final Long datasetId = dataset.getId();
            final String datasetGlobalId = dataset.getGlobalId().toString();
            final String parentTitle = parentDatasetTitle;

            AutoDetectParser ap = null;
            ParseContext ct = null;
            if (doFullTextIndexing) {
                ap = new AutoDetectParser();
                ct = new ParseContext();
            }
            final AutoDetectParser autoParser = ap;
            final ParseContext context = ct;

            Set<String> datasetPublicationStatuses = new HashSet<String>();
            if (dataset.getReleasedVersion() == null && !dataset.isHarvested()) {
                datasetPublicationStatuses.add(UNPUBLISHED_STRING);
            }

            if (datasetVersion.isInReview()) {
                datasetPublicationStatuses.add(IN_REVIEW_STRING);
            }

            if (indexableDataset.getDatasetState().equals(DatasetState.PUBLISHED)) {
                datasetPublicationStatuses.add(PUBLISHED_STRING);
            } else {
                if (indexableDataset.getDatasetState().equals(DatasetState.WORKING_COPY)) {
                    datasetPublicationStatuses.add(DRAFT_STRING);
                }
            }

            String datasetVersionId = datasetVersion.getId().toString();
            boolean indexThisMetadata = indexableDataset.isFilesShouldBeIndexed();
            boolean isReleasedVersion = datasetVersion.isReleased();

            String datasetPersistentURL = dataset.getPersistentURL();
            boolean isHarvested = dataset.isHarvested();
            long startTime = System.currentTimeMillis();
            fileMetadatas.stream().forEach(fileMetadata -> {
                DataFile datafile = fileMetadata.getDataFile();
                Embargo emb = datafile.getEmbargo();
                LocalDate end = null;
                if (emb != null) {
                    final LocalDate endDate = emb.getDateAvailable();
                    embargoEndDateRef.updateAndGet(current -> (current == null || endDate.isAfter(current)) ? endDate : current);
                    end = endDate;
                }
                Retention ret = datafile.getRetention();
                LocalDate start = null;
                if (ret != null) {
                    final LocalDate startDate = ret.getDateUnavailable();
                    retentionEndDateRef.updateAndGet(current -> (current == null || startDate.isBefore(current)) ? startDate : current);
                    start = startDate;
                }
                boolean indexThisFile = false;

                if (indexThisMetadata && (isReleasedVersion || changedFileMetadataIds.contains(fileMetadata.getId()))) {
                    indexThisFile = true;
                } else if (indexThisMetadata) {
                    logger.fine("Checking if this file metadata is a duplicate.");
                    FileMetadata getFromMap = fileMap.get(datafile.getId());
                    if (getFromMap != null) {
                        if (!VariableMetadataUtil.compareVariableMetadata(getFromMap, fileMetadata)) {
                            indexThisFile = true;
                            logger.fine("This file metadata hasn't changed since the released version; skipping indexing.");
                        }
                    }
                }
                if (indexThisFile) {

                    SolrInputDocument datafileSolrInputDocument = new SolrInputDocument();
                    Long fileEntityId = datafile.getId();
                    datafileSolrInputDocument.addField(SearchFields.ENTITY_ID, fileEntityId);
                    datafileSolrInputDocument.addField(SearchFields.DATAVERSE_VERSION_INDEXED_BY, dataverseVersion);
                    datafileSolrInputDocument.addField(SearchFields.IDENTIFIER, fileEntityId);
                    datafileSolrInputDocument.addField(SearchFields.PERSISTENT_URL, datasetPersistentURL);
                    datafileSolrInputDocument.addField(SearchFields.TYPE, "files");
                    datafileSolrInputDocument.addField(SearchFields.CATEGORY_OF_DATAVERSE, dvIndexableCategoryName);
                    if (end != null) {
                        datafileSolrInputDocument.addField(SearchFields.EMBARGO_END_DATE, end.toEpochDay());
                    }
                    if (start != null) {
                        datafileSolrInputDocument.addField(SearchFields.RETENTION_END_DATE, start.toEpochDay());
                    }
                    /* Full-text indexing using Apache Tika */
                    if (doFullTextIndexing) {
                        long fileSize = datafile.getFilesize();
                        if (!isHarvested && !datafile.isRestricted()
                                && !datafile.isFilePackage()
                                && fileSize != 0 && fileSize <= maxSize
                                && datafile.getRetention() == null) {
                            StorageIO<DataFile> accessObject = null;
                            InputStream instream = null;
                            ContentHandler textHandler = null;
                            try {
                                accessObject = DataAccess.getStorageIO(datafile,
                                        new DataAccessRequest());
                                if (accessObject != null) {
                                    accessObject.open();
                                    // If the size is >max, we don't use the stream. However, for S3, the stream is
                                    // currently opened in the call above (see
                                    // https://github.com/IQSS/dataverse/issues/5165), so we want to get a handle so
                                    // we can close it below.
                                    instream = accessObject.getInputStream();
                                    textHandler = new BodyContentHandler(-1);
                                    Metadata metadata = new Metadata();
                                    /*
                                         * Try parsing the file. Note that, other than by limiting size, there's been no
                                         * check see whether this file is a good candidate for text extraction (e.g.
                                         * based on type).
                                     */
                                    autoParser.parse(instream, textHandler, metadata, context);
                                    datafileSolrInputDocument.addField(SearchFields.FULL_TEXT,
                                            textHandler.toString());
                                }
                            } catch (Exception e) {
                                // Needs better logging of what went wrong in order to
                                // track down "bad" documents.
                                logger.warning(String.format("Full-text indexing for %s failed: %s",
                                        datafile.getDisplayName(), e.getLocalizedMessage()));
                                if (logger.isLoggable(Level.FINE)) {
                                    e.printStackTrace();
                                }
                            } catch (OutOfMemoryError e) {
                                logger.warning(String.format("Full-text indexing for %s failed due to OutOfMemoryError",
                                        datafile.getDisplayName()));
                            } catch (Error e) {
                                // Catch everything - full-text indexing is complex enough (and using enough 3rd party components) that it can fail
                                // and we don't want problems here to break other Dataverse functionality (e.g. edits)
                                logger.severe(String.format("Full-text indexing for %s failed due to Error: %s : %s",
                                        datafile.getDisplayName(), e.getClass().getCanonicalName(), e.getLocalizedMessage()));
                            } finally {
                                textHandler = null;
                                IOUtils.closeQuietly(instream);
                            }
                        }
                    }

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

                    datafileSolrInputDocument.addField(SearchFields.DATASET_VERSION_ID, datasetVersionId);
                    addLicenseToSolrDoc(datafileSolrInputDocument, datasetVersion);

                    /**
                     * for rules on sorting files see
                     * https://docs.google.com/a/harvard.edu/document/d/1DWsEqT8KfheKZmMB3n_VhJpl9nIxiUjai_AIQPAjiyA/edit?usp=sharing
                     * via https://redmine.hmdc.harvard.edu/issues/3701
                     */
                    Date fileSortByDate = new Date();

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
                            datafileSolrInputDocument.addField(SearchFields.ACCESS,
                                    FileUtil.isRetentionExpired(datafile)
                                        ? SearchConstants.RETENTIONEXPIRED :
                                            FileUtil.isActivelyEmbargoed(datafile)
                                                ? (fileMetadata.isRestricted() ? SearchConstants.EMBARGOEDTHENRESTRICTED
                                                        : SearchConstants.EMBARGOEDTHENPUBLIC)
                                                : (fileMetadata.isRestricted() ? SearchConstants.RESTRICTED
                                                        : SearchConstants.PUBLIC));
                        } else {
                            logger.fine("indexing file with fileCreateTimestamp. " + fileMetadata.getId() + " (file id " + datafile.getId() + ")");
                            Timestamp fileCreateTimestamp = datafile.getCreateDate();
                            if (fileCreateTimestamp != null) {
                                fileSortByDate = fileCreateTimestamp;
                            } else {
                                String msg = "fileCreateTimestamp was null for fileMetadata id " + fileMetadata.getId() + " (file id " + datafile.getId() + ")";
                                logger.info(msg);
                            }
                            datafileSolrInputDocument.addField(SearchFields.ACCESS,
                                    FileUtil.isActivelyEmbargoed(fileMetadata)
                                            ? (fileMetadata.isRestricted() ? SearchConstants.EMBARGOEDTHENRESTRICTED
                                                    : SearchConstants.EMBARGOEDTHENPUBLIC)
                                            : (fileMetadata.isRestricted() ? SearchConstants.RESTRICTED
                                                    : SearchConstants.PUBLIC));
                        }
                        if (datafile.isHarvested()) {
                            datafileSolrInputDocument.addField(SearchFields.IS_HARVESTED, true);
                            if (FeatureFlags.INDEX_HARVESTED_METADATA_SOURCE.enabled()) {
                                // New - as of 6.3 - option of indexing the actual origin of 
                                // harvested objects as the metadata source:
                                datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE,
                                        dataset.getHarvestedFrom() != null ? dataset.getHarvestedFrom().getMetadataSource() : HARVESTED);
                            } else {
                                datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE, HARVESTED);
                            }
                        } else {
                            datafileSolrInputDocument.addField(SearchFields.IS_HARVESTED, false);
                            datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE, rdvName);
                        }
                    }
                    datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, fileSortByDate);

                    datasetPublicationStatuses.forEach(s -> datafileSolrInputDocument.addField(SearchFields.PUBLICATION_STATUS, s));

                    String fileSolrDocId = solrDocIdentifierFile + fileEntityId;
                    indexableDataset.getDatasetState();
                    if (datasetPublicationStatuses.contains(PUBLISHED_STRING)) {
                        if (FeatureFlags.ADD_PUBLICOBJECT_SOLR_FIELD.enabled()) {
                            datafileSolrInputDocument.addField(SearchFields.PUBLIC_OBJECT, true);
                        }
                        addDatasetReleaseDateToSolrDoc(datafileSolrInputDocument, dataset);
                        // has this published file been deleted from the current draft version?
                        if (datafilesInDraftVersion != null && !datafilesInDraftVersion.contains(datafile.getId())) {
                            datafileSolrInputDocument.addField(SearchFields.FILE_DELETED, true);
                        }
                    } else {
                        indexableDataset.getDatasetState();
                        if (datasetPublicationStatuses.contains(DRAFT_STRING)) {
                            fileSolrDocId = solrDocIdentifierFile + fileEntityId + indexableDataset.getDatasetState().getSuffix();
                        }
                    }
                    datafileSolrInputDocument.addField(SearchFields.ID, fileSolrDocId);

                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_FRIENDLY, datafile.getFriendlyType());
                    datafileSolrInputDocument.addField(SearchFields.FILE_CONTENT_TYPE, datafile.getContentType());
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, datafile.getFriendlyType());
                    // For the file type facets, we have a property file that maps mime types
                    // to facet-friendly names; "application/fits" should become "FITS", etc.:
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, FileUtil.getIndexableFacetFileType(datafile));
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, FileUtil.getIndexableFacetFileType(datafile));
                    datafileSolrInputDocument.addField(SearchFields.FILE_SIZE_IN_BYTES, datafile.getFilesize());
                    if (DataFile.ChecksumType.MD5.equals(datafile.getChecksumType())) {
                        /**
                         * @todo Someday we should probably deprecate this
                         * FILE_MD5 in favor of a combination of
                         * FILE_CHECKSUM_TYPE and FILE_CHECKSUM_VALUE.
                         */
                        datafileSolrInputDocument.addField(SearchFields.FILE_MD5, datafile.getChecksumValue());
                    }
                    datafileSolrInputDocument.addField(SearchFields.FILE_CHECKSUM_TYPE, datafile.getChecksumType().toString());
                    datafileSolrInputDocument.addField(SearchFields.FILE_CHECKSUM_VALUE, datafile.getChecksumValue());
                    datafileSolrInputDocument.addField(SearchFields.FILE_RESTRICTED, datafile.isRestricted());
                    datafileSolrInputDocument.addField(SearchFields.DESCRIPTION, fileMetadata.getDescription());
                    datafileSolrInputDocument.addField(SearchFields.FILE_DESCRIPTION, fileMetadata.getDescription());
                    GlobalId filePid = datafile.getGlobalId();
                    datafileSolrInputDocument.addField(SearchFields.FILE_PERSISTENT_ID,
                            (filePid != null) ? filePid.toString() : null);
                       
                    datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
                    // datafileSolrInputDocument.addField(SearchFields.HOST_DATAVERSE,
                    // dataFile.getOwner().getOwner().getName());
                    // datafileSolrInputDocument.addField(SearchFields.PARENT_NAME,
                    // dataFile.getDataset().getTitle());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_ID, datasetId);
                    datafileSolrInputDocument.addField(SearchFields.PARENT_IDENTIFIER, datasetGlobalId);
                    datafileSolrInputDocument.addField(SearchFields.PARENT_CITATION, datasetCitation);

                    datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, parentTitle);
                    // If this is a tabular data file -- i.e., if there are data
                    // variables associated with this file, we index the variable
                    // names and labels:
                    DataTable dtable = datafile.getDataTable();
                    if (dtable != null) {
                        List<DataVariable> variables = dtable.getDataVariables();
                        Long observations = dtable.getCaseQuantity();
                        datafileSolrInputDocument.addField(SearchFields.VARIABLE_COUNT, variables.size());
                        datafileSolrInputDocument.addField(SearchFields.OBSERVATIONS, observations);
                        datafileSolrInputDocument.addField(SearchFields.UNF, dtable.getUnf());
                            

                        Map<Long, VariableMetadata> variableMap = null;
                        Collection<VariableMetadata> variablesByMetadata = fileMetadata.getVariableMetadatas();

                        variableMap = variablesByMetadata.stream().collect(Collectors.toMap(VariableMetadata::getId, Function.identity()));

                        for (DataVariable var : variables) {
                            // Hard-coded search fields, for now:
                            // TODO: eventually: review, decide how datavariables should
                            // be handled for indexing purposes. (should it be a fixed
                            // setup, defined in the code? should it be flexible? unlikely
                            // that this needs to be domain-specific... since these data
                            // variables are quite specific to tabular data, which in turn
                            // is something social science-specific...
                            // anyway -- needs to be reviewed. -- L.A. 4.0alpha1

                            // Variable Name
                            if (var.getName() != null && !var.getName().equals("")) {
                                datafileSolrInputDocument.addField(SearchFields.VARIABLE_NAME, var.getName());
                            }

                            VariableMetadata vm = variableMap.get(var.getId());
                            if (vm == null) {
                                // Variable Label
                                if (var.getLabel() != null && !var.getLabel().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.VARIABLE_LABEL, var.getLabel());
                                }
                            } else {
                                if (vm.getLabel() != null && !vm.getLabel().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.VARIABLE_LABEL, vm.getLabel());
                                }
                                if (vm.getLiteralquestion() != null && !vm.getLiteralquestion().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.LITERAL_QUESTION, vm.getLiteralquestion());
                                }
                                if (vm.getInterviewinstruction() != null && !vm.getInterviewinstruction().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.INTERVIEW_INSTRUCTIONS, vm.getInterviewinstruction());
                                }
                                if (vm.getPostquestion() != null && !vm.getPostquestion().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.POST_QUESTION, vm.getPostquestion());
                                }
                                if (vm.getUniverse() != null && !vm.getUniverse().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.VARIABLE_UNIVERSE, vm.getUniverse());
                                }
                                if (vm.getNotes() != null && !vm.getNotes().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.VARIABLE_NOTES, vm.getNotes());
                                }

                            }
                        }

                        // TABULAR DATA TAGS:
                        // (not to be confused with the file categories, indexed above!)
                        for (DataFileTag tag : datafile.getTags()) {
                            String tagLabel = tag.getTypeLabel();
                            datafileSolrInputDocument.addField(SearchFields.TABDATA_TAG, tagLabel);
                        }
                    }

                    filesIndexed.add(fileSolrDocId);
                    docs.add(datafileSolrInputDocument);
                }
            });
            long totalLoopTime = System.currentTimeMillis() - startTime;
            logger.fine("Processed all " + fileMetadatas.size() + " fileMetadatas in " + totalLoopTime + " ms");
            logger.fine("Indexed " + docs.size() + " documents to Solr");
            LocalDate embargoEndDate = embargoEndDateRef.get();
            LocalDate retentionEndDate = retentionEndDateRef.get();
            if(embargoEndDate!=null) {
              solrInputDocument.addField(SearchFields.EMBARGO_END_DATE, embargoEndDate.toEpochDay());
            }
            if(retentionEndDate!=null) {
                solrInputDocument.addField(SearchFields.RETENTION_END_DATE, retentionEndDate.toEpochDay());
            }
        }
        Long datasetId = dataset.getId();
        final String msg = "indexed dataset " + datasetId + " as " + datasetSolrDocId + ". filesIndexed: " + filesIndexed;
        logger.fine(msg);
        return new SolrInputDocuments(docs, msg, datasetId);
    }
    
    private String addOrUpdateDataset(IndexableDataset indexableDataset, Set<Long> datafilesInDraftVersion) throws  SolrServerException, IOException {   
        final SolrInputDocuments docs = toSolrDocs(indexableDataset, datafilesInDraftVersion);

        try {
            solrClientIndexService.getSolrClient().add(docs.getDocuments());
        } catch (SolrServerException | IOException ex) {
            logger.warning("Check process-failures logs re: " + ex.getLocalizedMessage());
            if (ex.getCause() instanceof SolrServerException) {
                throw new SolrServerException(ex);
            } else if (ex.getCause() instanceof IOException) {
                throw new IOException(ex);
            }
        }
        return docs.getMessage();
    }

    @Asynchronous
    private void updateLastIndexedTime(Long id) {
        // indexing is often in a transaction with update statements
        // if we flush on query (flush-mode auto), we want to prevent locking
        // -> update the dataset asynchronously in a new transaction
        updateLastIndexedTimeInNewTransaction(id);
    }

    @TransactionAttribute(REQUIRES_NEW)
    private void updateLastIndexedTimeInNewTransaction(Long id) {
        /// Dataset updatedDataset =
        /// (Dataset)dvObjectService.updateContentIndexTime(dataset);
        /// updatedDataset = null;
        // instead of making a call to dvObjectService, let's try and
        // modify the index time stamp using the local EntityManager:
        DvObject dvObjectToModify = em.find(DvObject.class, id);
        dvObjectToModify.setIndexTime(new Timestamp(new Date().getTime()));
        dvObjectToModify = em.merge(dvObjectToModify);
        em.flush();
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
        return findPathSegments(dataverse, segments, null);
    }

    public List<String> findPathSegments(Dataverse dataverse, List<String> segments, Dataverse topOfPath) {
        Dataverse rootDataverse = findRootDataverseCached();
        if (topOfPath == null) {
            topOfPath = rootDataverse;
        }
        if (!dataverse.equals(rootDataverse)) {
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                findPathSegments(dataverse.getOwner(), segments, topOfPath);
            }
            segments.add(dataverse.getId().toString());
            return segments;
        } else {
            // base case
            return segments;
        }
    }
        
    private boolean hasAnyLinkingDataverses(Dataverse dataverse) {
        Dataverse rootDataverse = findRootDataverseCached();
        List<Dataverse> ancestorList = dataverse.getOwners();
        ancestorList.add(dataverse);
        for (Dataverse prior : ancestorList) {
            if (!dataverse.equals(rootDataverse)) {
                List<Dataverse> linkingDVs = dvLinkingService.findLinkingDataverses(prior.getId());
                if (!linkingDVs.isEmpty()){
                    return true;
                }
            }
        }       
        return false;
    }
    
    private List<Dataverse> findAllLinkingDataverses(DvObject dvObject){
        /*
        here we find the linking dataverse of the input object
        then any linked dvs in its owners list
        */
        Dataset dataset = null;
        Dataverse dv = null;
        Dataverse rootDataverse = findRootDataverseCached();        
        List <Dataverse>linkingDataverses = new ArrayList();
        List<Dataverse> ancestorList = new ArrayList();
        
        try {
            if(dvObject.isInstanceofDataset()){
                dataset = (Dataset) dvObject;
                linkingDataverses = dsLinkingService.findLinkingDataverses(dataset.getId());
                ancestorList = dataset.getOwner().getOwners();
                ancestorList.add(dataset.getOwner()); //to show dataset in linking dv when parent dv is linked
            }
            if(dvObject.isInstanceofDataverse()){
                dv = (Dataverse) dvObject;
                linkingDataverses = dvLinkingService.findLinkingDataverses(dv.getId());
                ancestorList = dv.getOwners();
            }
        } catch (Exception ex) {
            logger.info("failed to find Linking Dataverses for " + SearchFields.SUBTREE + ": " + ex);
        }
        
        for (Dataverse owner : ancestorList) {
            if (!owner.equals(rootDataverse)) {
            linkingDataverses.addAll(dvLinkingService.findLinkingDataverses(owner.getId()));
            }
        }       
        
        return linkingDataverses;
    }
    
    private List<String> findLinkingDataversePaths(List<Dataverse> linkingDVs) {

        List<String> pathListAccumulator = new ArrayList<>();
        for (Dataverse toAdd : linkingDVs) {
            //get paths for each linking dataverse
            List<String> linkingDataversePathSegmentsAccumulator = findPathSegments(toAdd, new ArrayList<>());
            List<String> linkingDataversePaths = getDataversePathsFromSegments(linkingDataversePathSegmentsAccumulator);
            for (String dvPath : linkingDataversePaths) {
                if (!pathListAccumulator.contains(dvPath)) {
                    pathListAccumulator.add(dvPath);
                }
            }
        }

        return pathListAccumulator;
    }

    private List<String> getDataversePathsFromSegments(List<String> dataversePathSegments) {
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

    private void addLicenseToSolrDoc(SolrInputDocument solrInputDocument, DatasetVersion datasetVersion) {
        if (datasetVersion != null && datasetVersion.getTermsOfUseAndAccess() != null) {
            //test to see if the terms of use are the default set in 5.10 - if so and there's no license then don't add license to solr doc.   
            //fixes 10513
            if(TermsOfUseAndAccess.DEFAULT_NOTERMS.equals(datasetVersion.getTermsOfUseAndAccess().getTermsOfUse())) {
                return; 
            }
            
            String licenseName = "Custom Terms";
            if (datasetVersion.getTermsOfUseAndAccess().getLicense() != null) {
                licenseName = datasetVersion.getTermsOfUseAndAccess().getLicense().getName();
            } else if (datasetVersion.getTermsOfUseAndAccess().getTermsOfUse() == null) {
                // this fixes #10513 for datasets harvested in oai_dc - these 
                // have neither the license id, nor any actual custom terms 
                return; 
            }
            solrInputDocument.addField(SearchFields.DATASET_LICENSE, licenseName);
        }
    }

    private void addDataverseReleaseDateToSolrDoc(SolrInputDocument solrInputDocument, Dataverse dataverse) {
        if (dataverse.getPublicationDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dataverse.getPublicationDate().getTime());
            int YYYY = calendar.get(Calendar.YEAR);
            solrInputDocument.addField(SearchFields.PUBLICATION_YEAR, YYYY);
        }
    }

    private void addDatasetReleaseDateToSolrDoc(SolrInputDocument solrInputDocument, Dataset dataset) {
        if (dataset.getPublicationDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dataset.getPublicationDate().getTime());
            int YYYY = calendar.get(Calendar.YEAR);
            solrInputDocument.addField(SearchFields.PUBLICATION_YEAR, YYYY);
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
    
    
    
    private void updatePathForExistingSolrDocs(DvObject object) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(SearchUtil.constructQuery(SearchFields.ENTITY_ID, object.getId().toString()));

        QueryResponse res = solrClientService.getSolrClient().query(solrQuery);
        
        if (!res.getResults().isEmpty()) {            
            SolrDocument doc = res.getResults().get(0);
            SolrInputDocument sid = new SolrInputDocument();

            for (String fieldName : doc.getFieldNames()) {
                sid.addField(fieldName, doc.getFieldValue(fieldName));
            }

            Dataset dataset = null;
            if (object.isInstanceofDataset()) {
                dataset = datasetService.findDeep(object.getId());
            }
            List<String> paths = object.isInstanceofDataset() ? retrieveDVOPaths(dataset)
                    : retrieveDVOPaths(dataverseService.find(object.getId()));

            sid.removeField(SearchFields.SUBTREE);
            sid.addField(SearchFields.SUBTREE, paths);
            UpdateResponse addResponse = solrClientIndexService.getSolrClient().add(sid);
            if (object.isInstanceofDataset()) {
                for (DataFile df : dataset.getFiles()) {
                    solrQuery.setQuery(SearchUtil.constructQuery(SearchFields.ENTITY_ID, df.getId().toString()));
                    res = solrClientService.getSolrClient().query(solrQuery);
                    if (!res.getResults().isEmpty()) {
                        doc = res.getResults().get(0);
                        sid = new SolrInputDocument();
                        for (String fieldName : doc.getFieldNames()) {
                            sid.addField(fieldName, doc.getFieldValue(fieldName));
                        }
                        sid.removeField(SearchFields.SUBTREE);
                        sid.addField(SearchFields.SUBTREE, paths);
                        addResponse = solrClientIndexService.getSolrClient().add(sid);
                    }
                }
            }
        }            
    }
    
    
    private List<String> retrieveDVOPaths(DvObject dvo) {
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = new ArrayList<>();
        Dataset dataset = null;
        Dataverse dv = null;
        try {
            if(dvo.isInstanceofDataset()){
                dataset = (Dataset) dvo;
                dataverseSegments = findPathSegments(dataset.getOwner(), dataversePathSegmentsAccumulator);
            }
            if(dvo.isInstanceofDataverse()){
                dv = (Dataverse) dvo;
                dataverseSegments = findPathSegments(dv, dataversePathSegmentsAccumulator);
            }
        } catch (Exception ex) {
            logger.info("failed to find dataverseSegments for dataversePaths for " + SearchFields.SUBTREE + ": " + ex);
        }        
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        if (dataversePaths.size() > 0 && dvo.isInstanceofDataverse()) {
            // removing the dataverse's own id from the paths
            // fixes bug where if my parent dv was linked my dv was shown as linked to myself
            dataversePaths.remove(dataversePaths.size() - 1);
        }
        /*
        add linking paths
        */
        dataversePaths.addAll(findLinkingDataversePaths(findAllLinkingDataverses(dvo)));
        return dataversePaths;
    }

    public String delete(Dataverse doomed) {
        logger.fine("deleting Solr document for dataverse " + doomed.getId());
        UpdateResponse updateResponse;
        try {
            updateResponse = solrClientIndexService.getSolrClient().deleteById(solrDocIdentifierDataverse + doomed.getId());
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
            updateResponse = solrClientIndexService.getSolrClient().deleteById(doomed);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Attempted to delete " + doomed + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.fine(response);
        return response;
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

    //Only used when FeatureFlags.REDUCE_SOLR_DELETES is disabled
    private String removeDeaccessioned(Dataset dataset) {
        StringBuilder result = new StringBuilder();
        String deleteDeaccessionedResult = removeSolrDocFromIndex(determineDeaccessionedDatasetId(dataset));
        result.append(deleteDeaccessionedResult);
        List<String> docIds = findSolrDocIdsForFilesToDelete(dataset, IndexableDataset.DatasetState.DEACCESSIONED);
        String deleteFilesResult = removeMultipleSolrDocs(docIds);
        result.append(deleteFilesResult);
        return result.toString();
    }

    //Only used when FeatureFlags.REDUCE_SOLR_DELETES is disabled
    private String removePublished(Dataset dataset) {
        StringBuilder result = new StringBuilder();
        String deletePublishedResult = removeSolrDocFromIndex(determinePublishedDatasetSolrDocId(dataset));
        result.append(deletePublishedResult);
        List<String> docIds = findSolrDocIdsForFilesToDelete(dataset, IndexableDataset.DatasetState.PUBLISHED);
        String deleteFilesResult = removeMultipleSolrDocs(docIds);
        result.append(deleteFilesResult);
        return result.toString();
    }
    
    // Only used when FeatureFlags.REDUCE_SOLR_DELETES is disabled
    private String deleteDraftFiles(List<String> solrDocIdsForDraftFilesToDelete) {
        String deleteDraftFilesResults = "";
        IndexResponse indexResponse = solrIndexService.deleteMultipleSolrIds(solrDocIdsForDraftFilesToDelete);
        deleteDraftFilesResults = indexResponse.toString();
        return deleteDraftFilesResults;
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
     * (Exclude root because it is never indexed)
     */
    public List<Long> findStaleOrMissingDataverses() {
        List<Long> staleDataverseIds = dataverseService.findIdStale();
        Long rootId = dataverseService.findRootDataverse().getId();
        List<Long> ids = new ArrayList<>();
        staleDataverseIds.stream().filter(id -> (!id.equals(rootId))).forEachOrdered(id -> {
            ids.add(id);
        });
        return ids;
    }

    /**
     * @return Datasets that should be reindexed either because they have never
     * been indexed or their index time is before their modification time.
     */
    public List<Long> findStaleOrMissingDatasets() {
        return datasetService.findIdStale();
    }

  
    public List<String> findDataversesInSolrOnly() throws SearchException {
        try {
            /**
             * @todo define this centrally and statically
             */
            return findDvObjectInSolrOnly("dataverses");
        } catch (SearchException ex) {
            throw ex;
        }
    }

    public List<String> findDatasetsInSolrOnly() throws SearchException {
        try {
            /**
             * @todo define this centrally and statically
             */
            return findDvObjectInSolrOnly("datasets");
        } catch (SearchException ex) {
            throw ex;
        }
    }

    public List<String> findFilesInSolrOnly() throws SearchException {
        try {
            /**
             * @todo define this centrally and statically
             */
            return findDvObjectInSolrOnly("files");
        } catch (SearchException ex) {
            throw ex;
        }
    }
    /**
     * Finds permissions documents in Solr that don't have corresponding dvObjects
     * in the database, and returns a list of their Solr "id" field.
     * @return list of "id" field vales for the orphaned Solr permission documents
     * @throws SearchException 
     */
    public List<String> findPermissionsInSolrOnly() throws SearchException {
        logger.info("Checking for solr-only permissions");
        List<String> permissionInSolrOnly = new ArrayList<>();
        try {
            int rows = 1000;
            SolrQuery q = (new SolrQuery(SearchFields.DEFINITION_POINT_DVOBJECT_ID+":*")).setRows(rows).setSort(SortClause.asc(SearchFields.ID));
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;
            while (!done) {
                q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse rsp = solrClientService.getSolrClient().query(q);
                String nextCursorMark = rsp.getNextCursorMark();
                logger.fine("Next cursor mark (1K entries): " + nextCursorMark);
                SolrDocumentList list = rsp.getResults();
                for (SolrDocument doc: list) {
                    long id = Long.parseLong((String) doc.getFieldValue(SearchFields.DEFINITION_POINT_DVOBJECT_ID));
                    String docId = (String) doc.getFieldValue(SearchFields.ID);
                    String dtype = dvObjectService.getDtype(id);
                    if (dtype == null) {
                        permissionInSolrOnly.add(docId);
                    } else if (dtype.equals(DType.Dataset.getDType())) {
                        List<String> states = datasetService.getVersionStates(id);
                        if (states != null) {
                            String latestState = states.get(states.size() - 1);
                            if (docId.endsWith("draft_permission")) {
                                if (!latestState.equals(VersionState.DRAFT.toString())) {
                                    permissionInSolrOnly.add(docId);
                                }
                            } else if (docId.endsWith("deaccessioned_permission")) {
                                if (!latestState.equals(VersionState.DEACCESSIONED.toString())) {
                                    permissionInSolrOnly.add(docId);
                                }
                            } else {
                                if (!states.contains(VersionState.RELEASED.toString())) {
                                    permissionInSolrOnly.add(docId);
                                }
                            }
                        }
                    } else if (dtype.equals(DType.DataFile.getDType())) {
                        List<VersionState> states = dataFileService.findVersionStates(id);
                        Set<String> strings = states.stream().map(VersionState::toString).collect(Collectors.toSet());
                        logger.finest("States for " + docId + ": " + String.join(", ", strings));
                        if (docId.endsWith("draft_permission")) {
                            if (!states.contains(VersionState.DRAFT)) {
                                permissionInSolrOnly.add(docId);
                            }
                        } else if (docId.endsWith("deaccessioned_permission")) {
                            if (!states.contains(VersionState.DEACCESSIONED) && states.size() == 1) {
                                permissionInSolrOnly.add(docId);
                            }
                        } else {
                            if (!states.contains(VersionState.RELEASED)) {
                                permissionInSolrOnly.add(docId);
                            } else {
                                if (!dataFileService.isInReleasedVersion(id)) {
                                    logger.finest("Adding doc " + docId + " to list of permissions in Solr only");
                                    permissionInSolrOnly.add(docId);
                                }
                            }

                        }
                    }
                }
                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }
                cursorMark = nextCursorMark;
            }
        } catch (SolrServerException | IOException ex) {
           throw new SearchException("Error searching Solr for permissions" , ex);
 
        } catch (Exception e) {
            logger.warning(e.getLocalizedMessage());
            e.printStackTrace();
        }
        return permissionInSolrOnly;
    }
    
    private List<String> findDvObjectInSolrOnly(String type) throws SearchException {
        SolrQuery solrQuery = new SolrQuery();
        int rows = 100;
     
        solrQuery.setQuery("*").setRows(rows).setSort(SortClause.asc(SearchFields.ID));
        solrQuery.addFilterQuery(SearchFields.TYPE + ":" + type);
        List<String> dvObjectInSolrOnly = new ArrayList<>();
       
        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        while (!done) {
            solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse rsp = null;
            try {
                rsp = solrClientService.getSolrClient().query(solrQuery);
             } catch (SolrServerException | IOException ex) {
                throw new SearchException("Error searching Solr type: " + type, ex);

            }
            String nextCursorMark = rsp.getNextCursorMark();
            SolrDocumentList list = rsp.getResults();
            for (SolrDocument doc: list) {
                Object idObject = doc.getFieldValue(SearchFields.ENTITY_ID);
                if (idObject != null) {
                    try {
                        long id = (Long) idObject;
                        if (dvObjectService.getDtype(id) == null) {
                            dvObjectInSolrOnly.add((String)doc.getFieldValue(SearchFields.ID));
                        }
                    } catch (ClassCastException ex) {
                        throw new SearchException("Found " + SearchFields.ENTITY_ID + " but error casting " + idObject + " to long", ex);
                    }
                }
            }
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
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
            queryResponse = solrClientService.getSolrClient().query(solrQuery);
        } catch (SolrServerException | IOException ex) {
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
            logger.fine("result of an attempted delete of the harvested files associated with the dataset " + harvestedDataset.getId() + ": " + resultOfAttemptToDeleteFiles);

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

        deleteHarvestedDocuments(solrIdsOfDocumentsToDelete);
    }
    
    public void deleteHarvestedDocuments(List<String> solrIdsOfDocumentsToDelete) {

        logger.fine("attempting to delete the following documents from the index: " + StringUtils.join(solrIdsOfDocumentsToDelete, ","));
        IndexResponse resultOfAttemptToDeleteDocuments = solrIndexService.deleteMultipleSolrIds(solrIdsOfDocumentsToDelete);
        logger.fine("result of attempt to delete harvested documents: " + resultOfAttemptToDeleteDocuments + "\n");
    }

}
