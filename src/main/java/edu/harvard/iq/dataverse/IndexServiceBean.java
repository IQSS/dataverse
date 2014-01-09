package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.io.IOException;
import java.text.ParseException;
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

    /**
     * @todo this is only a first stab at indexing into Solr... it's very
     * inefficient (deletes index and indexes EVERYTHING on each run) but at
     * least it populates Solr with data when dataverses and datasets are saved.
     */
    public String index() {
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
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

        Collection<SolrInputDocument> docs = new ArrayList<>();

        List<Dataverse> dataverses = dataverseService.findAll();
        for (Dataverse dataverse : dataverses) {
            SolrInputDocument solrInputDocument = new SolrInputDocument();
            solrInputDocument.addField(SearchFields.ID, "dataverse_" + dataverse.getId());
            solrInputDocument.addField(SearchFields.ENTITY_ID, dataverse.getId());
            solrInputDocument.addField(SearchFields.TYPE, "dataverses");
            solrInputDocument.addField(SearchFields.NAME, dataverse.getName());
            solrInputDocument.addField(SearchFields.ORIGINAL_DATAVERSE, dataverse.getName());
            solrInputDocument.addField(SearchFields.DATAVERSE_HIERARCHY_TAG, dataverse.getName());
            for (Dataverse dataverseOwner : dataverse.getOwners()) {
                if (!dataverseService.findRootDataverse().equals(dataverseOwner)) {
                    solrInputDocument.addField(SearchFields.DATAVERSE_HIERARCHY_TAG, dataverseOwner.getName());
                }
            }
            solrInputDocument.addField(SearchFields.DESCRIPTION, dataverse.getDescription());
            solrInputDocument.addField(SearchFields.CATEGORY, dataverse.getAffiliation());
            docs.add(solrInputDocument);
        }

        List<Dataset> datasets = datasetService.findAll();
        for (Dataset dataset : datasets) {
            SolrInputDocument solrInputDocument = new SolrInputDocument();
            solrInputDocument.addField(SearchFields.ID, "dataset_" + dataset.getId());
            solrInputDocument.addField(SearchFields.ENTITY_ID, dataset.getId());
            solrInputDocument.addField(SearchFields.TYPE, "datasets");
            /**
             * @todo: should we assign a dataset title to name like this?
             */
            solrInputDocument.addField("name", dataset.getTitle());
            solrInputDocument.addField(SearchFields.AUTHOR_STRING, dataset.getAuthor());
            solrInputDocument.addField(SearchFields.TITLE, dataset.getTitle());
            /**
             * @todo: don't use distributor for category. testing facets
             */
            solrInputDocument.addField(SearchFields.CATEGORY, dataset.getDistributor());
            solrInputDocument.addField(SearchFields.DESCRIPTION, dataset.getDescription());
            solrInputDocument.addField(SearchFields.ORIGINAL_DATAVERSE, dataset.getOwner().getName());
            solrInputDocument.addField(SearchFields.DATAVERSE_HIERARCHY_TAG, dataset.getOwner().getName());
            for (Dataverse dataverseOwner : dataset.getOwner().getOwners()) {
                if (!dataverseService.findRootDataverse().equals(dataverseOwner)) {
                    solrInputDocument.addField(SearchFields.DATAVERSE_HIERARCHY_TAG, dataverseOwner.getName());
                }
            }

            SimpleDateFormat inputDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            try {
                Date citationDate = inputDate.parse(dataset.getCitationDate());
                SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
                String citationYear = yearOnly.format(citationDate);
                solrInputDocument.addField(SearchFields.CITATION_YEAR, Integer.parseInt(citationYear));
                solrInputDocument.addField(SearchFields.CITATION_DATE, citationDate);
            } catch (ParseException ex) {
                logger.info("Can't convert " + dataset.getCitationDate() + " to a date from dataset " + dataset.getId() + ": " + dataset.getTitle());
            }
            docs.add(solrInputDocument);

            List<DataFile> files = dataset.getFiles();
            for (DataFile dataFile : files) {
                SolrInputDocument datafileSolrInputDocument = new SolrInputDocument();
                datafileSolrInputDocument.addField(SearchFields.ID, "datafile_" + dataFile.getId());
                datafileSolrInputDocument.addField(SearchFields.ENTITY_ID, dataFile.getId());
                datafileSolrInputDocument.addField(SearchFields.TYPE, "files");
                datafileSolrInputDocument.addField(SearchFields.NAME, dataFile.getName());
                datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, dataFile.getContentType());
                datafileSolrInputDocument.addField(SearchFields.ORIGINAL_DATAVERSE, dataFile.getDataset().getOwner().getName());
                datafileSolrInputDocument.addField(SearchFields.DATAVERSE_HIERARCHY_TAG, dataFile.getDataset().getOwner().getName());
                for (Dataverse dataverseOwner : dataFile.getDataset().getOwner().getOwners()) {
                    if (!dataverseService.findRootDataverse().equals(dataverseOwner)) {
                        datafileSolrInputDocument.addField(SearchFields.DATAVERSE_HIERARCHY_TAG, dataverseOwner.getName());
                    }
                }
                docs.add(datafileSolrInputDocument);
            }
        }
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

        return "reached end of index method (no exceptions)" + "\n";
    }

}
