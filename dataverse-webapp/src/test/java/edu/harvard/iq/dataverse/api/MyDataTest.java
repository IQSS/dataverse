package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.api.dto.SolrSearchResultDTOCreatorTest;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyDataTest {

    @Mock
    private DataverseRoleServiceBean dataverseRoleService;

    @Mock
    private RoleAssigneeServiceBean roleAssigneeService;

    @Mock
    private DvObjectServiceBean dvObjectServiceBean;

    @Mock
    private SearchServiceBean searchService;

    @Mock
    private AuthenticationServiceBean authenticationService;

    @Mock
    private DataverseDao dataverseDao;

    @Mock
    private HttpServletRequest request;

    @Mock
    protected SystemConfig systemConfig;

    @InjectMocks
    private MyData endpoint = new MyData(dataverseRoleService, roleAssigneeService, dvObjectServiceBean,
            searchService, authenticationService, dataverseDao);

    private AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    @BeforeEach
    void setUp() throws SearchException {
        String token = "123456";
        authenticatedUser.setUserIdentifier("user");
        when(request.getParameter("key")).thenReturn(token);
        when(authenticationService.lookupUser(token)).thenReturn(authenticatedUser);
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        when(roleAssigneeService.getAssigneeAndRoleIdListFor(any(MyDataFilterParams.class)))
                .thenReturn(toListOfArrays(2, 1L, 1L, 2L, 1L, 3L, 1L, 4L, 1L));
        when(dvObjectServiceBean.getDvObjectInfoForMyData(anyList()))
                .thenReturn(toListOfArrays(3,
                        1, "Dataset", 3L,
                        2, "DataFile", 3L,
                        3, "Dataverse", null,
                        4, "Dataset", 3L));
        SolrQueryResponse solrQueryResponse = new SolrQueryResponse(new SolrQuery());
        solrQueryResponse.setSolrSearchResults(SolrSearchResultDTOCreatorTest.createSolrResults());
        solrQueryResponse.setNumResultsFound(4L);
        when(searchService.search(any(DataverseRequest.class), isNull(), anyString(), any(SearchForTypes.class),
                anyList(), anyString(), any(SearchServiceBean.SortOrder.class), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(solrQueryResponse);
    }

    @Test
    void retrieveMyData() throws WrappedResponse {
        // given & when
        Response response = endpoint.retrieveMyData(Arrays.asList("Dataverse", "Dataset", "DataFile"),
                Arrays.asList("Published"), 1, "",
                Arrays.asList(1L, 2L), "user");

        // then
        String result = (String) response.getEntity();
        assertThat(result).contains("pagination", "docsPerPage",
                "items", "name", "Title",
                "dvobject_counts",
                "selected_filters", "Published");
    }

    // -------------------- PRIVATE --------------------

    private List<Object[]> toListOfArrays(int size, Object... elements) {
        List<Object[]> result = new ArrayList<>();
        for (int i = 0; i < elements.length; i = i + size) {
            Object[] listElement = new Object[size];
            System.arraycopy(elements, i, listElement, 0, size);
            result.add(listElement);
        }
        return result;
    }
}