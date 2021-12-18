package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.api.dto.MyDataDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.mydata.MyDataFinder;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.mydata.RoleTagRetriever;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;


/**
 * @author rmp553
 */
@Path("mydata")
public class MyData extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(MyData.class.getCanonicalName());

    public static final String MSG_NO_RESULTS_FOUND = BundleUtil.getStringFromBundle("dataretrieverAPI.noMsgResultsFound");

    private DataverseRoleServiceBean dataverseRoleService;
    private RoleAssigneeServiceBean roleAssigneeService;
    private DvObjectServiceBean dvObjectServiceBean;
    private SearchServiceBean searchService;
    private AuthenticationServiceBean authenticationService;
    private DataverseDao dataverseDao;

    // -------------------- CONSTRUCTORS --------------------

    public MyData() { }

    @Inject
    public MyData(DataverseRoleServiceBean dataverseRoleService, RoleAssigneeServiceBean roleAssigneeService,
                  DvObjectServiceBean dvObjectServiceBean, SearchServiceBean searchService,
                  AuthenticationServiceBean authenticationService, DataverseDao dataverseDao) {
        this.dataverseRoleService = dataverseRoleService;
        this.roleAssigneeService = roleAssigneeService;
        this.dvObjectServiceBean = dvObjectServiceBean;
        this.searchService = searchService;
        this.authenticationService = authenticationService;
        this.dataverseDao = dataverseDao;
    }

    // -------------------- LOGIC --------------------

    @Path("retrieve")
    @GET
    @Produces("application/json")
    public Response retrieveMyData(@QueryParam("dvobject_types") List<String> dvobject_types,
                                   @QueryParam("published_states") List<String> published_states,
                                   @QueryParam("selected_page") Integer selectedPage,
                                   @QueryParam("mydata_search_term") String searchTerm,
                                   @QueryParam("role_ids") List<Long> roleIds,
                                   @QueryParam("userIdentifier") String userIdentifier) throws WrappedResponse {
        boolean OTHER_USER = false;

        // For superusers the searchUser may differ from the authUser
        AuthenticatedUser authUser;
        AuthenticatedUser searchUser = null;
        authUser = findAuthenticatedUserOrDie();

        // If person is a superuser, see if a userIdentifier has been specified and use that instead
        if (authUser.isSuperuser() && StringUtils.isNotEmpty(userIdentifier)) {
            searchUser = getUserFromIdentifier(userIdentifier);
            if (searchUser == null) {
                return notFound(String.format("No user found for: \"%s\"", userIdentifier));
            }
            authUser = searchUser;
            OTHER_USER = true;
        }

        List<DataverseRole> roleList = dataverseRoleService.findAll();
        DataverseRolePermissionHelper rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

        List<String> dtypes = dvobject_types != null ? dvobject_types : MyDataFilterParams.defaultDvObjectTypes;

        // ---------------------------------
        // (1) Initialize filterParams and check for Errors
        // ---------------------------------
        DataverseRequest dataverseRequest = createDataverseRequest(authUser);

        MyDataFilterParams filterParams = new MyDataFilterParams(dataverseRequest, dtypes, published_states, roleIds, searchTerm);
        if (filterParams.hasError()) {
            return badRequest(filterParams.getErrorMessage());
        }

        // ---------------------------------
        // (2) Initialize MyDataFinder and check for Errors
        // ---------------------------------
        MyDataFinder myDataFinder = new MyDataFinder(rolePermissionHelper, roleAssigneeService, dvObjectServiceBean);
        myDataFinder.runFindDataSteps(filterParams);
        if (myDataFinder.hasError()) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, myDataFinder.getErrorMessage());
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = selectedPage != null ? selectedPage : 1;
        int solrCardStart = (paginationStart - 1) * SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;

        // Default the searchUser to the authUser.
        // The exception: for logged-in superusers, the searchUser may differ from the authUser
        if (searchUser == null) {
            searchUser = authUser;
        }
        SearchForTypes typesToSearch = filterParams.getSolrFragmentForDvObjectType();

        List<String> filterQueries = myDataFinder.getSolrFilterQueries();
        if (filterQueries == null) {
            logger.fine("No ids found for this search");
            return notFound(MyData.MSG_NO_RESULTS_FOUND);
        }

        SolrQueryResponse solrQueryResponse;
        try {
            solrQueryResponse = searchService.search(
                    dataverseRequest,
                    null, // subtree, default it to Dataverse for now
                    filterParams.getSearchTerm(),
                    typesToSearch,
                    filterQueries,
                    SearchFields.RELEASE_OR_CREATE_DATE, SortOrder.desc,
                    solrCardStart,
                    SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE,
                    false);

            if (solrQueryResponse.getNumResultsFound() == 0) {
                return notFound(MyData.MSG_NO_RESULTS_FOUND);
            }
        } catch (SearchException ex) {
            logger.severe("Solr SearchException: " + ex.getMessage());
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Sorry! There was an error with the search service.");
        }

        // ---------------------------------
        // (4) Build JSON document including: Pager, Formatted solr docs, Num results found, Search term,
        //      DvObject counts
        // ---------------------------------

        Pager pager = new Pager(
                solrQueryResponse.getNumResultsFound().intValue(), SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE, paginationStart);

        RoleTagRetriever roleTagRetriever = new RoleTagRetriever(rolePermissionHelper, roleAssigneeService, dvObjectServiceBean);
        roleTagRetriever.loadRoles(dataverseRequest, solrQueryResponse);

        MyDataDTO myDataDTO = new MyDataDTO.Creator(dataverseDao, roleTagRetriever, rolePermissionHelper)
                .create(solrQueryResponse, pager, filterParams);
        if (OTHER_USER) {
            myDataDTO.setOtherUser(searchUser.getIdentifier());
        }
        return ok(myDataDTO);
    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUser getUserFromIdentifier(String userIdentifier) {
        return userIdentifier == null || userIdentifier.isEmpty()
                ? null : authenticationService.getAuthenticatedUser(userIdentifier);
    }
}