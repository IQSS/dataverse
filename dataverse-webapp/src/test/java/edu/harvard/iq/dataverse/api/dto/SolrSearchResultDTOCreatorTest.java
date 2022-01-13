package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.mydata.RoleTagRetriever;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.response.SearchParentInfo;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


@ExtendWith(MockitoExtension.class)
public class SolrSearchResultDTOCreatorTest {

    @Mock
    private DataverseDao dataverseDao;

    @Mock
    private RoleTagRetriever roleTagRetriever;

    private SolrSearchResultDTO.Creator creator;

    @BeforeEach
    void setUp() {
        creator = new SolrSearchResultDTO.Creator(dataverseDao, roleTagRetriever);
    }

    // -------------------- TESTS --------------------

    @Test
    void createResultsForMyData() {
        // given
        Mockito.when(dataverseDao.getParentAliasesForIds(Mockito.anyList()))
                .thenAnswer(this::getParentAliasesForIds);
        Mockito.when(roleTagRetriever.getRolesForCard(Mockito.anyList()))
                .thenAnswer(this::getRolesForCard);
        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery());
        response.setSolrSearchResults(createSolrResults());

        // when
        List<SolrSearchResultDTO> results = creator.createResultsForMyData(response);

        // then
        assertThat(results).extracting(
                SolrSearchResultDTO::getEntityId,
                SolrSearchResultDTO::getName,
                SolrSearchResultDTO::getType,
                SolrSearchResultDTO::getParentAlias,
                r -> String.join(" ", r.getUserRoles()))
                .containsExactly(
                    tuple(1L, "Title", "dataset", "Parent2", "Role1 Role2"),
                    tuple(2L, "Name", "file", "Parent1", "Role1 Role2 Role3"),
                    tuple(3L, "Name", "dataverse", "Parent2", "Role1"),
                    tuple(4L, "Title", "dataset", "Parent1", "Role1 Role2"));
    }

    @Test
    void createResultsForSearch() {
        // given
        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery());
        response.setSolrSearchResults(createSolrResults());

        // when
        List<SolrSearchResultDTO> results = SolrSearchResultDTO.Creator.createResultsForSearch(response);

        // then
        assertThat(results).extracting(
                SolrSearchResultDTO::getEntityId,
                SolrSearchResultDTO::getName,
                SolrSearchResultDTO::getType)
                .containsExactly(
                        tuple(1L, "Title", "dataset"),
                        tuple(2L, "Name", "file"),
                        tuple(3L, "Name", "dataverse"),
                        tuple(4L, "Title", "dataset"));
    }

    // -------------------- LOGIC --------------------

    public static List<SolrSearchResult> createSolrResults() {
        Date creationDate = new Date();
        return LongStream.rangeClosed(1, 4).mapToObj(i -> {
            SolrSearchResult result = new SolrSearchResult();
            result.setType(SearchObjectType.values()[(int) i % 3]);
            result.setName("Name");
            result.setTitle("Title");
            result.setEntityId(i);
            result.setHighlightsMap(Collections.emptyMap());
            result.setReleaseOrCreateDate(creationDate);
            SearchParentInfo parentInfo = new SearchParentInfo();
            parentInfo.setId("3");
            result.setParent(parentInfo);
            return result;
        }).collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private Object getParentAliasesForIds(InvocationOnMock invocation) {
        List<Long> ids = invocation.getArgument(0);
        return ids.stream()
                .map(i -> new Object[]{i, "Parent" + (i % 2 + 1)})
                .collect(Collectors.toList());
    }

    private Object getRolesForCard(InvocationOnMock invocation) {
        List<Long> ids = invocation.getArgument(0);
        return ids.stream()
                .collect(Collectors.toMap(i -> i,
                        i -> LongStream.rangeClosed(1, i % 3 + 1)
                                .mapToObj(id -> "Role" + id)
                                .collect(Collectors.toList()),
                        (prev, next) -> next));
    }
}