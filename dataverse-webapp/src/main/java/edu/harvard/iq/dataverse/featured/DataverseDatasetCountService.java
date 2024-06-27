package edu.harvard.iq.dataverse.featured;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service counting the number of published datasets in a dataverse.
 */
@Stateless
public class DataverseDatasetCountService {
    private static final Logger logger = LoggerFactory.getLogger(DataverseDatasetCountService.class);

    @Inject
    private SolrClient solrClient;

    @Inject
    private IndexServiceBean indexService;

    // -------------------- LOGIC --------------------

    public List<DataverseDatasetCount> countDatasetsInChildrenOf(Dataverse parentDataverse) {
        String parentSubtreePath = indexService.findPathSegments(parentDataverse).stream()
                .collect(Collectors.joining("/", "/", ""));

        QueryResponse response = querySolr(createSolrQuery(parentSubtreePath));

        return response.getFacetField(SearchFields.SUBTREE).getValues().stream()
                .filter(ff -> ff.getName().matches("^" + parentSubtreePath + "/[0-9]+$"))
                .map(DataverseDatasetCountService::toDataverseDatasetCount)
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private SolrQuery createSolrQuery(String subtreePath) {
        SolrQuery solrQuery = new SolrQuery("*")
                .addFilterQuery(SearchFields.TYPE + ":(" + SearchObjectType.DATASETS.getSolrValue() + ")")
                .addFilterQuery(SearchFields.SUBTREE + ":\"" + subtreePath + "\"")
                .addFilterQuery(SearchFields.PUBLICATION_STATUS + ":\"" + SearchPublicationStatus.PUBLISHED.getSolrValue() + "\"")
                .setStart(0)
                .setRows(0) // don't need data, just the facet counter's
                .addFacetField(SearchFields.SUBTREE);

        solrQuery.add(FacetParams.FACET_PREFIX, subtreePath);

        return solrQuery;
    }

    private QueryResponse querySolr(SolrQuery solrQuery) {
        try {
            return solrClient.query(solrQuery);
        } catch (SolrServerException | IOException ex) {
            logger.warn("Exception during solr query: ", ex);
            throw new IllegalStateException("Couldn't retrieve dataset counts.");
        }
    }

    private static DataverseDatasetCount toDataverseDatasetCount(FacetField.Count ff) {
        String[] childPathSegments = ff.getName().split("/");
        Long childDataverseId = Long.valueOf(childPathSegments[childPathSegments.length - 1]);
        logger.info("Dataverse ({}) dataset count:{}", childDataverseId, ff.getCount());
        return new DataverseDatasetCount(childDataverseId, ff.getCount());
    }
}
