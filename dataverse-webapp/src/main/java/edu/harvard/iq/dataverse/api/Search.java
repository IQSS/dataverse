package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.api.dto.SearchDTO;
import edu.harvard.iq.dataverse.api.dto.SolrSearchResultDTO;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchUtil;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SortBy;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    // -------------------- LOGIC --------------------

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
            @QueryParam("show_api_urls") boolean showApiUrls)
            throws WrappedResponse {
        if (query == null) {
            return allowCors(badRequest("q parameter is missing"));
        }

        User user = getUser();
        SearchForTypes typesToSearch = types.isEmpty() ? SearchForTypes.all() : getSearchForFromTypes(types);

        SortBy sortBy;
        try {
            sortBy = SearchUtil.getSortBy(sortField, sortOrder);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }

        int numResultsPerPage = getNumberOfResultsPerPage(numResultsPerPageRequested);

        List<Dataverse> dataverseSubtrees = new ArrayList<>();
        // we have to add "" (root) otherwise there is no permissions check
        if (subtrees.isEmpty()) {
            dataverseSubtrees.add(getSubtree(""));
        } else {
            for (String subtree : subtrees) {
                dataverseSubtrees.add(getSubtree(subtree));
            }
        }
        filterQueries.add(getFilterQueryFromSubtrees(dataverseSubtrees));

        if (filterQueries.isEmpty()) {
            return internalServerError("Filter is empty, which should never happen, as this allows unfettered searching of our index");
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
                                                     false);
        } catch (SearchException se) {
            Throwable cause = se;
            StringBuilder sb = new StringBuilder().append(cause).append(" ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName()).append(" ")
                        .append(cause).append(" ");
            }
            String message = String.format("Exception running search for [%s] with filterQueries %s and paginationStart [%d]: %s",
                    query, filterQueries, paginationStart, sb.toString());
            logger.info(message);
            return internalServerError(message);
        }

        SearchDTO dto = new SearchDTO.Creator().create(solrQueryResponse);
        removeUnnecessaryDataFromJson(dto, showRelevance, showEntityIds, showApiUrls, showFacets);
        return allowCors(ok(dto));
    }

    // -------------------- PRIVATE --------------------

    private void removeUnnecessaryDataFromJson(SearchDTO result,
                   boolean showRelevance, boolean showEntityIds, boolean showApiUrls, boolean showFacets) {
        if (!showFacets) {
            result.getFacets().clear();
        }
        if (showRelevance && showApiUrls && showEntityIds) {
            return;
        }
        for (SolrSearchResultDTO solrResult : result.getItems()) {
            if (!showRelevance) {
                solrResult.setMatches(Collections.emptyList());
                solrResult.setScore(null);
            }
            if (!showEntityIds) {
                solrResult.setEntityId(null);
            }
            if (!showApiUrls) {
                solrResult.setApiUrl(null);
            }
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
        return nonPublicSearchAllowed() ? userToExecuteSearchAs : GuestUser.get();
    }

    private boolean nonPublicSearchAllowed() {
        return settingsSvc.isTrueForKey(SettingsServiceBean.Key.SearchApiNonPublicAllowed);
    }

    private boolean tokenLessSearchAllowed() {
        boolean tokenLessSearchAllowed = !settingsSvc.isTrueForKey(SettingsServiceBean.Key.SearchApiRequiresToken);
        logger.fine("tokenLessSearchAllowed: " + tokenLessSearchAllowed);
        return tokenLessSearchAllowed;
    }

    private int getNumberOfResultsPerPage(int numResultsPerPage) {
        int maxLimit = 1000;
        if (numResultsPerPage == 0) {
            return 10; // default limit
        } else if (numResultsPerPage < 0) {
            throw new IllegalArgumentException(numResultsPerPage + " results per page requested but can not be less than zero.");
        } else if (numResultsPerPage > maxLimit) {
            throw new IllegalArgumentException(numResultsPerPage + " results per page requested but max limit is " + maxLimit + ".");
        } else {
            return numResultsPerPage;
        }
    }

    private SearchForTypes getSearchForFromTypes(List<String> types) throws WrappedResponse {
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
                throw new WrappedResponse(badRequest(String.format("Invalid type '%s'. Must be one of %s", type, validTypes)));
            }
        }
        return SearchForTypes.byTypes(typeRequested.toArray(new SearchObjectType[0]));
    }

    private String getFilterQueryFromSubtrees(List<Dataverse> subtrees) {
        String subtreesFilter = subtrees.stream()
                .filter(s -> !s.equals(dataverseDao.findRootDataverse()))
                .map(s -> "\"" + dataverseDao.determineDataversePath(s) + "\"")
                .collect(Collectors.joining(" OR "));

        if (StringUtils.isNotEmpty(subtreesFilter)) {
            subtreesFilter = SearchFields.SUBTREE + ":(" + subtreesFilter + ")";
        }

        return subtreesFilter;
    }

    private Dataverse getSubtree(String alias) throws WrappedResponse {
        if (StringUtils.isBlank(alias)) {
            return dataverseDao.findRootDataverse();
        }

        Dataverse subtree = dataverseDao.findByAlias(alias);
        if (subtree != null) {
            return subtree;
        } else {
            throw new WrappedResponse(notFound("Could not find dataverse with alias " + alias));
        }
    }
}
