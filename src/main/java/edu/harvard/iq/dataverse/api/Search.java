package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FacetCategory;
import edu.harvard.iq.dataverse.FacetLabel;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.SearchServiceBean;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.search.DvObjectSolrDoc;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.SortBy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;

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
    public Response search(
            @QueryParam("q") String query,
            @QueryParam("type") final List<String> types,
            @QueryParam("subtree") String subtreeRequested,
            @QueryParam("sort") String sortField,
            @QueryParam("order") String sortOrder,
            @QueryParam("per_page") final int numResultsPerPageRequested,
            @QueryParam("start") final int paginationStart,
            @QueryParam("show_relevance") boolean showRelevance,
            @QueryParam("show_facets") boolean showFacets,
            @QueryParam("fq") final List<String> filterQueries,
            @QueryParam("show_entity_ids") boolean showEntityIds,
            @QueryParam("show_api_urls") boolean showApiUrls
    ) {
        if (query != null) {

            // sanity checking on user-supplied arguments
            SortBy sortBy;
            int numResultsPerPage;
            Dataverse subtree;
            try {
                if (!types.isEmpty()) {
                    filterQueries.add(getFilterQueryFromTypes(types));
                }
                sortBy = getSortBy(sortField, sortOrder);
                numResultsPerPage = getNumberOfResultsPerPage(numResultsPerPageRequested);
                subtree = getSubtree(subtreeRequested);
                if (!subtree.equals(dataverseService.findRootDataverse())) {
                    String dataversePath = dataverseService.determineDataversePath(subtree);
                    String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
                    /**
                     * @todo Should filterDownToSubtree logic be centralized in
                     * SearchServiceBean?
                     */
                    filterQueries.add(filterDownToSubtree);
                }
            } catch (Exception ex) {
                return errorResponse(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
            }

            // users can't change these (yet anyway)
            boolean dataRelatedToMe = getDataRelatedToMe();
            User user = getUser();

            SolrQueryResponse solrQueryResponse;
            try {
                solrQueryResponse = searchService.search(
                        user,
                        subtree,
                        query,
                        filterQueries,
                        sortBy.getField(),
                        sortBy.getOrder(),
                        paginationStart,
                        dataRelatedToMe,
                        numResultsPerPage
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
                return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
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
                return errorResponse(Response.Status.BAD_REQUEST, solrQueryResponse.getError());
            }
            return okResponse(value);
        } else {
            return errorResponse(Response.Status.BAD_REQUEST, "q parameter is missing");
        }
    }

    private User getUser() {
        /**
         * @todo support searching as non-guest:
         * https://github.com/IQSS/dataverse/issues/1299
         *
         * Note that superusers can't currently use the Search API because they
         * see permission documents (all Solr documents, really) and we get a
         * NPE when trying to determine the DvObject type.
         */
        User user = new GuestUser();
        return user;
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

    private SortBy getSortBy(String sortField, String sortOrder) throws Exception {

        if (StringUtils.isBlank(sortField)) {
            sortField = SearchFields.RELEVANCE;
        } else if (sortField.equals("name")) {
            // "name" sounds better than "name_sort" so we convert it here so users don't have to pass in "name_sort"
            sortField = SearchFields.NAME_SORT;
        } else if (sortField.equals("date")) {
            // "date" sounds better than "release_or_create_date_dt"
            sortField = SearchFields.RELEASE_OR_CREATE_DATE;
        }

        if (StringUtils.isBlank(sortOrder)) {
            if (StringUtils.isNotBlank(sortField)) {
                // default sorting per field if not specified
                if (sortField.equals(SearchFields.RELEVANCE)) {
                    sortOrder = SortBy.DESCENDING;
                } else if (sortField.equals(SearchFields.NAME_SORT)) {
                    sortOrder = SortBy.ASCENDING;
                } else if (sortField.equals(SearchFields.RELEASE_OR_CREATE_DATE)) {
                    sortOrder = SortBy.DESCENDING;
                } else {
                    // asc for alphabetical by default despite GitHub using desc by default:
                    // "The sort order if sort parameter is provided. One of asc or desc. Default: desc"
                    // http://developer.github.com/v3/search/
                    sortOrder = SortBy.ASCENDING;
                }
            }
        }

        List<String> allowedSortOrderValues = SortBy.allowedOrderStrings();
        if (!allowedSortOrderValues.contains(sortOrder)) {
            throw new Exception("The 'order' parameter was '" + sortOrder + "' but expected one of " + allowedSortOrderValues + ". (The 'sort' parameter was/became '" + sortField + "'.)");
        }

        return new SortBy(sortField, sortOrder);
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

    /**
     * This method is for integration tests of search and should be disabled
     * with the boolean within it before release.
     */
    @GET
    @Path("test")
    public Response searchDebug(
            @QueryParam("key") String apiToken,
            @QueryParam("q") String query,
            @QueryParam("fq") final List<String> filterQueries) {

        boolean searchTestMethodDisabled = false;
        if (searchTestMethodDisabled) {
            return errorResponse(Response.Status.BAD_REQUEST, "disabled");
        }

        User user = findUserByApiToken(apiToken);
        if (user == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiToken + "'");
        }

        Dataverse subtreeScope = dataverseService.findRootDataverse();

        String sortField = SearchFields.ID;
        String sortOrder = SortBy.ASCENDING;
        int paginationStart = 0;
        boolean dataRelatedToMe = false;
        int numResultsPerPage = Integer.MAX_VALUE;
        SolrQueryResponse solrQueryResponse;
        try {
            solrQueryResponse = searchService.search(user, subtreeScope, query, filterQueries, sortField, sortOrder, paginationStart, dataRelatedToMe, numResultsPerPage);
        } catch (SearchException ex) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage() + ": " + ex.getCause().getLocalizedMessage());
        }

        JsonArrayBuilder itemsArrayBuilder = Json.createArrayBuilder();
        List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
        for (SolrSearchResult solrSearchResult : solrSearchResults) {
            itemsArrayBuilder.add(solrSearchResult.getType() + ":" + solrSearchResult.getNameSort());
        }

        return okResponse(itemsArrayBuilder);
    }

    /**
     * This method is for integration tests of search and should be disabled
     * with the boolean within it before release.
     */
    @GET
    @Path("perms")
    public Response searchPerms(
            @QueryParam("key") String apiToken,
            @QueryParam("id") Long dvObjectId) {

        boolean searchTestMethodDisabled = false;
        if (searchTestMethodDisabled) {
            return errorResponse(Response.Status.BAD_REQUEST, "disabled");
        }

        User user = findUserByApiToken(apiToken);
        if (user == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiToken + "'");
        }

        List<DvObjectSolrDoc> solrDocs = SolrIndexService.determineSolrDocs(dvObjectId);

        JsonObjectBuilder data = Json.createObjectBuilder();

        JsonArrayBuilder permissionsData = Json.createArrayBuilder();

        for (DvObjectSolrDoc solrDoc : solrDocs) {
            JsonObjectBuilder dataDoc = Json.createObjectBuilder();
            dataDoc.add(SearchFields.ID, solrDoc.getSolrId());
            dataDoc.add(SearchFields.NAME_SORT, solrDoc.getNameOrTitle());
            JsonArrayBuilder perms = Json.createArrayBuilder();
            for (String perm : solrDoc.getPermissions()) {
                perms.add(perm);
            }
            permissionsData.add(dataDoc);
        }
        data.add("perms", permissionsData);

        DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
        Set<RoleAssignment> roleAssignments = rolesSvc.rolesAssignments(dvObject);
        JsonArrayBuilder roleAssignmentsData = Json.createArrayBuilder();
        for (RoleAssignment roleAssignment : roleAssignments) {
            roleAssignmentsData.add(roleAssignment.getRole() + " has been granted to " + roleAssignment.getAssigneeIdentifier() + " on " + roleAssignment.getDefinitionPoint());
        }
        data.add("roleAssignments", roleAssignmentsData);

        return okResponse(data);
    }

}
