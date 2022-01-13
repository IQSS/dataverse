package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.api.dto.SolrSearchResultDTOCreatorTest;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTest {

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private SearchServiceBean searchService;

    @Mock
    private DataverseDao dataverseDao;

    @Mock
    private HttpServletRequest request;

    @Mock
    private AuthenticationServiceBean authenticationService;

    @InjectMocks
    private Search endpoint = new Search();

    @BeforeEach
    void setUp() throws SearchException {
        Dataverse root = new Dataverse();
        when(request.getParameter("key")).thenReturn(null);
        when(authenticationService.lookupUser((String) isNull())).thenReturn(null);
        when(dataverseDao.findRootDataverse()).thenReturn(root);
        SolrQueryResponse solrQueryResponse = new SolrQueryResponse(new SolrQuery());
        solrQueryResponse.setSolrSearchResults(SolrSearchResultDTOCreatorTest.createSolrResults());
        solrQueryResponse.setNumResultsFound(4L);
        solrQueryResponse.setSpellingSuggestionsByToken(Collections.emptyMap());
        solrQueryResponse.setFacetCategoryList(Collections.emptyList());
        when(searchService.search(any(DataverseRequest.class), anyList(), anyString(), any(SearchForTypes.class),
                anyList(), anyString(), any(SearchServiceBean.SortOrder.class), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(solrQueryResponse);
    }

    // -------------------- TESTS --------------------

    @Test
    void search() throws AbstractApiBean.WrappedResponse {
        // given & when
        Response response = endpoint.search("query", Collections.emptyList(), Collections.emptyList(), null, null,
                10, 0, true, true, new ArrayList<>(),
                true, true);

        // then
        String result = (String) response.getEntity();
        assertThat(result).contains("OK", "200", "total_count", "items", "name", "Title", "facets", "count_in_response");
    }
}