package edu.harvard.iq.dataverse.search;

import java.util.List;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

public interface SearchService {

    String getServiceName();
    
    /**
     * Import note: "onlyDatatRelatedToMe" relies on filterQueries for providing
     * access to Private Data for the correct user
     *
     * In other words "onlyDatatRelatedToMe", negates other filter Queries
     * related to permissions
     *
     *
     * @param dataverseRequest
     * @param dataverses
     * @param query
     * @param filterQueries
     * @param sortField
     * @param sortOrder
     * @param paginationStart
     * @param onlyDatatRelatedToMe
     * @param numResultsPerPage
     * @return
     * @throws SearchException
     */
    default SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
            List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
            boolean onlyDatatRelatedToMe, int numResultsPerPage) throws SearchException {
        return search(dataverseRequest, dataverses, query, filterQueries, sortField, sortOrder, paginationStart, onlyDatatRelatedToMe, numResultsPerPage, true, null, null);
    }

    /**
     * Import note: "onlyDatatRelatedToMe" relies on filterQueries for providing
     * access to Private Data for the correct user
     *
     * In other words "onlyDatatRelatedToMe", negates other filter Queries
     * related to permissions
     *
     *
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
    
     * @return
     * @throws SearchException
     */
    default SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
            List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
            boolean onlyDatatRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
            String geoRadius) throws SearchException{
        return search(dataverseRequest, dataverses, query, filterQueries, sortField, sortOrder, paginationStart, onlyDatatRelatedToMe, numResultsPerPage, true, null, null, true, true);
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
    SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
            List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
            boolean onlyDatatRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
            String geoRadius, boolean addFacets, boolean addHighlights) throws SearchException;

}