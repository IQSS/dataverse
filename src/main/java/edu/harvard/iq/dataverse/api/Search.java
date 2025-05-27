package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchService;
import edu.harvard.iq.dataverse.search.SearchServiceFactory;
import edu.harvard.iq.dataverse.search.FacetCategory;
import edu.harvard.iq.dataverse.search.FacetLabel;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * User-facing documentation:
 * <a href="http://guides.dataverse.org/en/latest/api/search.html">http://guides.dataverse.org/en/latest/api/search.html</a>
 */
@Path("search")
public class Search extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Search.class.getCanonicalName());

    @EJB
    SearchServiceFactory searchServiceFactory;
    @EJB
    DataverseServiceBean dataverseService;
    @Inject
    DatasetVersionFilesServiceBean datasetVersionFilesServiceBean;

    @GET
    @AuthRequired
    public Response search(
            @Context ContainerRequestContext crc,
            @QueryParam("q") String query,
            @QueryParam("type") final List<String> types,
            @QueryParam("subtree") final List<String> subtrees,
            @QueryParam("sort") String sortField,
            @QueryParam("order") String sortOrder,
            @QueryParam("per_page") final int numResultsPerPageRequested,
            @QueryParam("start") final int paginationStart,
            @QueryParam("show_relevance") boolean showRelevance,
            @QueryParam("show_facets") boolean showFacets,
            @QueryParam("fq") final List<String> filterQueries,
            @QueryParam("show_entity_ids") boolean showEntityIds,
            @QueryParam("show_api_urls") boolean showApiUrls,
            @QueryParam("query_entities") @DefaultValue("true") boolean queryEntities,
            @QueryParam("metadata_fields") List<String> metadataFields,
            @QueryParam("geo_point") String geoPointRequested,
            @QueryParam("geo_radius") String geoRadiusRequested,
            @QueryParam("show_type_counts") boolean showTypeCounts,
            @QueryParam("search_service") String searchServiceName,
            @Context HttpServletResponse response
    ) {

        User user;
        try {
            user = getUser(crc);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        if (query != null) {

            // sanity checking on user-supplied arguments
            SortBy sortBy;
            int numResultsPerPage;
            String geoPoint;
            String geoRadius;
            List<Dataverse> dataverseSubtrees = new ArrayList<>();
            DataverseRequest requestUser = createDataverseRequest(user);
            String allTypes = ":(" + SearchConstants.DATAVERSES + " OR " + SearchConstants.DATASETS + " OR " + SearchConstants.FILES + ")";
            Map<String, Long> objectTypeCountsMap = new HashMap<>(3);
            objectTypeCountsMap.put(SearchConstants.UI_DATAVERSES, 0L);
            objectTypeCountsMap.put(SearchConstants.UI_DATASETS, 0L);
            objectTypeCountsMap.put(SearchConstants.UI_FILES, 0L);

            // hard-coded to false since dataRelatedToMe is only used by MyData (DataRetrieverAPI)
            boolean dataRelatedToMe = false;
            SearchService searchService = null;
            if (StringUtils.isNotBlank(searchServiceName)) {
                try {
                    searchService = searchServiceFactory.getSearchService(searchServiceName);
                } catch (IllegalArgumentException e) {
                    return badRequest("Invalid search engine.");
                }
            } else {
                searchService = searchServiceFactory.getDefaultSearchService();
            }
            try {
                // we have to add "" (root) otherwise there is no permissions check
                if (subtrees.isEmpty()) {
                    dataverseSubtrees.add(getSubtree(""));
                }
                else {
                    for (String subtree : subtrees) {
                        dataverseSubtrees.add(getSubtree(subtree));
                    }
                }
                filterQueries.add(getFilterQueryFromSubtrees(dataverseSubtrees));

                if (!types.isEmpty()) {
                    // Query to get the totals if needed.
                    // Only needed if the list of types doesn't include all types since missing types will default to count of 0
                    // Only get the totals for the first page (paginationStart == 0) for speed
                    if (showTypeCounts && types.size() < objectTypeCountsMap.size() && paginationStart == 0) {
                        List<String> totalFilterQueries = new ArrayList<>();
                        totalFilterQueries.addAll(filterQueries);
                        totalFilterQueries.add(SearchFields.TYPE + allTypes);
                        
                        try {
                            
                            SolrQueryResponse resp = searchService.search(requestUser, dataverseSubtrees, query, totalFilterQueries, null, null, 0,
                                    dataRelatedToMe, 1, false, null, null, false, false);
                            if (resp != null) {
                                for (FacetCategory facetCategory : resp.getTypeFacetCategories()) {
                                    for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                                        objectTypeCountsMap.put(facetLabel.getName(), facetLabel.getCount());
                                    }
                                }
                            }
                        } catch(Exception e) {
                            logger.info("Search getting total counts: " + e.getMessage());
                        }
                    }
                    filterQueries.add(getFilterQueryFromTypes(types));
                } else {
                    /**
                     * Added to prevent a NullPointerException for superusers
                     * (who don't use our permission JOIN) when
                     * SearchServiceBean tries to get SearchFields.TYPE. The GUI
                     * always seems to add SearchFields.TYPE, even for superusers.
                     */
                    filterQueries.add(SearchFields.TYPE + allTypes);
                }
                sortBy = SearchUtil.getSortBy(sortField, sortOrder);
                numResultsPerPage = getNumberOfResultsPerPage(numResultsPerPageRequested);
                
                if(filterQueries.isEmpty()) { //Extra sanity check just in case someone else touches this
                    throw new IOException("Filter is empty, which should never happen, as this allows unfettered searching of our index");
                }
                
                geoPoint = getGeoPoint(geoPointRequested);
                geoRadius = getGeoRadius(geoRadiusRequested);

                if (geoPoint != null && geoRadius == null) {
                    return error(Response.Status.BAD_REQUEST, "If you supply geo_point you must also supply geo_radius.");
                }

                if (geoRadius != null && geoPoint == null) {
                    return error(Response.Status.BAD_REQUEST, "If you supply geo_radius you must also supply geo_point.");
                }

            } catch (Exception ex) {
                return error(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
            }
            
            SolrQueryResponse solrQueryResponse;
            try {
                solrQueryResponse = searchService.search(requestUser,
                        dataverseSubtrees,
                        query,
                        filterQueries,
                        sortBy.getField(),
                        sortBy.getOrder(),
                        paginationStart,
                        dataRelatedToMe,
                        numResultsPerPage,
                        queryEntities, 
                        geoPoint,
                        geoRadius,
                        showFacets, // facets are expensive, no need to ask for them if not requested
                        showRelevance // no need for highlights unless requested either
                );
            } catch (SearchException ex) {
                Throwable cause = ex;
                StringBuilder sb = new StringBuilder();
                sb.append(cause + " ");
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    sb.append(cause.getClass().getCanonicalName() + " ");
                    sb.append(cause + " ");
                    // if you search for a colon you see RemoteSolrException: org.apache.solr.search.SyntaxError: Cannot parse ':'
                }
                String message = "Exception running search for [" + query + "] with filterQueries " + filterQueries + " and paginationStart [" + paginationStart + "]: " + sb.toString();
                logger.info(message);
                return error(Response.Status.INTERNAL_SERVER_ERROR, message);
            }

            JsonArrayBuilder itemsArrayBuilder = Json.createArrayBuilder();
            List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
            for (SolrSearchResult solrSearchResult : solrSearchResults) {
                itemsArrayBuilder.add(solrSearchResult.json(showRelevance, showEntityIds, showApiUrls, metadataFields));
            }

            JsonObjectBuilder spelling_alternatives = Json.createObjectBuilder();
            for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
                spelling_alternatives.add(entry.getKey(), entry.getValue().toString());
            }

            JsonObjectBuilder value = Json.createObjectBuilder()
                    .add("q", query)
                    .add("total_count", solrQueryResponse.getNumResultsFound())
                    .add("start", solrQueryResponse.getResultsStart())
                    .add("spelling_alternatives", spelling_alternatives)
                    .add("items", itemsArrayBuilder.build());

            if (showFacets) {
                JsonArrayBuilder facets = Json.createArrayBuilder();
                JsonObjectBuilder facetCategoryBuilder = Json.createObjectBuilder();
                for (FacetCategory facetCategory : solrQueryResponse.getFacetCategoryList()) {
                    JsonObjectBuilder facetCategoryBuilderFriendlyPlusData = Json.createObjectBuilder();
                    JsonArrayBuilder facetLabelBuilderData = Json.createArrayBuilder();
                    for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                        JsonObjectBuilder countBuilder = Json.createObjectBuilder();
                        countBuilder.add(facetLabel.getName(), facetLabel.getCount());
                        facetLabelBuilderData.add(countBuilder);
                    }
                    facetCategoryBuilderFriendlyPlusData.add("friendly", facetCategory.getFriendlyName());
                    facetCategoryBuilderFriendlyPlusData.add("labels", facetLabelBuilderData);
                    facetCategoryBuilder.add(facetCategory.getName(), facetCategoryBuilderFriendlyPlusData);
                }
                facets.add(facetCategoryBuilder);
                value.add("facets", facets);
            }

            value.add("count_in_response", solrSearchResults.size());

            if (showTypeCounts) {
                for (FacetCategory facetCategory : solrQueryResponse.getTypeFacetCategories()) {
                    for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                        if (facetLabel.getCount() > 0) {
                            objectTypeCountsMap.put(facetLabel.getName(), facetLabel.getCount());
                        }
                    }
                }
                JsonObjectBuilder objectTypeCounts = Json.createObjectBuilder();
                objectTypeCountsMap.forEach((k,v) -> objectTypeCounts.add(k,v));
                value.add("total_count_per_object_type", objectTypeCounts);
            }
            /**
             * @todo Returning the fq might be useful as a troubleshooting aid
             * but we don't want to expose the raw dataverse database ids in
             * "subtree_ss" path like "/2/3".
             */
//            value.add("fq_provided", filterQueries.toString());
            if (solrQueryResponse.getError() != null) {
                /**
                 * @todo You get here if you pass only ":" as a query, for
                 * example. Should we return more or better information?
                 */
                return error(Response.Status.BAD_REQUEST, solrQueryResponse.getError());
            }
            return ok(value);
        } else {
            return error(Response.Status.BAD_REQUEST, "q parameter is missing");
        }
    }

    
    @GET
    @Path("/services")
    public Response getSearchEngines() {
        Map<String, SearchService> availableEngines = searchServiceFactory.getAvailableServices();
        
        JsonArrayBuilder enginesArray = Json.createArrayBuilder();
        
        for (String engine : availableEngines.keySet()) {
            JsonObjectBuilder engineObject = Json.createObjectBuilder()
                .add("name", engine)
                .add("displayName", availableEngines.get(engine).getDisplayName());
            enginesArray.add(engineObject);
        }
        
        return ok(enginesArray);
    }
    
    private User getUser(ContainerRequestContext crc) throws WrappedResponse {
        User userToExecuteSearchAs = GuestUser.get();
        try {
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (authenticatedUser != null) {
                userToExecuteSearchAs = authenticatedUser;
            }
        } catch (WrappedResponse ex) {
            if (!tokenLessSearchAllowed()) {
                throw ex;
            }
        }
        return userToExecuteSearchAs;
    }

    private boolean tokenLessSearchAllowed() {
        boolean outOfBoxBehavior = false;
        boolean tokenLessSearchAllowed = settingsSvc.isFalseForKey(SettingsServiceBean.Key.SearchApiRequiresToken, outOfBoxBehavior);
        logger.fine("tokenLessSearchAllowed: " + tokenLessSearchAllowed);
        return tokenLessSearchAllowed;
    }

    private int getNumberOfResultsPerPage(int numResultsPerPage) {
        /**
         * @todo should maxLimit be configurable?
         */
        int maxLimit = 1000; 
        if (numResultsPerPage == 0) {
            /**
             * @todo should defaultLimit be configurable?
             */
            int defaultLimit = 10;
            return defaultLimit;
        } else if (numResultsPerPage < 0) {
            throw new IllegalArgumentException(numResultsPerPage + " results per page requested but can not be less than zero.");
        } else if (numResultsPerPage > maxLimit) {
            /**
             * @todo numbers higher than 2147483647 emit HTML rather than the
             * expected JSON response below.
             *
             * It also returns a 404 but
             * http://docs.oracle.com/javaee/7/tutorial/jaxrs002.htm says 'an
             * HTTP 400 ("Client Error") response is returned' if an int "cannot
             * be parsed as a 32-bit signed integer".
             *
             * Is this perhaps due a change to web.xml and all the prettyfaces
             * stuff in https://github.com/IQSS/dataverse/issues/958 ?
             *
             */
            throw new IllegalArgumentException(numResultsPerPage + " results per page requested but max limit is " + maxLimit + ".");
        } else {
            // ok, fine, you get what you asked for
            return numResultsPerPage;
        }
    }

    private String getFilterQueryFromTypes(List<String> types) throws Exception {
        String filterQuery = null;
        List<String> typeRequested = new ArrayList<>();
        List<String> validTypes = Arrays.asList(SearchConstants.DATAVERSE, SearchConstants.DATASET, SearchConstants.FILE);
        for (String type : types) {
            if (validTypes.contains(type)) {
                if (type.equals(SearchConstants.DATAVERSE)) {
                    typeRequested.add(SearchConstants.DATAVERSES);
                } else if (type.equals(SearchConstants.DATASET)) {
                    typeRequested.add(SearchConstants.DATASETS);
                } else if (type.equals(SearchConstants.FILE)) {
                    typeRequested.add(SearchConstants.FILES);
                }
            } else {
                throw new Exception("Invalid type '" + type + "'. Must be one of " + validTypes);
            }
        }
        filterQuery = SearchFields.TYPE + ":(" + StringUtils.join(typeRequested, " OR ") + ")";
        return filterQuery;
    }
    
    //Only called when there is content
    /**
    * @todo (old) Should filterDownToSubtree logic be centralized in
    * SearchServiceBean?
    */
    private String getFilterQueryFromSubtrees(List<Dataverse> subtrees) throws Exception {
        String subtreesFilter = "";
        
        for(Dataverse dv : subtrees) {
            if (!dv.equals(dataverseService.findRootDataverse())) {
                String dataversePath = dataverseService.determineDataversePath(dv);

                subtreesFilter += "\"" + dataversePath + "\" OR ";

            }
        }
        try{
            subtreesFilter = subtreesFilter.substring(0, subtreesFilter.lastIndexOf("OR"));
        } catch (StringIndexOutOfBoundsException ex) {
            //This case should only happen the root subtree is searched 
            //and there are no ORs in the string
            subtreesFilter = "";
        }
        
        if(!subtreesFilter.equals("")) {
            subtreesFilter =  SearchFields.SUBTREE + ":(" + subtreesFilter + ")";
        }
        
        return subtreesFilter;
    }

    private Dataverse getSubtree(String alias) throws Exception {
        if (StringUtils.isBlank(alias)) {
            return dataverseService.findRootDataverse();
        } else {
            Dataverse subtree = dataverseService.findByAlias(alias);
            if (subtree != null) {
                return subtree;
            } else {
                throw new Exception("Could not find dataverse with alias " + alias);
            }
        }
    }

    private String getGeoPoint(String geoPointRequested) {
        return SearchUtil.getGeoPoint(geoPointRequested);
    }

    private String getGeoRadius(String geoRadiusRequested) {
        return SearchUtil.getGeoRadius(geoRadiusRequested);
    }

}
