package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
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
    SystemConfig systemConfig;

    @Asynchronous
    public Future<String> indexAll() {
        long indexAllTimeBegin = System.currentTimeMillis();
        String status;
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

        List<Dataverse> dataverses = dataverseService.findAll();
        int dataverseIndexCount = 0;
        for (Dataverse dataverse : dataverses) {
            logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverses.size() + ": " + indexService.indexDataverse(dataverse));
            dataverseIndexCount++;
        }

        int datasetIndexCount = 0;
        List<Dataset> datasets = datasetService.findAll();
        for (Dataset dataset : datasets) {
            datasetIndexCount++;
            logger.info("indexing dataset " + datasetIndexCount + " of " + datasets.size() + ": " + indexService.indexDataset(dataset));
        }
//        logger.info("advanced search fields: " + advancedSearchFields);
//        logger.info("not advanced search fields: " + notAdvancedSearchFields);
        logger.info("done iterating through all datasets");

        long indexAllTimeEnd = System.currentTimeMillis();
        String timeElapsed = "index all took " + (indexAllTimeEnd - indexAllTimeBegin) + " milliseconds";
        logger.info(timeElapsed);
        status = dataverseIndexCount + " dataverses and " + datasetIndexCount + " datasets indexed " + timeElapsed + "\n";
        return new AsyncResult<>(status);
    }

}
