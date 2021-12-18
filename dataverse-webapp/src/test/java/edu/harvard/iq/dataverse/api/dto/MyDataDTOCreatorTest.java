package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.mydata.RoleTagRetriever;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class MyDataDTOCreatorTest {

    @Mock
    private DataverseDao dataverseDao;

    @Mock
    private RoleTagRetriever roleTagRetriever;

    @Mock
    private DataverseRolePermissionHelper permissionHelper;

    @Mock
    private AuthenticatedUser user;

    private MyDataDTO.Creator creator;

    @BeforeEach
    void setUp() {
        creator = new MyDataDTO.Creator(dataverseDao, roleTagRetriever, permissionHelper);
        Mockito.when(permissionHelper.getRoleName(Mockito.anyLong()))
                .thenAnswer(invocation -> "Role" + invocation.getArgument(0));
        Mockito.when(user.getIdentifier()).thenReturn("user");
    }

    // -------------------- TESTS --------------------

    @Test
    void create() {
        // given
        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery());
        response.setSolrSearchResults(SolrSearchResultDTOCreatorTest.createSolrResults());

        // when
        MyDataDTO myData = creator.create(response,
                new Pager(20, 10, 1),
                new MyDataFilterParams(new DataverseRequest(user, (HttpServletRequest) null),
                        Arrays.asList("Dataverse", "Dataset", "DataFile"),
                        Arrays.asList("Published", "Draft"),
                        Arrays.asList(1L, 2L), ""));

        // then
        assertThat(myData.getPagination())
                .extracting(PagerDTO::getNumResultsString, PagerDTO::getEndResultNumberString)
                .containsExactly("20", "10");
        assertThat(myData.getItems())
                .extracting(SolrSearchResultDTO::getEntityId)
                .containsExactly(1L, 2L, 3L, 4L);
        assertThat(myData.getDvObjectCounts()).isNotNull();
        assertThat(myData.getPubStatusCounts()).isNotNull();
        assertThat(myData.getSelectedFilters())
                .extracting(f -> String.join(" ", f.getPublicationStatuses()),
                        f -> String.join(" ", f.getRoleNames()))
                .containsExactly("Published Draft", "Role1 Role2");
    }
}