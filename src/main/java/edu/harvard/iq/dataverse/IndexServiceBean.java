package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

@Stateless
@Named
public class IndexServiceBean {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;

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
        logger.info("done iterating through all datasets");

        return dataverseIndexCount + " dataverses" + " and " + datasetIndexCount + " datasets indexed\n";
    }

    public String indexDataverse(Dataverse dataverse) {
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, "dataverse_" + dataverse.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataverse.getId());
        solrInputDocument.addField(SearchFields.TYPE, "dataverses");
        solrInputDocument.addField(SearchFields.NAME, dataverse.getName());
        solrInputDocument.addField(SearchFields.ORIGINAL_DATAVERSE, dataverse.getName());
        solrInputDocument.addField(SearchFields.DESCRIPTION, dataverse.getDescription());
        logger.info("dataverse affiliation: " + dataverse.getAffiliation());
        if (dataverse.getAffiliation() != null && !dataverse.getAffiliation().isEmpty()) {
            /**
             * @todo: stop using affiliation as category
             */
//            solrInputDocument.addField(SearchFields.CATEGORY, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.AFFILIATION, dataverse.getAffiliation());
        }
        // checking for NPE is important so we can create the root dataverse
        if (rootDataverse != null && !dataverse.equals(rootDataverse)) {
            solrInputDocument.addField(SearchFields.PARENT_TYPE, "dataverses");
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                solrInputDocument.addField(SearchFields.PARENT_ID, dataverse.getOwner().getId());
                solrInputDocument.addField(SearchFields.PARENT_NAME, dataverse.getOwner().getName());
            }
        }
//        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
//        List<String> dataverseSegments = findPathSegments(dataverse, dataversePathSegmentsAccumulator);
//        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
//        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
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
        Collection<SolrInputDocument> docs = new ArrayList<>();
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
//        List<String> dataverseSegments = null;
//        try {
//            dataverseSegments = findPathSegments(dataset.getOwner(), dataversePathSegmentsAccumulator);
//        } catch (Exception ex) {
//            logger.info("failed to find dataverseSegments for dataversePaths for " + SearchFields.SUBTREE + ": " + ex);
//        }
//        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, "dataset_" + dataset.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataset.getId());
        solrInputDocument.addField(SearchFields.TYPE, "datasets");
        /**
         * @todo: should we assign a dataset title to name like this?
         */
        if (dataset.getLatestVersion() != null) {
            if (dataset.getLatestVersion().getMetadata() != null) {
                if (dataset.getLatestVersion().getMetadata().getAuthorsStr() != null) {
                    if (!dataset.getLatestVersion().getMetadata().getAuthorsStr().isEmpty()) {
                        solrInputDocument.addField(SearchFields.AUTHOR_STRING, dataset.getLatestVersion().getMetadata().getAuthorsStr());
                    } else {
                        logger.info("author string was empty");
                    }
                } else {
                    logger.info("dataset.getLatestVersion().getMetadata().getAuthorsStr() was null");
                }
                if (dataset.getLatestVersion().getMetadata().getTitle() != null) {
                    if (!dataset.getLatestVersion().getMetadata().getTitle().isEmpty()) {
                        solrInputDocument.addField(SearchFields.TITLE, dataset.getLatestVersion().getMetadata().getTitle());
                    }
                    else {
                        logger.info("title was empty");
                    }
                }
                if (dataset.getLatestVersion().getMetadata().getProductionDate() != null) {
                    /**
                     * @todo: clean this up, DRY
                     */
                    SimpleDateFormat inputDateyyyy = new SimpleDateFormat("yyyy", Locale.ENGLISH);
                    try {
                        Date citationDate = inputDateyyyy.parse(dataset.getLatestVersion().getMetadata().getProductionDate());
                        solrInputDocument.addField(SearchFields.CITATION_DATE, citationDate);
                        SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
                        String citationYear = yearOnly.format(citationDate);
                        solrInputDocument.addField(SearchFields.CITATION_YEAR, Integer.parseInt(citationYear));
                    } catch (Exception ex) {
                        logger.info("Can't convert " + dataset.getLatestVersion().getMetadata().getProductionDate() + " to a YYYY date from dataset " + dataset.getId());
                    }
                    SimpleDateFormat inputDateyyyyMMdd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    try {
                        Date citationDate = inputDateyyyyMMdd.parse(dataset.getLatestVersion().getMetadata().getProductionDate());
                        solrInputDocument.addField(SearchFields.CITATION_DATE, citationDate);
                        SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
                        String citationYear = yearOnly.format(citationDate);
                        solrInputDocument.addField(SearchFields.CITATION_YEAR, Integer.parseInt(citationYear));
                    } catch (Exception ex) {
                        logger.info("Can't convert " + dataset.getLatestVersion().getMetadata().getProductionDate() + " to a YYYY-MM-DD date from dataset " + dataset.getId());
                    }
                }
                else {
                    logger.info("dataset.getLatestVersion().getMetadata().getTitle() was null");
                }
            } else {
                logger.info("dataset.getLatestVersion().getMetadata() was null");
            }

        } else {
            logger.info("dataset.getLatestVersion() was null");
        }
        /**
         * @todo: don't use distributor for category. testing facets
         */
       // solrInputDocument.addField(SearchFields.CATEGORY, dataset.getDistributor());
        if (dataset.getDescription() != null && !dataset.getDescription().isEmpty()) {
            solrInputDocument.addField(SearchFields.DESCRIPTION, dataset.getDescription());
        }
//        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
        solrInputDocument.addField(SearchFields.ORIGINAL_DATAVERSE, dataset.getOwner().getName());
        solrInputDocument.addField(SearchFields.PARENT_TYPE, "datasets");
        solrInputDocument.addField(SearchFields.PARENT_ID, dataset.getOwner().getId());
        solrInputDocument.addField(SearchFields.PARENT_NAME, dataset.getOwner().getName());

        docs.add(solrInputDocument);

        List<DataFile> files = dataset.getFiles();
        for (DataFile dataFile : files) {
            SolrInputDocument datafileSolrInputDocument = new SolrInputDocument();
            datafileSolrInputDocument.addField(SearchFields.ID, "datafile_" + dataFile.getId());
            datafileSolrInputDocument.addField(SearchFields.ENTITY_ID, dataFile.getId());
            datafileSolrInputDocument.addField(SearchFields.TYPE, "files");
            datafileSolrInputDocument.addField(SearchFields.NAME, dataFile.getName());
            datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, dataFile.getContentType());
            datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_GROUP, dataFile.getContentType().split("/")[0]);
//            datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
            datafileSolrInputDocument.addField(SearchFields.ORIGINAL_DATAVERSE, dataFile.getOwner().getOwner().getName());
            datafileSolrInputDocument.addField(SearchFields.PARENT_TYPE, "datasets");
           // datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, dataFile.getDataset().getTitle());
            datafileSolrInputDocument.addField(SearchFields.PARENT_ID, dataFile.getOwner().getId());
            if (!dataFile.getOwner().getLatestVersion().getMetadata().getTitle().isEmpty()) {
                datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, dataFile.getOwner().getLatestVersion().getMetadata().getTitle());
            }
            docs.add(datafileSolrInputDocument);
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

        return "indexed dataset " + dataset.getId(); // + ":" + dataset.getTitle();
    }

    public List<String> findPathSegments(Dataverse dataverse, List<String> segments) {
        if (!dataverseService.findRootDataverse().equals(dataverse)) {
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
}
