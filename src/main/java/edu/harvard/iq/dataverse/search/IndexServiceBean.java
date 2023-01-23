package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataAccessRequest;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataUtil;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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
    SolrClientService solrClientService;
    @EJB
    DataFileServiceBean dataFileService;

    @EJB
    VariableServiceBean variableService;
    
    @EJB
    IndexBatchServiceBean indexBatchService;
    
    @EJB
    DatasetFieldServiceBean datasetFieldService;

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
    SolrClient solrServer;

    private VariableMetadataUtil variableMetadataUtil;

    @PostConstruct
    public void init() {
        // Get from MPCONFIG. Might be configured by a sysadmin or simply return the default shipped with
        // resources/META-INF/microprofile-config.properties.
        String protocol = JvmSettings.SOLR_PROT.lookup();
        String path = JvmSettings.SOLR_PATH.lookup();
    
        String urlString = protocol + "://" + systemConfig.getSolrHostColonPort() + path;
        solrServer = new HttpSolrClient.Builder(urlString).build();

        rootDataverseName = findRootDataverseCached().getName();
    }

    @PreDestroy
    public void close() {
        if (solrServer != null) {
            try {
                solrServer.close();
            } catch (IOException e) {
                logger.warning("Solr closing error: " + e);
            }
            solrServer = null;
        }
    }
   
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
        solrInputDocument.addField(SearchFields.METADATA_SOURCE, findRootDataverseCached().getName()); //rootDataverseName);
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
                solrClientService.getSolrClient().add(docs);
            } else {
                logger.info("WARNING: indexing of a dataverse with no id attempted");
            }
        } catch (SolrServerException | IOException ex) {
            status = ex.toString();
            logger.info(status);
            return new AsyncResult<>(status);
        }
        try {
            solrClientService.getSolrClient().commit();
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
    public Future<String> indexDatasetInNewTransaction(Long datasetId) throws  SolrServerException, IOException{ //Dataset dataset) {
        boolean doNormalSolrDocCleanUp = false;
        Dataset dataset = em.find(Dataset.class, datasetId);
        // return indexDataset(dataset, doNormalSolrDocCleanUp);
        Future<String> ret = indexDataset(dataset, doNormalSolrDocCleanUp);
        dataset = null;
        return ret;
    }
    
    @TransactionAttribute(REQUIRES_NEW)
    public Future<String> indexDatasetObjectInNewTransaction(Dataset dataset) throws  SolrServerException, IOException{ //Dataset dataset) {
        boolean doNormalSolrDocCleanUp = false;
        // return indexDataset(dataset, doNormalSolrDocCleanUp);
        Future<String> ret = indexDataset(dataset, doNormalSolrDocCleanUp);
        dataset = null;
        return ret;
    }

    @Asynchronous
    public Future<String> asyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) throws  SolrServerException, IOException {
        return indexDataset(dataset, doNormalSolrDocCleanUp);
    }
    
    @Asynchronous
    public void asyncIndexDatasetList(List<Dataset> datasets, boolean doNormalSolrDocCleanUp) throws  SolrServerException, IOException {
        for(Dataset dataset : datasets) {
            indexDataset(dataset, true);
        }
    }
    
    public Future<String> indexDvObject(DvObject objectIn) throws  SolrServerException, IOException {
        
        if (objectIn.isInstanceofDataset() ){
            return (indexDataset((Dataset)objectIn, true));
        }
        if (objectIn.isInstanceofDataverse() ){
            return (indexDataverse((Dataverse)objectIn));
        }
        return null;
    }
    
    public Future<String> indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) throws  SolrServerException, IOException {
        logger.fine("indexing dataset " + dataset.getId());
        /**
         * @todo should we use solrDocIdentifierDataset or
         * IndexableObject.IndexableTypes.DATASET.getName() + "_" ?
         */
        // String solrIdPublished = solrDocIdentifierDataset + dataset.getId();
        String solrIdPublished = determinePublishedDatasetSolrDocId(dataset);
        String solrIdDraftDataset = IndexableObject.IndexableTypes.DATASET.getName() + "_" + dataset.getId() + IndexableDataset.DatasetState.WORKING_COPY.getSuffix();
        // String solrIdDeaccessioned = IndexableObject.IndexableTypes.DATASET.getName()
        // + "_" + dataset.getId() +
        // IndexableDataset.DatasetState.DEACCESSIONED.getSuffix();
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
                return new AsyncResult<>(result);
            } else if (latestVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {

                desiredCards.put(DatasetVersion.VersionState.DEACCESSIONED, true);
                IndexableDataset indexableDeaccessionedVersion = new IndexableDataset(latestVersion);
                String indexDeaccessionedVersionResult = addOrUpdateDataset(indexableDeaccessionedVersion);
                results.append("No draft version. Attempting to index as deaccessioned. Result: ").append(indexDeaccessionedVersionResult).append("\n");

                desiredCards.put(DatasetVersion.VersionState.RELEASED, false);
                if (doNormalSolrDocCleanUp) {
                    String deletePublishedResults = removePublished(dataset);
                    results.append("No published version. Attempting to delete traces of published version from index. Result: ").append(deletePublishedResults).append("\n");
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

    private String addOrUpdateDataset(IndexableDataset indexableDataset) throws  SolrServerException, IOException {
        return addOrUpdateDataset(indexableDataset, null);
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

        //This only grabs the immediate parent dataverse's category. We do the same for dataverses themselves.
        solrInputDocument.addField(SearchFields.CATEGORY_OF_DATAVERSE, dataset.getDataverseContext().getIndexableCategoryName());
        solrInputDocument.addField(SearchFields.IDENTIFIER_OF_DATAVERSE, dataset.getDataverseContext().getAlias());
        solrInputDocument.addField(SearchFields.DATAVERSE_NAME, dataset.getDataverseContext().getDisplayName());
        
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

        if (state.equals(indexableDataset.getDatasetState().PUBLISHED)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, PUBLISHED_STRING);
            // solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE,
            // dataset.getPublicationDate());
        } else if (state.equals(indexableDataset.getDatasetState().WORKING_COPY)) {
            solrInputDocument.addField(SearchFields.PUBLICATION_STATUS, DRAFT_STRING);
        }

        addDatasetReleaseDateToSolrDoc(solrInputDocument, dataset);

        if (dataset.isHarvested()) {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, true);
            solrInputDocument.addField(SearchFields.METADATA_SOURCE, HARVESTED);
        } else {
            solrInputDocument.addField(SearchFields.IS_HARVESTED, false);
            solrInputDocument.addField(SearchFields.METADATA_SOURCE, findRootDataverseCached().getName()); //rootDataverseName);
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
            if(datasetVersion.getExternalStatusLabel()!=null) {
                solrInputDocument.addField(SearchFields.EXTERNAL_STATUS, datasetVersion.getExternalStatusLabel());
            }

            Set<String> langs = settingsService.getConfiguredLanguages();
            Map<Long, JsonObject> cvocMap = datasetFieldService.getCVocConf(false);
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
                    } else if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.DATE)) {
                        String dateAsString = "";
                        if (!dsf.getValues_nondisplay().isEmpty()) {
                            dateAsString = dsf.getValues_nondisplay().get(0);
                        }                      
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
                                // solrInputDocument.addField(solrFieldSearchable,
                                // Integer.parseInt(datasetFieldFlaggedAsDate));
                                solrInputDocument.addField(solrFieldSearchable, datasetFieldFlaggedAsDate);
                                if (dsfType.getSolrField().isFacetable()) {
                                    // solrInputDocument.addField(solrFieldFacetable,
                                    // Integer.parseInt(datasetFieldFlaggedAsDate));
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
                            // datasets have titles not names but index title under name as well so we can
                            // sort datasets by name along dataverses and files
                            List<String> possibleTitles = dsf.getValues();
                            String firstTitle = possibleTitles.get(0);
                            if (firstTitle != null) {
                                parentDatasetTitle = firstTitle;
                            }
                            solrInputDocument.addField(SearchFields.NAME_SORT, dsf.getValues());
                        }
                        
                        if(cvocMap.containsKey(dsfType.getId())) {
                            List<String> vals = dsf.getValues_nondisplay();
                            Set<String> searchStrings = new HashSet<String>();
                            for (String val: vals) {
                                searchStrings.add(val);
                                searchStrings.addAll(datasetFieldService.getStringsFor(val));
                            }
                            solrInputDocument.addField(solrFieldSearchable, searchStrings);
                            if (dsfType.getSolrField().isFacetable()) {
                                solrInputDocument.addField(solrFieldFacetable, vals);
                            }
                        }
                        if (dsfType.isControlledVocabulary()) {
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
                            //W, E, N, S
                            solrInputDocument.addField(SearchFields.GEOLOCATION, "ENVELOPE(" + westLon + "," + eastLon + "," + northLat + "," + southLat + ")");
                        }
                    }
                    //Only one bbox per dataset
                    //W, E, N, S
                    if ((minWestLon != null || maxEastLon != null) && (maxNorthLat != null || minSouthLat != null)) {
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

        if (state.equals(indexableDataset.getDatasetState().DEACCESSIONED)) {
            String deaccessionNote = datasetVersion.getVersionNote();
            if (deaccessionNote != null) {
                solrInputDocument.addField(SearchFields.DATASET_DEACCESSION_REASON, deaccessionNote);
            }
        }

        docs.add(solrInputDocument);

        /**
         * File Indexing
         */
        boolean doFullTextIndexing = settingsService.isTrueForKey(SettingsServiceBean.Key.SolrFullTextIndexing, false);
        Long maxFTIndexingSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.SolrMaxFileSizeForFullTextIndexing);
        long maxSize = maxFTIndexingSize != null ? maxFTIndexingSize.longValue() : Long.MAX_VALUE;

        List<String> filesIndexed = new ArrayList<>();
        if (datasetVersion != null) {
            List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            List<FileMetadata> releasedFileMetadatas = new ArrayList<>();
            Map<Long, FileMetadata> fileMap = new HashMap<>();
            boolean checkForDuplicateMetadata = false;
            if (datasetVersion.isDraft() && dataset.isReleased() && dataset.getReleasedVersion() != null) {
                checkForDuplicateMetadata = true;
                releasedFileMetadatas = dataset.getReleasedVersion().getFileMetadatas(); 
                for(FileMetadata released: releasedFileMetadatas){
                    fileMap.put(released.getDataFile().getId(), released);
                }
                logger.fine(
                        "We are indexing a draft version of a dataset that has a released version. We'll be checking file metadatas if they are exact clones of the released versions.");
            }
            LocalDate embargoEndDate=null;
            LocalDate end = null;
            for (FileMetadata fileMetadata : fileMetadatas) {
               
                Embargo emb= fileMetadata.getDataFile().getEmbargo();
                if(emb!=null) {
                    end = emb.getDateAvailable();
                    if(embargoEndDate==null || end.isAfter(embargoEndDate)) {
                        embargoEndDate=end;
                    }
                }

                boolean indexThisMetadata = true;
                if (checkForDuplicateMetadata && !releasedFileMetadatas.isEmpty()) {
                    logger.fine("Checking if this file metadata is a duplicate.");
                    FileMetadata getFromMap = fileMap.get(fileMetadata.getDataFile().getId());
                    if (getFromMap != null) {
                        if ((fileMetadata.getDataFile().isRestricted() == getFromMap.getDataFile().isRestricted())) {
                            if (fileMetadata.contentEquals(getFromMap)
                                    && variableMetadataUtil.compareVariableMetadata(getFromMap, fileMetadata)) {
                                indexThisMetadata = false;
                                logger.fine("This file metadata hasn't changed since the released version; skipping indexing.");
                            } else {
                                logger.fine("This file metadata has changed since the released version; we want to index it!");
                            }
                        } else {
                            logger.fine("This file's restricted status has changed since the released version; we want to index it!");
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
                    datafileSolrInputDocument.addField(SearchFields.CATEGORY_OF_DATAVERSE, dataset.getDataverseContext().getIndexableCategoryName());
                    if(end!=null) {
                        datafileSolrInputDocument.addField(SearchFields.EMBARGO_END_DATE, end.toEpochDay()); 
                    }
                    
                    /* Full-text indexing using Apache Tika */
                    if (doFullTextIndexing) {
                        if (!dataset.isHarvested() && !fileMetadata.getDataFile().isRestricted() && !fileMetadata.getDataFile().isFilePackage()) {
                            StorageIO<DataFile> accessObject = null;
                            InputStream instream = null;
                            ContentHandler textHandler = null;
                            try {
                                accessObject = DataAccess.getStorageIO(fileMetadata.getDataFile(),
                                        new DataAccessRequest());
                                if (accessObject != null) {
                                    accessObject.open();
                                    // If the size is >max, we don't use the stream. However, for S3, the stream is
                                    // currently opened in the call above (see
                                    // https://github.com/IQSS/dataverse/issues/5165), so we want to get a handle so
                                    // we can close it below.
                                    instream = accessObject.getInputStream();
                                    if (accessObject.getSize() <= maxSize) {
                                        AutoDetectParser autoParser = new AutoDetectParser();
                                        textHandler = new BodyContentHandler(-1);
                                        Metadata metadata = new Metadata();
                                        ParseContext context = new ParseContext();
                                        /*
                                         * Try parsing the file. Note that, other than by limiting size, there's been no
                                         * check see whether this file is a good candidate for text extraction (e.g.
                                         * based on type).
                                         */
                                        autoParser.parse(instream, textHandler, metadata, context);
                                        datafileSolrInputDocument.addField(SearchFields.FULL_TEXT,
                                                textHandler.toString());
                                    }
                                }
                            } catch (Exception e) {
                                // Needs better logging of what went wrong in order to
                                // track down "bad" documents.
                                logger.warning(String.format("Full-text indexing for %s failed",
                                        fileMetadata.getDataFile().getDisplayName()));
                                e.printStackTrace();
                                continue;
                            } catch (OutOfMemoryError e) {
                                textHandler = null;
                                logger.warning(String.format("Full-text indexing for %s failed due to OutOfMemoryError",
                                        fileMetadata.getDataFile().getDisplayName()));
                                continue;
                            } finally {
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
                            datafileSolrInputDocument.addField(SearchFields.ACCESS,
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
                            datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE, HARVESTED);
                        } else {
                            datafileSolrInputDocument.addField(SearchFields.IS_HARVESTED, false);
                            datafileSolrInputDocument.addField(SearchFields.METADATA_SOURCE, findRootDataverseCached().getName());
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
                        // datafileSolrInputDocument.addField(SearchFields.PERMS, publicGroupString);
                        addDatasetReleaseDateToSolrDoc(datafileSolrInputDocument, dataset);
                        // has this published file been deleted from the current draft version? 
                        if (datafilesInDraftVersion != null && !datafilesInDraftVersion.contains(datafile.getId())) {
                            datafileSolrInputDocument.addField(SearchFields.FILE_DELETED, true);
                        }
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
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, FileUtil.getIndexableFacetFileType(fileMetadata.getDataFile()));
                    datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_SEARCHABLE, FileUtil.getIndexableFacetFileType(fileMetadata.getDataFile()));
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
                    datafileSolrInputDocument.addField(SearchFields.FILE_PERSISTENT_ID, fileMetadata.getDataFile().getGlobalId().toString());
                    datafileSolrInputDocument.addField(SearchFields.UNF, fileMetadata.getDataFile().getUnf());
                    datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
                    // datafileSolrInputDocument.addField(SearchFields.HOST_DATAVERSE,
                    // dataFile.getOwner().getOwner().getName());
                    // datafileSolrInputDocument.addField(SearchFields.PARENT_NAME,
                    // dataFile.getDataset().getTitle());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_ID, fileMetadata.getDataFile().getOwner().getId());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_IDENTIFIER, fileMetadata.getDataFile().getOwner().getGlobalId().toString());
                    datafileSolrInputDocument.addField(SearchFields.PARENT_CITATION, fileMetadata.getDataFile().getOwner().getCitation());

                    datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, parentDatasetTitle);

                    // If this is a tabular data file -- i.e., if there are data
                    // variables associated with this file, we index the variable
                    // names and labels:
                    if (fileMetadata.getDataFile().isTabularData()) {
                        List<DataVariable> variables = fileMetadata.getDataFile().getDataTable().getDataVariables();
                        
                        Map<Long, VariableMetadata> variableMap = null;
                        List<VariableMetadata> variablesByMetadata = variableService.findVarMetByFileMetaId(fileMetadata.getId());

                        variableMap = 
                            variablesByMetadata.stream().collect(Collectors.toMap(VariableMetadata::getId, Function.identity())); 
    
                                      
                        for (DataVariable var : variables) {
                            // Hard-coded search fields, for now:
                            // TODO: eventually: review, decide how datavariables should
                            // be handled for indexing purposes. (should it be a fixed
                            // setup, defined in the code? should it be flexible? unlikely
                            // that this needs to be domain-specific... since these data
                            // variables are quite specific to tabular data, which in turn
                            // is something social science-specific...
                            // anyway -- needs to be reviewed. -- L.A. 4.0alpha1

                            //Variable Name
                            if (var.getName() != null && !var.getName().equals("")) {
                                datafileSolrInputDocument.addField(SearchFields.VARIABLE_NAME, var.getName());
                            }
                            
                            VariableMetadata vm = variableMap.get(var.getId()); 
                            if (vm == null) {    
                                //Variable Label
                                if (var.getLabel() != null && !var.getLabel().equals("")) {
                                    datafileSolrInputDocument.addField(SearchFields.VARIABLE_LABEL, var.getLabel());
                                }
                            } else {
                                if (vm.getLabel() != null && !vm.getLabel().equals("")  ) {
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
            if(embargoEndDate!=null) {
              solrInputDocument.addField(SearchFields.EMBARGO_END_DATE, embargoEndDate.toEpochDay());
            }
        }
        Long datasetId = dataset.getId();
        final String msg = "indexed dataset " + datasetId + " as " + datasetSolrDocId + ". filesIndexed: " + filesIndexed;
        return new SolrInputDocuments(docs, msg, datasetId);
    }
    
    private String addOrUpdateDataset(IndexableDataset indexableDataset, Set<Long> datafilesInDraftVersion) throws  SolrServerException, IOException {   
        final SolrInputDocuments docs = toSolrDocs(indexableDataset, datafilesInDraftVersion);

        try {
            solrClientService.getSolrClient().add(docs.getDocuments());
            solrClientService.getSolrClient().commit();
        } catch (SolrServerException | IOException ex) {
            if (ex.getCause() instanceof SolrServerException) {
                throw new SolrServerException(ex);
            } else if (ex.getCause() instanceof IOException) {
                throw new IOException(ex);
            }
        }
        /// Dataset updatedDataset =
        /// (Dataset)dvObjectService.updateContentIndexTime(dataset);
        /// updatedDataset = null;
        // instead of making a call to dvObjectService, let's try and
        // modify the index time stamp using the local EntityManager:
        DvObject dvObjectToModify = em.find(DvObject.class, docs.getDatasetId());
        dvObjectToModify.setIndexTime(new Timestamp(new Date().getTime()));
        dvObjectToModify = em.merge(dvObjectToModify);
        dvObjectToModify = null;

        return docs.getMessage();
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

            List<String> paths =  object.isInstanceofDataset() ? retrieveDVOPaths(datasetService.find(object.getId())) 
                    : retrieveDVOPaths(dataverseService.find(object.getId()));

            sid.removeField(SearchFields.SUBTREE);
            sid.addField(SearchFields.SUBTREE, paths);
            UpdateResponse addResponse = solrClientService.getSolrClient().add(sid);
            UpdateResponse commitResponse = solrClientService.getSolrClient().commit();
            if (object.isInstanceofDataset()) {
                for (DataFile df : datasetService.find(object.getId()).getFiles()) {
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
                        addResponse = solrClientService.getSolrClient().add(sid);
                        commitResponse = solrClientService.getSolrClient().commit();
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
            updateResponse = solrClientService.getSolrClient().deleteById(solrDocIdentifierDataverse + doomed.getId());
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            solrClientService.getSolrClient().commit();
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
            updateResponse = solrClientService.getSolrClient().deleteById(doomed);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            solrClientService.getSolrClient().commit();
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
        List<String> permissionInSolrOnly = new ArrayList<>();
        try {
            int rows = 100;
            SolrQuery q = (new SolrQuery(SearchFields.DEFINITION_POINT_DVOBJECT_ID+":*")).setRows(rows).setSort(SortClause.asc(SearchFields.ID));
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;
            while (!done) {
                q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse rsp = solrServer.query(q);
                String nextCursorMark = rsp.getNextCursorMark();
                SolrDocumentList list = rsp.getResults();
                for (SolrDocument doc: list) {
                    long id = Long.parseLong((String) doc.getFieldValue(SearchFields.DEFINITION_POINT_DVOBJECT_ID));
                    if(!dvObjectService.checkExists(id)) {
                        permissionInSolrOnly.add((String)doc.getFieldValue(SearchFields.ID));
                    }
                }
                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }
                cursorMark = nextCursorMark;
            }
        } catch (SolrServerException | IOException ex) {
           throw new SearchException("Error searching Solr for permissions" , ex);
 
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
                rsp = solrServer.query(solrQuery);
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
                        if (!dvObjectService.checkExists(id)) {
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

        logger.fine("attempting to delete the following documents from the index: " + StringUtils.join(solrIdsOfDocumentsToDelete, ","));
        IndexResponse resultOfAttemptToDeleteDocuments = solrIndexService.deleteMultipleSolrIds(solrIdsOfDocumentsToDelete);
        logger.fine("result of attempt to delete harvested documents: " + resultOfAttemptToDeleteDocuments + "\n");
    }

}
