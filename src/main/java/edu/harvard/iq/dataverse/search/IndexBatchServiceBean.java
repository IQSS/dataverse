package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.solr.client.solrj.SolrServerException;

@Named
@Stateless
public class IndexBatchServiceBean {

    private static final Logger logger = Logger.getLogger(IndexBatchServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    IndexServiceBean indexService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SystemConfig systemConfig;
    
    @Asynchronous
    public Future<JsonObjectBuilder> indexStatus() {
        JsonObjectBuilder response = Json.createObjectBuilder();
        logger.info("Beginning indexStatus()");
        try {
            JsonObject contentInDatabaseButStaleInOrMissingFromSolr = getContentInDatabaseButStaleInOrMissingFromSolr().build();
            JsonObject contentInSolrButNotDatabase = getContentInSolrButNotDatabase().build();
            JsonObject permissionsInSolrButNotDatabase = getPermissionsInSolrButNotDatabase().build();
            JsonObject permissionsInDatabaseButStaleInOrMissingFromSolr = getPermissionsInDatabaseButStaleInOrMissingFromSolr().build();

            response
                    .add("contentInDatabaseButStaleInOrMissingFromIndex", contentInDatabaseButStaleInOrMissingFromSolr)
                    .add("contentInIndexButNotDatabase", contentInSolrButNotDatabase)
                    .add("permissionsInDatabaseButStaleInOrMissingFromIndex", permissionsInDatabaseButStaleInOrMissingFromSolr)
                    .add("permissionsInIndexButNotDatabase", permissionsInSolrButNotDatabase);

            logger.log(Level.INFO, "contentInDatabaseButStaleInOrMissingFromIndex: {0}", contentInDatabaseButStaleInOrMissingFromSolr);
            logger.log(Level.INFO, "contentInIndexButNotDatabase: {0}", contentInSolrButNotDatabase);
            logger.log(Level.INFO, "permissionsInDatabaseButStaleInOrMissingFromIndex: {0}", permissionsInDatabaseButStaleInOrMissingFromSolr);
            logger.log(Level.INFO, "permissionsInIndexButNotDatabase: {0}", permissionsInSolrButNotDatabase);
        } catch (Exception ex) {
            String msg = "Can not determine index status. " + ex.getLocalizedMessage() + ". Is Solr down? Exception: " + ex.getCause().getLocalizedMessage();
            logger.info(msg);
            ex.printStackTrace();
            response.add("SearchException ", msg);
        }
        return new AsyncResult<>(response);
    }

    @Asynchronous
    public Future<JsonObjectBuilder> clearOrphans() {
        JsonObjectBuilder response = Json.createObjectBuilder();
        List<String> solrIds = new ArrayList<>();
        logger.info("Beginning clearOrphans() to check for orphan Solr documents.");
        try {     
            logger.info("checking for orphans type dataverse");
            solrIds.addAll(indexService.findDataversesInSolrOnly());
            logger.info("checking for orphans type dataset");
            solrIds.addAll(indexService.findDatasetsInSolrOnly());
            logger.info("checking for orphans file");
            solrIds.addAll(indexService.findFilesInSolrOnly());
            logger.info("checking for orphan permissions");
            solrIds.addAll(indexService.findPermissionsInSolrOnly());
        } catch (SearchException e) {
            logger.info("SearchException in clearOrphans: " + e.getMessage());
            response.add("response from clearOrphans","SearchException: " + e.getMessage() );
        } 
        logger.info("found " + solrIds.size()+ " orphan documents");
        IndexResponse resultOfSolrDeletionAttempt = solrIndexService.deleteMultipleSolrIds(solrIds);
        logger.info(resultOfSolrDeletionAttempt.getMessage());
        response.add("resultOfSolrDeletionAttempt", resultOfSolrDeletionAttempt.getMessage());
        
        return new AsyncResult<>(response);
    }

    
    @Asynchronous
    public Future<JsonObjectBuilder> indexAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed, boolean previewOnly) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        indexAllOrSubset(numPartitions, partitionId, skipIndexed);
        String status = "indexAllOrSubset has begun";
        response.add("responseFromIndexAllOrSubset", status);
        return new AsyncResult<>(response);
    }

    public JsonObjectBuilder indexAllOrSubsetPreview(long numPartitions, long partitionId, boolean skipIndexed) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonObjectBuilder previewOfWorkload = Json.createObjectBuilder();
        JsonObjectBuilder dvContainerIds = Json.createObjectBuilder();
        
        List<Long> dataverseIds = dataverseService.findDataverseIdsForIndexing(skipIndexed);
        
        JsonArrayBuilder dataverseIdsJson = Json.createArrayBuilder();
        //List<Dataverse> dataverses = dataverseService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        for (Long id : dataverseIds) {
            dataverseIdsJson.add(id);
        }
        
        List<Long> datasetIds = datasetService.findAllOrSubset(numPartitions, partitionId, skipIndexed);

        JsonArrayBuilder datasetIdsJson = Json.createArrayBuilder();
        for (Long id : datasetIds) {
            datasetIdsJson.add(id);
        }
        dvContainerIds.add("dataverses", dataverseIdsJson);
        dvContainerIds.add("datasets", datasetIdsJson);
        previewOfWorkload.add("dvContainerIds", dvContainerIds);
        previewOfWorkload.add("dataverseCount", dataverseIds.size());
        previewOfWorkload.add("datasetCount", datasetIds.size());
        previewOfWorkload.add("partitionId", partitionId);
        response.add("previewOfPartitionWorkload", previewOfWorkload);
        return response;
    }

    public Future<String> indexAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        long indexAllTimeBegin = System.currentTimeMillis();
        String status;

        String resultOfClearingIndexTimes;
        /**
         * @todo Should we allow sysadmins to request that the Solr index and
         * related timestamps in the database be cleared as part of "index all"?
         * If so, we can make this boolean a parameter that's passed into this
         * method. A method to do this clearing has been added as a separate API
         * endpoint.
         */
        boolean clearSolrAndTimestamps = false;
        /**
         * We only allow clearing of Solr and database index timestamps if we
         * are operating on the entire index ("index all") and if we are not
         * running in "continue" mode.
         */
        if (numPartitions == 1 && !skipIndexed && clearSolrAndTimestamps) {
            logger.info("attempting to delete all Solr documents before a complete re-index");
            try {
                JsonObject response = solrIndexService.deleteAllFromSolrAndResetIndexTimes().build();
                String message = response.getString(SolrIndexServiceBean.messageString);
                int numRowsCleared = response.getInt(SolrIndexServiceBean.numRowsClearedByClearAllIndexTimes);
                resultOfClearingIndexTimes = message + " Database rows from which index timestamps were cleared: " + numRowsCleared;
            } catch (SolrServerException | IOException ex) {
                resultOfClearingIndexTimes = "Solr index and database timestamps were not cleared: " + ex;
            }
        } else {
            resultOfClearingIndexTimes = "Solr index was not cleared before indexing.";
        }

        // List<Dataverse> dataverses = dataverseService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        // Note: no support for "partitions" in this experimental branch. 
        // The method below returns the ids of all the unindexed dataverses.
        List<Long> dataverseIds = dataverseService.findDataverseIdsForIndexing(skipIndexed);
        
        int dataverseIndexCount = 0;
        int dataverseFailureCount = 0;
        //for (Dataverse dataverse : dataverses) {
        for (Long id : dataverseIds) {
            try {
                dataverseIndexCount++;
                Dataverse dataverse = dataverseService.find(id);
                logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverseIds.size() + " (id=" + id + ", persistentId=" + dataverse.getAlias() + ")");
                Future<String> result = indexService.indexDataverseInNewTransaction(dataverse);
                dataverse = null;
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                dataverseFailureCount++;
                logger.info("FAILURE indexing dataverse " + dataverseIndexCount + " of " + dataverseIds.size() + " (id=" + id + ") Exception info: " + e.getMessage());
            }
        }

        int datasetIndexCount = 0;
        int datasetFailureCount = 0;
        List<Long> datasetIds = datasetService.findAllOrSubsetOrderByFilesOwned(skipIndexed);
        for (Long id : datasetIds) {
            datasetIndexCount++;
            logger.info("indexing dataset " + datasetIndexCount + " of " + datasetIds.size() + " (id=" + id + ")");
            indexService.indexDatasetInNewTransaction(id);
        }
        logger.info("done iterating through all datasets");

        long indexAllTimeEnd = System.currentTimeMillis();
        String timeElapsed = "index all took " + (indexAllTimeEnd - indexAllTimeBegin) + " milliseconds";
        logger.info(timeElapsed);
        if (datasetFailureCount + dataverseFailureCount > 0){
            String failureMessage = "There were index failures. " + dataverseFailureCount + " dataverse(s) and " + datasetFailureCount + " dataset(s) failed to index. Please check the log for more information.";
            logger.info(failureMessage);            
        }
        status = dataverseIndexCount + " dataverses and " + datasetIndexCount + " datasets indexed. " + timeElapsed + ". " + resultOfClearingIndexTimes + "\n";
        logger.info(status);
        return new AsyncResult<>(status);
    }
        
    @Asynchronous
    public void indexDataverseRecursively(Dataverse dataverse) {
        long start = System.currentTimeMillis();
        int datasetIndexCount = 0, datasetFailureCount = 0, dataverseIndexCount = 0, dataverseFailureCount = 0;
        // get list of Dataverse children
        List<Long> dataverseChildren = dataverseService.findAllDataverseDataverseChildren(dataverse.getId());
        
        // get list of Dataset children
        List<Long> datasetChildren = dataverseService.findAllDataverseDatasetChildren(dataverse.getId());

        logger.info("Starting index on " + (dataverseChildren.size() + 1) + " dataverses and " + datasetChildren.size() + " datasets.");

        // first we have to index the root dataverse or it will not index properly
        try {
            dataverseIndexCount++;
            logger.info("indexing dataverse " + dataverseIndexCount + " of " + (dataverseChildren.size() + 1) + " (id=" + dataverse.getId() + ", persistentId=" + dataverse.getAlias() + ")");
            indexService.indexDataverseInNewTransaction(dataverse);
        } catch (Exception e) {
            //We want to keep running even after an exception so throw some more info into the log
            dataverseFailureCount++;
            logger.info("FAILURE indexing dataverse " + dataverseIndexCount + " of " + (dataverseChildren.size() + 1) + " (id=" + dataverse.getId() + ") Exception info: " + e.getMessage());
        }
        
        // index the Dataverse children
        for (Long childId : dataverseChildren) {
            try {
                dataverseIndexCount++;
                Dataverse dv = dataverseService.find(childId);
                logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverseChildren.size() + " (id=" + childId + ", persistentId=" + dv.getAlias() + ")");
                Future<String> result = indexService.indexDataverseInNewTransaction(dv);
                dv = null;
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                dataverseFailureCount++;
                logger.info("FAILURE indexing dataverse " + dataverseIndexCount + " of " + dataverseChildren.size() + " (id=" + childId + ") Exception info: " + e.getMessage());
            }
        }
        
        // index the Dataset children
        for (Long childId : datasetChildren) {
            datasetIndexCount++;
            logger.info("indexing dataset " + datasetIndexCount + " of " + datasetChildren.size() + " (id=" + childId + ")");
            indexService.indexDatasetInNewTransaction(childId);
        }
        long end = System.currentTimeMillis();
        if (datasetFailureCount + dataverseFailureCount > 0){
            logger.info("There were index failures. " + dataverseFailureCount + " dataverse(s) and " + datasetFailureCount + " dataset(s) failed to index. Please check the log for more information.");            
        }
        logger.info(dataverseIndexCount + " dataverses and " + datasetIndexCount + " datasets indexed. Total time to index " + (end - start) + ".");
    }
      private JsonObjectBuilder getContentInDatabaseButStaleInOrMissingFromSolr() {
        logger.info("checking for stale or missing dataverses");
        List<Long> stateOrMissingDataverses = indexService.findStaleOrMissingDataverses();
        logger.info("checking for stale or missing datasets");  
        List<Long> staleOrMissingDatasets = indexService.findStaleOrMissingDatasets();
        JsonArrayBuilder jsonStaleOrMissingDataverses = Json.createArrayBuilder();
        for (Long id : stateOrMissingDataverses) {
            jsonStaleOrMissingDataverses.add(id);
        }
        JsonArrayBuilder datasetsInDatabaseButNotSolr = Json.createArrayBuilder();
        for (Long id : staleOrMissingDatasets) {
            datasetsInDatabaseButNotSolr.add(id);
        }
        JsonObjectBuilder contentInDatabaseButStaleInOrMissingFromSolr = Json.createObjectBuilder()
                /**
                 * @todo What about files? Currently files are always indexed
                 * along with their parent dataset
                 */
                .add("dataverses", jsonStaleOrMissingDataverses.build())
                .add("datasets", datasetsInDatabaseButNotSolr.build());
        logger.info("completed check for stale or missing content.");
        return contentInDatabaseButStaleInOrMissingFromSolr;
    }

    private JsonObjectBuilder getContentInSolrButNotDatabase() throws SearchException {
        logger.info("checking for dataverses in Solr only");
        List<String> dataversesInSolrOnly = indexService.findDataversesInSolrOnly();
        logger.info("checking for datasets in Solr only");
        List<String> datasetsInSolrOnly = indexService.findDatasetsInSolrOnly();
        logger.info("checking for files in Solr only");
        List<String> filesInSolrOnly = indexService.findFilesInSolrOnly();
        JsonArrayBuilder dataversesInSolrButNotDatabase = Json.createArrayBuilder();
        logger.info("completed check for content in Solr but not database");
        for (String dataverseId : dataversesInSolrOnly) {
            dataversesInSolrButNotDatabase.add(dataverseId);
        }
        JsonArrayBuilder datasetsInSolrButNotDatabase = Json.createArrayBuilder();
        for (String datasetId : datasetsInSolrOnly) {
            datasetsInSolrButNotDatabase.add(datasetId);
        }
        JsonArrayBuilder filesInSolrButNotDatabase = Json.createArrayBuilder();
        for (String fileId : filesInSolrOnly) {
            filesInSolrButNotDatabase.add(fileId);
        }
        JsonObjectBuilder contentInSolrButNotDatabase = Json.createObjectBuilder()
                /**
                 * @todo What about files? Currently files are always indexed
                 * along with their parent dataset
                 */
                .add("dataverses", dataversesInSolrButNotDatabase.build())
                .add("datasets", datasetsInSolrButNotDatabase.build())
                .add("files", filesInSolrButNotDatabase.build());
        
        return contentInSolrButNotDatabase;
    }

    private JsonObjectBuilder getPermissionsInDatabaseButStaleInOrMissingFromSolr() {
        List<Long> staleOrMissingPermissions;
        logger.info("checking for permissions in database but stale or missing from Solr");
        staleOrMissingPermissions = solrIndexService.findPermissionsInDatabaseButStaleInOrMissingFromSolr();
        logger.info("completed checking for permissions in database but stale or missing from Solr");
        JsonArrayBuilder stalePermissionList = Json.createArrayBuilder();
        for (Long dvObjectId : staleOrMissingPermissions) {
            stalePermissionList.add(dvObjectId);
        }
        return Json.createObjectBuilder()
                .add("dvobjects", stalePermissionList.build());
    }
    
    private JsonObjectBuilder getPermissionsInSolrButNotDatabase() throws SearchException {
        
        List<String> staleOrMissingPermissions = indexService.findPermissionsInSolrOnly();
        JsonArrayBuilder stalePermissionList = Json.createArrayBuilder();
        for (String id : staleOrMissingPermissions) {
            stalePermissionList.add(id);
        }
        return Json.createObjectBuilder()
                .add("permissions", stalePermissionList.build());
    }


}
