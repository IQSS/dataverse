package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.search.FacetCategory;
import edu.harvard.iq.dataverse.search.FacetLabel;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
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
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SolrIndexServiceBean SolrIndexService;

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
            @QueryParam("show_my_data") boolean showMyData,
            @QueryParam("query_entities") boolean queryEntities,
            @QueryParam("metadata_fields") List<String> metadataFields,
            @QueryParam("geo_point") String geoPointRequested,
            @QueryParam("geo_radius") String geoRadiusRequested,
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

            try {
                if (!types.isEmpty()) {
                    filterQueries.add(getFilterQueryFromTypes(types));
                } else {
                    /**
                     * Added to prevent a NullPointerException for superusers
                     * (who don't use our permission JOIN) when
                     * SearchServiceBean tries to get SearchFields.TYPE. The GUI
                     * always seems to add SearchFields.TYPE, even for superusers.
                     */
                    filterQueries.add(SearchFields.TYPE + ":(" + SearchConstants.DATAVERSES + " OR " + SearchConstants.DATASETS + " OR " + SearchConstants.FILES + ")");
                }
                sortBy = SearchUtil.getSortBy(sortField, sortOrder);
                numResultsPerPage = getNumberOfResultsPerPage(numResultsPerPageRequested);
                
                 // we have to add "" (root) otherwise there is no permissions check
                if(subtrees.isEmpty()) {
                    dataverseSubtrees.add(getSubtree(""));
                }
                else {
                    for(String subtree : subtrees) {
                        dataverseSubtrees.add(getSubtree(subtree));
                    }
                }
                filterQueries.add(getFilterQueryFromSubtrees(dataverseSubtrees));
                
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

            // users can't change these (yet anyway)
            boolean dataRelatedToMe = showMyData; //getDataRelatedToMe();
            
            SolrQueryResponse solrQueryResponse;
            try {
                solrQueryResponse = searchService.search(createDataverseRequest(user),
                        dataverseSubtrees,
                        query,
                        filterQueries,
                        sortBy.getField(),
                        sortBy.getOrder(),
                        paginationStart,
                        dataRelatedToMe,
                        numResultsPerPage,
                        true, //SEK get query entities always for search API additional Dataset Information 6300  12/6/2019
                        geoPoint,
                        geoRadius
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
                itemsArrayBuilder.add(solrSearchResult.toJsonObject(showRelevance, showEntityIds, showApiUrls, metadataFields));
            }

            JsonObjectBuilder spelling_alternatives = Json.createObjectBuilder();
            for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
                spelling_alternatives.add(entry.getKey(), entry.getValue().toString());
            }

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

            JsonObjectBuilder value = Json.createObjectBuilder()
                    .add("q", query)
                    .add("total_count", solrQueryResponse.getNumResultsFound())
                    .add("start", solrQueryResponse.getResultsStart())
                    .add("spelling_alternatives", spelling_alternatives)
                    .add("items", itemsArrayBuilder.build());
            if (showFacets) {
                value.add("facets", facets);
            }
            value.add("count_in_response", solrSearchResults.size());
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

    public boolean tokenLessSearchAllowed() {
        boolean outOfBoxBehavior = false;
        boolean tokenLessSearchAllowed = settingsSvc.isFalseForKey(SettingsServiceBean.Key.SearchApiRequiresToken, outOfBoxBehavior);
        logger.fine("tokenLessSearchAllowed: " + tokenLessSearchAllowed);
        return tokenLessSearchAllowed;
    }

    private boolean getDataRelatedToMe() {
        /**
         * @todo support Data Related To Me:
         * https://github.com/IQSS/dataverse/issues/1299
         */
        boolean dataRelatedToMe = false;
        return dataRelatedToMe;
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
