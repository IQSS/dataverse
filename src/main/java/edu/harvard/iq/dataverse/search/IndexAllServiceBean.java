package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
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
import javax.json.JsonObjectBuilder;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

@Named
@Stateless
public class IndexAllServiceBean {

    private static final Logger logger = Logger.getLogger(IndexAllServiceBean.class.getCanonicalName());

    @EJB
    IndexServiceBean indexService;
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
        if (numPartitions == 1) {
            SolrServer server = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
            logger.info("attempting to delete all Solr documents before a complete re-index");
            try {
                server.deleteByQuery("*:*");// CAUTION: deletes everything!
            } catch (SolrServerException | IOException ex) {
                status = ex.toString();
                logger.info(status);
                return new AsyncResult<>(status);
            }
            try {
                server.commit();
            } catch (SolrServerException | IOException ex) {
                status = ex.toString();
                logger.info(status);
                return new AsyncResult<>(status);
            }

            int numRowsAffected = dvObjectService.clearAllIndexTimes();
            resultOfClearingIndexTimes = "Number of rows affected by clearAllIndexTimes: " + numRowsAffected + ".";
        } else {
            resultOfClearingIndexTimes = "Solr index was not cleared before indexing.";
        }

        List<Dataverse> dataverses = dataverseService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        int dataverseIndexCount = 0;
        for (Dataverse dataverse : dataverses) {
            dataverseIndexCount++;
            logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverses.size());
            Future<String> result = indexService.indexDataverseInNewTransaction(dataverse);
        }

        int datasetIndexCount = 0;
        List<Dataset> datasets = datasetService.findAllOrSubset(numPartitions, partitionId, skipIndexed);
        for (Dataset dataset : datasets) {
            datasetIndexCount++;
            logger.info("indexing dataset " + datasetIndexCount + " of " + datasets.size());
            Future<String> result = indexService.indexDatasetInNewTransaction(dataset);
        }
//        logger.info("advanced search fields: " + advancedSearchFields);
//        logger.info("not advanced search fields: " + notAdvancedSearchFields);
        logger.info("done iterating through all datasets");

        long indexAllTimeEnd = System.currentTimeMillis();
        String timeElapsed = "index all took " + (indexAllTimeEnd - indexAllTimeBegin) + " milliseconds";
        logger.info(timeElapsed);
        status = dataverseIndexCount + " dataverses and " + datasetIndexCount + " datasets indexed. " + timeElapsed + ". " + resultOfClearingIndexTimes + "\n";
        logger.info(status);
        return new AsyncResult<>(status);
    }

}
