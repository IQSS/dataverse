package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Stateless
public class IndexBatchServiceBean {

    private static final Logger logger = Logger.getLogger(IndexBatchServiceBean.class.getCanonicalName());

    @EJB
    IndexServiceBean indexService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    DatasetDao datasetDao;
    @EJB
    SystemConfig systemConfig;

    @Asynchronous
    public Future<JsonObjectBuilder> indexAllOrSubsetAsync(long numPartitions, long partitionId, boolean skipIndexed) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        Future<String> responseFromIndexAllOrSubset = indexAllOrSubset(numPartitions, partitionId, skipIndexed);
        String status = "indexAllOrSubset has begun";
        response.add("responseFromIndexAllOrSubset", status);
        return new AsyncResult<>(response);
    }

    public JsonObjectBuilder indexAllOrSubsetPreview(long numPartitions, long partitionId, boolean skipIndexed) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonObjectBuilder previewOfWorkload = Json.createObjectBuilder();
        JsonObjectBuilder dvContainerIds = Json.createObjectBuilder();

        List<Long> dataverseIds = dataverseDao.findDataverseIdsForIndexing(skipIndexed);

        JsonArrayBuilder dataverseIdsJson = Json.createArrayBuilder();
        //List<Dataverse> dataverses = dataverseDao.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        for (Long id : dataverseIds) {
            dataverseIdsJson.add(id);
        }

        List<Long> datasetIds = datasetDao.findAllOrSubset(numPartitions, partitionId, skipIndexed);

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

    private Future<String> indexAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        long indexAllTimeBegin = System.currentTimeMillis();
        String status;

        // List<Dataverse> dataverses = dataverseDao.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        // Note: no support for "partitions" in this experimental branch. 
        // The method below returns the ids of all the unindexed dataverses.
        List<Long> dataverseIds = dataverseDao.findDataverseIdsForIndexing(skipIndexed);

        int dataverseIndexCount = 0;
        int dataverseFailureCount = 0;
        //for (Dataverse dataverse : dataverses) {
        for (Long id : dataverseIds) {
            try {
                dataverseIndexCount++;
                Dataverse dataverse = dataverseDao.find(id);
                logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverseIds.size() + " (id=" + id + ", persistentId=" + dataverse.getAlias() + ")");
                Future<String> result = indexService.indexDataverseInNewTransaction(dataverse);
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                dataverseFailureCount++;
                logger.info("FAILURE indexing dataverse " + dataverseIndexCount + " of " + dataverseIds.size() + " (id=" + id + ") Exception info: " + e.getMessage());
            }
        }

        int datasetIndexCount = 0;
        int datasetFailureCount = 0;
        List<Long> datasetIds = datasetDao.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        for (Long id : datasetIds) {
            try {
                datasetIndexCount++;
                logger.info("indexing dataset " + datasetIndexCount + " of " + datasetIds.size() + " (id=" + id + ")");
                Future<String> result = indexService.indexDatasetInNewTransaction(id);
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                datasetFailureCount++;
                logger.info("FAILURE indexing dataset " + datasetIndexCount + " of " + datasetIds.size() + " (id=" + id + ") Exception info: " + e.getMessage());
            }
        }
        logger.info("done iterating through all datasets");

        long indexAllTimeEnd = System.currentTimeMillis();
        String timeElapsed = "index all took " + (indexAllTimeEnd - indexAllTimeBegin) + " milliseconds";
        logger.info(timeElapsed);
        if (datasetFailureCount + dataverseFailureCount > 0) {
            String failureMessage = "There were index failures. " + dataverseFailureCount + " dataverse(s) and " + datasetFailureCount + " dataset(s) failed to index. Please check the log for more information.";
            logger.info(failureMessage);
        }
        status = dataverseIndexCount + " dataverses and " + datasetIndexCount + " datasets indexed. " + timeElapsed + ". Solr index was not cleared before indexing.\n";
        logger.info(status);
        return new AsyncResult<>(status);
    }

    @Asynchronous
    public void indexDataverseRecursively(Dataverse dataverse) {
        long start = System.currentTimeMillis();
        int datasetIndexCount = 0, datasetFailureCount = 0, dataverseIndexCount = 0, dataverseFailureCount = 0;
        // get list of Dataverse children
        List<Long> dataverseChildren = dataverseDao.findAllDataverseDataverseChildren(dataverse.getId());

        // get list of Dataset children
        List<Long> datasetChildren = dataverseDao.findAllDataverseDatasetChildren(dataverse.getId());

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
                Dataverse dv = dataverseDao.find(childId);
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
            try {
                datasetIndexCount++;
                logger.info("indexing dataset " + datasetIndexCount + " of " + datasetChildren.size() + " (id=" + childId + ")");
                indexService.indexDatasetInNewTransaction(childId);
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                datasetFailureCount++;
                logger.info("FAILURE indexing dataset " + datasetIndexCount + " of " + datasetChildren.size() + " (id=" + childId + ") Exception info: " + e.getMessage());
            }
        }
        long end = System.currentTimeMillis();
        if (datasetFailureCount + dataverseFailureCount > 0) {
            logger.info("There were index failures. " + dataverseFailureCount + " dataverse(s) and " + datasetFailureCount + " dataset(s) failed to index. Please check the log for more information.");
        }
        logger.info(dataverseIndexCount + " dataverses and " + datasetIndexCount + " datasets indexed. Total time to index " + (end - start) + ".");
    }

}
