package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.solr.client.solrj.SolrServerException;

@Named
@Stateless
public class IndexAllServiceBean {

    private static final Logger logger = Logger.getLogger(IndexAllServiceBean.class.getCanonicalName());

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
    public Future<JsonObjectBuilder> indexAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed, boolean previewOnly) {
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
        JsonArrayBuilder dataverseIds = Json.createArrayBuilder();
        List<Dataverse> dataverses = dataverseService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        for (Dataverse dataverse : dataverses) {
            dataverseIds.add(dataverse.getId());
        }
        JsonArrayBuilder datasetIds = Json.createArrayBuilder();
        List<Dataset> datasets = datasetService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
          for (Dataset dataset : datasets) {
            datasetIds.add(dataset.getId());
        }
        dvContainerIds.add("dataverses", dataverseIds);
        dvContainerIds.add("datasets", datasetIds);
        previewOfWorkload.add("dvContainerIds", dvContainerIds);
        previewOfWorkload.add("dataverseCount", dataverses.size());
        previewOfWorkload.add("datasetCount", datasets.size());
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

        List<Dataverse> dataverses = dataverseService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        int dataverseIndexCount = 0;
        int dataverseFailureCount = 0;
        for (Dataverse dataverse : dataverses) {
            try {
                dataverseIndexCount++;
                logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverses.size() + " (id=" + dataverse.getId() + ", persistentId=" + dataverse.getAlias() + ")");
                Future<String> result = indexService.indexDataverseInNewTransaction(dataverse);
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                dataverseFailureCount++;
                logger.info("FAILURE indexing dataverse " + dataverseIndexCount + " of " + dataverses.size() + " (id=" + dataverse.getId() + ", persistentId=" + dataverse.getAlias() + ") Exception info: " + e.getMessage());
            }
        }

        int datasetIndexCount = 0;
        int datasetFailureCount = 0;
        List<Dataset> datasets = datasetService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        for (Dataset dataset : datasets) {
            try {
                datasetIndexCount++;
                logger.info("indexing dataset " + datasetIndexCount + " of " + datasets.size() + " (id=" + dataset.getId() + ", persistentId=" + dataset.getGlobalId() + ")");
                Future<String> result = indexService.indexDatasetInNewTransaction(dataset);
            } catch (Exception e) {
                //We want to keep running even after an exception so throw some more info into the log
                datasetFailureCount++;
                logger.info("FAILURE indexing dataset " + datasetIndexCount + " of " + datasets.size() + " (id=" + dataset.getId() + ", identifier = " + dataset.getIdentifier() + ") Exception info: " + e.getMessage());
            }

        }
//        logger.info("advanced search fields: " + advancedSearchFields);
//        logger.info("not advanced search fields: " + notAdvancedSearchFields);
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

}
