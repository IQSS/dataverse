package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import org.apache.solr.client.solrj.SolrQuery;

@Stateless
@Named
public class GoldenOldiesSearchServiceBean implements SearchService {

    private static final Logger logger = Logger.getLogger(GoldenOldiesSearchServiceBean.class.getCanonicalName());

    private SearchService solrSearchService;
    
    public GoldenOldiesSearchServiceBean() {
        // Default constructor
    }
    @Override
    public void setSolrSearchService(SearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public String getServiceName() {
        return "goldenOldies";
    }
    
    @Override
    public String getDisplayName() {
        return "Demo: Search Over First 1K Entries";
    }
    /**
     * @param dataverseRequest
     * @param dataverses
     * @param query
     * @param filterQueries
     * @param sortField
     * @param sortOrder
     * @param paginationStart
     * @param onlyDatatRelatedToMe
     * @param numResultsPerPage
     * @param retrieveEntities - look up dvobject entities with .find() (potentially expensive!)
     * @param geoPoint e.g. "35,15"
     * @param geoRadius e.g. "5"
     * @param addFacets boolean
     * @param addHighlights boolean
     * @param addCollections boolean
     * @return
     * @throws SearchException
     */
    @Override
    public SolrQueryResponse search(
            DataverseRequest dataverseRequest,
            List<Dataverse> dataverses,
            String query,
            List<String> filterQueries,
            String sortField, 
            String sortOrder,
            int paginationStart,
            boolean onlyDatatRelatedToMe,
            int numResultsPerPage,
            boolean retrieveEntities,
            String geoPoint,
            String geoRadius,
            boolean addFacets,
            boolean addHighlights,
            boolean addCollections
    ) throws SearchException {

        // Execute the query using SolrSearchService
        SolrQueryResponse queryResponse = solrSearchService.search(dataverseRequest, null, "entityId:[* TO 1000]", null, "entityId", SolrQuery.ORDER.asc.toString(), paginationStart, false, numResultsPerPage, retrieveEntities, null, null);
        
        // Process the results
        List<SolrSearchResult> solrSearchResults = queryResponse.getSolrSearchResults();
        
        // Sort the results by ID
        solrSearchResults.sort(Comparator.comparing(SolrSearchResult::getEntityId));
        
        queryResponse.setSolrSearchResults(solrSearchResults);
        
        return queryResponse;
    }
}

