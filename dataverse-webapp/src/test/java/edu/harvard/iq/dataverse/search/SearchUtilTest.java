package edu.harvard.iq.dataverse.search;

import static edu.harvard.iq.dataverse.search.SearchFields.ADD_DATASET_PERM;
import static edu.harvard.iq.dataverse.search.SearchFields.DEFINITION_POINT;
import static edu.harvard.iq.dataverse.search.SearchFields.DEFINITION_POINT_DVOBJECT_ID;
import static edu.harvard.iq.dataverse.search.SearchFields.DISCOVERABLE_BY;
import static edu.harvard.iq.dataverse.search.SearchFields.DISCOVERABLE_BY_PUBLIC_FROM;
import static edu.harvard.iq.dataverse.search.SearchFields.ID;
import static edu.harvard.iq.dataverse.search.SearchFields.NAME_SORT;
import static edu.harvard.iq.dataverse.search.SearchFields.RELEASE_OR_CREATE_DATE;
import static edu.harvard.iq.dataverse.search.SearchFields.RELEVANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.sql.Timestamp;
import java.time.Instant;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.index.PermissionsSolrDoc;
import edu.harvard.iq.dataverse.search.index.SearchPermissions;
import edu.harvard.iq.dataverse.search.index.SolrPermission;
import edu.harvard.iq.dataverse.search.index.SolrPermissions;
import edu.harvard.iq.dataverse.search.query.SortBy;

public class SearchUtilTest {

    // -------------------- TESTS --------------------

    @Test
    void createSolrDoc__nullInput() {

        assertNull(SearchUtil.createSolrDoc(null));
    }

    @Test
    void createSolrDoc() {
        // given
        Long datasetVersionId = 345678L;

        // when
        SolrInputDocument solrInputDocument = SearchUtil.createSolrDoc(
                new PermissionsSolrDoc(12345L, "dataset_12345", datasetVersionId, "myNameOrTitleNotUsedHere",
                    new SolrPermissions(new SearchPermissions(singletonList("group_someAlias"), Instant.EPOCH),
                        new SolrPermission(Permission.AddDataset, singletonList("group_someOtherAlias")))));

        // then
        assertThat(solrInputDocument.get(ID).toString())
                .isEqualTo(ID + "=" + IndexServiceBean.solrDocIdentifierDataset + "12345"
                        + IndexServiceBean.discoverabilityPermissionSuffix);
        assertThat(solrInputDocument.get(DEFINITION_POINT).toString())
                .isEqualTo(DEFINITION_POINT + "=dataset_12345");
        assertThat(solrInputDocument.get(DEFINITION_POINT_DVOBJECT_ID).toString())
                .isEqualTo(DEFINITION_POINT_DVOBJECT_ID + "=12345");
        assertThat(solrInputDocument.get(DISCOVERABLE_BY).toString())
                .isEqualTo(DISCOVERABLE_BY + "=[group_someAlias]");
        assertThat(solrInputDocument.getField(DISCOVERABLE_BY_PUBLIC_FROM).getValue())
                .isEqualTo("1970-01-01T00:00:00Z");
        assertThat(solrInputDocument.get(ADD_DATASET_PERM).toString())
                .isEqualTo(ADD_DATASET_PERM + "=[group_someOtherAlias]");
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
                Arguments.of(null, null, RELEVANCE, SortOrder.desc),
                Arguments.of("name", null, NAME_SORT, SortOrder.asc),
                Arguments.of("date", null, RELEASE_OR_CREATE_DATE, SortOrder.desc),
                Arguments.of(DatasetFieldConstant.authorName, null, DatasetFieldConstant.authorName, SortOrder.asc));
    }

    @ParameterizedTest
    @MethodSource("sortByParameters")
    void getSortBy(String sortField, String sortOrder, String expectedSortField, 
            SortOrder expectedSortOrder) throws Exception {
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