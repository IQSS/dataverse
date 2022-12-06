package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.index.PermissionsSolrDoc;
import edu.harvard.iq.dataverse.search.index.SearchPermissions;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.index.SolrPermission;
import edu.harvard.iq.dataverse.search.index.SolrPermissions;
import edu.harvard.iq.dataverse.search.query.SortBy;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

public class SearchUtilTest {

    // -------------------- TESTS --------------------

    @Test
    void createSolrDoc__nullInput() {
        // when
        SolrInputDocument solrDoc = SearchUtil.createSolrDoc(null);

        // then
        assertThat((Map<?,?>) solrDoc).isNull();
    }

    @Test
    void createSolrDoc() {
        // given
        Long datasetVersionId = 345678L;

        // when
        SolrInputDocument solrInputDocument = SearchUtil.createSolrDoc(
                new PermissionsSolrDoc(12345L, "dataset_12345", datasetVersionId, "myNameOrTitleNotUsedHere",
                    new SolrPermissions(new SearchPermissions(Collections.singletonList("group_someAlias"), Instant.EPOCH),
                        new SolrPermission(Permission.AddDataset, Collections.singletonList("group_someOtherAlias")))));

        // then
        assertThat(solrInputDocument.get(SearchFields.ID).toString())
                .isEqualTo(SearchFields.ID + "=" + IndexServiceBean.solrDocIdentifierDataset + "12345"
                        + IndexServiceBean.discoverabilityPermissionSuffix);
        assertThat(solrInputDocument.get(SearchFields.DEFINITION_POINT).toString())
                .isEqualTo(SearchFields.DEFINITION_POINT + "=dataset_12345");
        assertThat(solrInputDocument.get(SearchFields.DEFINITION_POINT_DVOBJECT_ID).toString())
                .isEqualTo(SearchFields.DEFINITION_POINT_DVOBJECT_ID + "=12345");
        assertThat(solrInputDocument.get(SearchFields.DISCOVERABLE_BY).toString())
                .isEqualTo(SearchFields.DISCOVERABLE_BY + "=[group_someAlias]");
        assertThat(solrInputDocument.getField(SearchFields.DISCOVERABLE_BY_PUBLIC_FROM).getValue())
                .isEqualTo("1970-01-01T00:00:00Z");
        assertThat(solrInputDocument.get(SearchFields.ADD_DATASET_PERM).toString())
                .isEqualTo(SearchFields.ADD_DATASET_PERM + "=[group_someOtherAlias]");
    }


    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "           | ",
            "987654321  | 1970-01-12T10:20:54Z"
    })
    void getTimestampOrNull(Long input, String expected) {
        // given
        Timestamp inputTimestamp = input != null ? new Timestamp(input) : null;

        // when
        String timestamp = SearchUtil.getTimestampOrNull(inputTimestamp);

        // then
        assertThat(timestamp).isEqualTo(expected);
    }

    private static Stream<Arguments> sortByParameters() {
        return Stream.of(
                Arguments.of(null, null, SearchFields.RELEVANCE, SortOrder.desc),
                Arguments.of("name", null, SearchFields.NAME_SORT, SortOrder.asc),
                Arguments.of("date", null, SearchFields.RELEASE_OR_CREATE_DATE, SortOrder.desc),
                Arguments.of(DatasetFieldConstant.authorName, null, DatasetFieldConstant.authorName, SortOrder.asc));
    }

    @ParameterizedTest
    @MethodSource("sortByParameters")
    void getSortBy(String sortField, String sortOrder, String expectedSortField, SortOrder expectedSortOrder) throws Exception {
        // when
        SortBy sortBy = SearchUtil.getSortBy(sortField, sortOrder);

        // then
        assertThat(sortBy.getField()).isEqualTo(expectedSortField);
        assertThat(sortBy.getOrder()).isEqualTo(expectedSortOrder);
    }

    @Test
    void getSortBy__wrongInput() {
        // when & then
        catchThrowableOfType(() -> SearchUtil.getSortBy(null, "unsortable"), Exception.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter='|', value = {
            "       |   *   ",
            "''     |   *   ",
            "foo    |   foo "
    })
    void determineFinalQuery(String input, String expectedQuery) {
        // when
        String query = SearchUtil.determineFinalQuery(input);

        // then
        assertThat(query).isEqualTo(expectedQuery);
    }
}