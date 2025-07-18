package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

@Stateless
@Named
public class OddlyEnoughSearchServiceBean implements SearchService {

    private static final Logger logger = Logger.getLogger(OddlyEnoughSearchServiceBean.class.getCanonicalName());

    private SearchService solrSearchService;
    
    public OddlyEnoughSearchServiceBean() {
        // Default constructor
    }
    
    @Override
    public void setSolrSearchService(SearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public String getServiceName() {
        return "oddlyEnough";
    }
    @Override 
    public String getDisplayName() {
        return "Demo: Odd Results Only";
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
            boolean addHighlights
    ) throws SearchException {

        logger.info("Search query: " + query + "handled by OddlyEnough search service");
        // Execute the query using SolrSearchService
        SolrQueryResponse queryResponse = solrSearchService.search(dataverseRequest, dataverses, query, filterQueries, sortField, sortOrder, 0, onlyDatatRelatedToMe, 1000, retrieveEntities, geoPoint, geoRadius, addFacets, addHighlights);
        
        // Process the results
        List<SolrSearchResult> solrSearchResults = queryResponse.getSolrSearchResults();
        
        logger.info("Number of results: " + solrSearchResults.size());
        logger.info("Number of results found: " + queryResponse.getNumResultsFound());
        
        // Sort the results by ID and filter out even entityIds
        // Sort the results by ID, filter out even entityIds, skip entries based on pagination, and limit results
        solrSearchResults = solrSearchResults.stream()
                .filter(result -> result.getEntityId() % 2 != 0)
                .sorted(Comparator.comparing(SolrSearchResult::getEntityId))
                .toList();
        queryResponse.setNumResultsFound((long) solrSearchResults.size());
        logger.info("Number of results after filter: " + queryResponse.getNumResultsFound());
        
        solrSearchResults = solrSearchResults.stream()
                .skip((long) paginationStart * numResultsPerPage)
                .limit(numResultsPerPage)
                .toList();
        
        logger.info("Remaining number of results: " + solrSearchResults.size());
        for(SolrSearchResult result : solrSearchResults) {
            logger.info("Result ID: " + result.getEntityId());
        }
        queryResponse.setSolrSearchResults(solrSearchResults);
        
        return queryResponse;
    }
}

