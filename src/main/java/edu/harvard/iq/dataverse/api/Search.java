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
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.search.SortBy;
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
            @QueryParam("fq") final List<String> filterQueries,
            @QueryParam("sort") String sortField,
            @QueryParam("order") String sortOrder,
            @QueryParam("start") final int paginationStart,
            @QueryParam("show_relevance") boolean showRelevance,
            @QueryParam("show_facets") boolean showFacets,
            @QueryParam("show_spelling_alternatives") boolean showSpellingAlternatives,
            @QueryParam("subtree") String subtreeRequested
    ) {
        if (query != null) {
            User user = getUser();
            boolean dataRelatedToMe = getDataRelatedToMe();
            int numResultsPerPage = getNumberOfResultsPerPage();

            Dataverse subtree;
            try {
                subtree = getSubtree(subtreeRequested);
            } catch (Exception ex) {
                return errorResponse(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
            }

            SortBy sortBy;
            try {
                sortBy = getSortBy(sortField, sortOrder);
            } catch (Exception ex) {
                return errorResponse(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
            }

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
            JsonArrayBuilder relevancePerResult = Json.createArrayBuilder();
            List<SolrSearchResult> solrSearchResults = solrQueryResponse.getSolrSearchResults();
            for (SolrSearchResult solrSearchResult : solrSearchResults) {
                itemsArrayBuilder.add(solrSearchResult.toJsonObject(showRelevance));
                relevancePerResult.add(solrSearchResult.getRelevance());
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
            Map<String, String> datasetfieldFriendlyNamesBySolrField = solrQueryResponse.getDatasetfieldFriendlyNamesBySolrField();
//            logger.info("the hash: " + datasetfieldFriendlyNamesBySolrField);
            for (Map.Entry<String, String> entry : datasetfieldFriendlyNamesBySolrField.entrySet()) {
                String string = entry.getKey();
                String string1 = entry.getValue();
//                logger.info(string + ":" + string1);
            }

            JsonObjectBuilder value = Json.createObjectBuilder()
                    .add("q", query)
                    .add("fq_provided", filterQueries.toString())
                    .add("total_count", solrQueryResponse.getNumResultsFound())
                    .add("start", solrQueryResponse.getResultsStart())
                    /**
                     * @todo consider removing count_in_response and letting
                     * client calculate it
                     */
                    .add("count_in_response", solrSearchResults.size())
                    .add("items", itemsArrayBuilder.build());
            if (showRelevance) {
                /**
                 * @todo rather than adding relevance as a separate array, have
                 * relevance per item in the main "items" array.
                 */
                value.add("relevance", relevancePerResult.build());
            }
            if (showFacets) {
                value.add("facets", facets);
            }
            if (showSpellingAlternatives) {
                value.add("spelling_alternatives", spelling_alternatives);
            }
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

    private int getNumberOfResultsPerPage() {
        /**
         * @todo Raise the limit of results per page.
         */
        int numResultsPerPage = 10;
        return numResultsPerPage;
    }

    /**
     * @todo implement this!
     */
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
                }
            } else {
                // asc for alphabetical by default despite GitHub using desc by default:
                // "The sort order if sort parameter is provided. One of asc or desc. Default: desc"
                // http://developer.github.com/v3/search/
                sortOrder = SortBy.ASCENDING;
            }
        }

        List<String> allowedSortOrderValues = SortBy.allowedOrderStrings();
        if (!allowedSortOrderValues.contains(sortOrder)) {
            throw new Exception("The 'order' parameter was '" + sortOrder + "' but expected one of " + allowedSortOrderValues + ". (The 'sort' parameter was/became '" + sortField + "'.)");
        }

        return new SortBy(sortField, sortOrder);
    }

    private Dataverse getSubtree(String alias) throws Exception {
        if (true) {
            /**
             * @todo remove this "if true" after moving the subtree logic from
             * SearchIncludeFragment to SearchServiceBean
             */
            return dataverseService.findRootDataverse();
        }
        if (StringUtils.isBlank(alias)) {
            return dataverseService.findRootDataverse();
        } else {
            Dataverse subtree = dataverseService.findByAlias(alias);
            if (subtree != null) {
                return subtree;
            } else {
                throw new Exception("Could not find dataverse with alias" + alias);
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
