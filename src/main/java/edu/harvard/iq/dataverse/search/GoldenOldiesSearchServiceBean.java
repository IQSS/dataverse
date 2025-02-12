package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.solr.client.solrj.SolrQuery;

@Stateless
@Named
public class GoldenOldiesSearchServiceBean implements SearchService {

    private static final Logger logger = Logger.getLogger(GoldenOldiesSearchServiceBean.class.getCanonicalName());

    private static final String ALL_GROUPS = "*";

    /**
     * We're trying to make the SolrSearchServiceBean lean, mean, and fast, with as
     * few injections of EJBs as possible.
     */
    /**
     * @todo Can we do without the DatasetFieldServiceBean?
     */
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    GroupServiceBean groupService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    SolrClientService solrClientService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;

    private SolrSearchServiceBean solrSearchService;
    
    public void setSolrSearchService(SolrSearchServiceBean solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public String getServiceName() {
        return "goldenOldies";
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

        // Create a new SolrQuery
        SolrQuery solrQuery = new SolrQuery();
        
        // Set the query to search for entity IDs from 0 to 1000
        solrQuery.setQuery("entityId:[* TO 1000]");
        
        // Set sorting by ID
        solrQuery.setSort("entityId", SolrQuery.ORDER.asc);
        
        // Set pagination
        solrQuery.setStart(paginationStart);
        solrQuery.setRows(numResultsPerPage);
        
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

