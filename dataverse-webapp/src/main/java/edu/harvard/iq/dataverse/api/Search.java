package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SortBy;
import edu.harvard.iq.dataverse.search.response.FacetCategory;
import edu.harvard.iq.dataverse.search.response.FacetLabel;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.search.index.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
    DataverseDao dataverseDao;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    SolrIndexServiceBean SolrIndexService;

    @GET
    public Response search(
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
            @Context HttpServletResponse response
    ) {

        User user;
        try {
            user = getUser();
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        if (query != null) {

            // sanity checking on user-supplied arguments
            SortBy sortBy;
            int numResultsPerPage;
            List<Dataverse> dataverseSubtrees = new ArrayList<>();
            SearchForTypes typesToSearch = SearchForTypes.all();

            try {
                if (!types.isEmpty()) {
                    typesToSearch = getSearchForFromTypes(types);
                }
                sortBy = SearchUtil.getSortBy(sortField, sortOrder);
                numResultsPerPage = getNumberOfResultsPerPage(numResultsPerPageRequested);

                // we have to add "" (root) otherwise there is no permissions check
                if (subtrees.isEmpty()) {
                    dataverseSubtrees.add(getSubtree(""));
                } else {
                    for (String subtree : subtrees) {
                        dataverseSubtrees.add(getSubtree(subtree));
                    }
                }
                filterQueries.add(getFilterQueryFromSubtrees(dataverseSubtrees));

                if (filterQueries.isEmpty()) { //Extra sanity check just in case someone else touches this
                    throw new IOException("Filter is empty, which should never happen, as this allows unfettered searching of our index");
                }

            } catch (Exception ex) {
                return error(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
            }

            SolrQueryResponse solrQueryResponse;
            try {
                solrQueryResponse = searchService.search(createDataverseRequest(user),
                                                         dataverseSubtrees,
                                                         query,
                                                         typesToSearch,
                                                         filterQueries,
                                                         sortBy.getField(),
                                                         sortBy.getOrder(),
                                                         paginationStart,
                                                         numResultsPerPage,
                                                         false
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
                itemsArrayBuilder.add(solrSearchResult.toJsonObject(showRelevance, showEntityIds, showApiUrls));
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
                for (FacetLabel facetLabel : facetCategory.getFacetLabels()) {
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
            response.setHeader("Access-Control-Allow-Origin", "*");
            return allowCors(ok(value));
        } else {
            return allowCors(error(Response.Status.BAD_REQUEST, "q parameter is missing"));
        }
    }

    private User getUser() throws WrappedResponse {
        /**
         * @todo support searching as non-guest:
         * https://github.com/IQSS/dataverse/issues/1299
         *
         * Note that superusers can't currently use the Search API because they
         * see permission documents (all Solr documents, really) and we get a
         * NPE when trying to determine the DvObject type if their query matches
         * a permission document.
         */
        User userToExecuteSearchAs = GuestUser.get();
        try {
            AuthenticatedUser authenticatedUser = findAuthenticatedUserOrDie();
            if (authenticatedUser != null) {
                userToExecuteSearchAs = authenticatedUser;
            }
        } catch (WrappedResponse ex) {
            if (!tokenLessSearchAllowed()) {
                throw ex;
            }
        }
        if (nonPublicSearchAllowed()) {
            return userToExecuteSearchAs;
        } else {
            return GuestUser.get();
        }
    }

    public boolean nonPublicSearchAllowed() {
        return settingsSvc.isTrueForKey(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
    }

    public boolean tokenLessSearchAllowed() {
        boolean tokenLessSearchAllowed = !settingsSvc.isTrueForKey(SettingsServiceBean.Key.SearchApiRequiresToken);
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

    private SearchForTypes getSearchForFromTypes(List<String> types) throws Exception {
        List<SearchObjectType> typeRequested = new ArrayList<>();
        List<String> validTypes = Arrays.asList(SearchConstants.DATAVERSE, SearchConstants.DATASET, SearchConstants.FILE);
        for (String type : types) {
            if (validTypes.contains(type)) {
                if (type.equals(SearchConstants.DATAVERSE)) {
                    typeRequested.add(SearchObjectType.DATAVERSES);
                } else if (type.equals(SearchConstants.DATASET)) {
                    typeRequested.add(SearchObjectType.DATASETS);
                } else if (type.equals(SearchConstants.FILE)) {
                    typeRequested.add(SearchObjectType.FILES);
                }
            } else {
                throw new Exception("Invalid type '" + type + "'. Must be one of " + validTypes);
            }
        }
        return SearchForTypes.byTypes(typeRequested.toArray(new SearchObjectType[0]));
    }

    //Only called when there is content

    /**
     * @todo (old) Should filterDownToSubtree logic be centralized in
     * SearchServiceBean?
     */
    private String getFilterQueryFromSubtrees(List<Dataverse> subtrees) throws Exception {
        String subtreesFilter = "";

        for (Dataverse dv : subtrees) {
            if (!dv.equals(dataverseDao.findRootDataverse())) {
                String dataversePath = dataverseDao.determineDataversePath(dv);

                subtreesFilter += "\"" + dataversePath + "\" OR ";

            }
        }
        try {
            subtreesFilter = subtreesFilter.substring(0, subtreesFilter.lastIndexOf("OR"));
        } catch (StringIndexOutOfBoundsException ex) {
            //This case should only happen the root subtree is searched
            //and there are no ORs in the string
            subtreesFilter = "";
        }

        if (!subtreesFilter.equals("")) {
            subtreesFilter = SearchFields.SUBTREE + ":(" + subtreesFilter + ")";
        }

        return subtreesFilter;
    }

    private Dataverse getSubtree(String alias) throws Exception {
        if (StringUtils.isBlank(alias)) {
            return dataverseDao.findRootDataverse();
        } else {
            Dataverse subtree = dataverseDao.findByAlias(alias);
            if (subtree != null) {
                return subtree;
            } else {
                throw new Exception("Could not find dataverse with alias " + alias);
            }
        }
    }

}
