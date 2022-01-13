package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.search.response.FacetCategory;
import edu.harvard.iq.dataverse.search.response.FacetLabel;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SearchDTOCreatorTest {

    // -------------------- TESTS --------------------

    @Test
    void create() {
        // given
        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery("query"));
        response.setSpellingSuggestionsByToken(Collections.singletonMap("e", Arrays.asList("ee", "eee")));
        response.setSolrSearchResults(SolrSearchResultDTOCreatorTest.createSolrResults());
        response.setFacetCategoryList(createFacetCategories());
        response.setNumResultsFound(4L);
        response.setResultsStart(1L);

        // when
        SearchDTO result = new SearchDTO.Creator().create(response);

        // then
        assertThat(result)
                .extracting(SearchDTO::getQ, SearchDTO::getTotalCount, SearchDTO::getCountInResponse, SearchDTO::getStart)
                .containsExactly("query", 4L, 4, 1L);
        assertThat(result.getSpellingAlternatives())
                .contains(entry("e", "[ee, eee]"));
        Map<String, SearchDTO.FacetCategoryDTO> facets = result.getFacets().get(0);
        assertThat(facets).containsOnlyKeys("Category 1", "Category 2");
        assertThat(facets.values().stream()
                .flatMap(f -> f.getLabels().stream()
                        .flatMap(l -> l.keySet().stream()))
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("Label 1", "Label 2", "Label 3");
    }

    // -------------------- PRIVATE --------------------

    private List<FacetCategory> createFacetCategories() {
        return Stream.of("Category 1", "Category 2")
                .map(c -> {
                    FacetCategory category = new FacetCategory();
                    category.setFriendlyName(c);
                    category.setName(c);
                    category.setFacetLabels(Stream.of(1L, 2L, 3L)
                            .map(n -> new FacetLabel("Label " + n, "Label " + n, n))
                            .collect(Collectors.toList()));
                    return category;
                })
                .collect(Collectors.toList());
    }
}