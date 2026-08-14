package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LocalJvmSettings
class MeilisearchSearchServiceBeanTest {

    @Test
    void buildsPlaceholderSearchRequest() {
        JsonObject request = MeilisearchSearchServiceBean.buildRequest("*", 25);

        assertEquals("", request.getString("q"));
        assertEquals(0, request.getInt("offset"));
        assertEquals(25, request.getInt("limit"));
        assertEquals("pid", request.getJsonArray("attributesToRetrieve").getString(0));
        assertTrue(request.getBoolean("showRankingScore"));
    }

    @Test
    void recognizesSupportedAndUnsupportedQueries() {
        assertTrue(MeilisearchSearchServiceBean.supportsQuery("Darwin finches"));
        assertTrue(MeilisearchSearchServiceBean.supportsQuery("\"Darwin finches\" -sparrow"));
        assertTrue(MeilisearchSearchServiceBean.supportsQuery("*"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("title:finches"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("finches AND sparrows"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("finches && sparrows"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("+finches !sparrows"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("date:[2020 TO 2024]"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("escaped\\ query"));
        assertFalse(MeilisearchSearchServiceBean.supportsQuery("\"unterminated"));
    }

    @Test
    void detectsQueriesThatRequireNonDatasetResults() {
        assertTrue(MeilisearchSearchServiceBean.requiresNonDatasetResults(List.of("dvObjectType:(files)")));
        assertTrue(MeilisearchSearchServiceBean.requiresNonDatasetResults(List.of("dvObjectType:dataverses")));
        assertFalse(MeilisearchSearchServiceBean.requiresNonDatasetResults(List.of("dvObjectType:(datasets)")));
        assertFalse(MeilisearchSearchServiceBean.requiresNonDatasetResults(
                List.of("dvObjectType:(dataverses OR datasets OR files)")));
    }

    @Test
    void parsesRankedPidsAndKeepsFirstDuplicate() throws Exception {
        JsonObject response = Json.createObjectBuilder()
                .add("hits", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("pid", "doi:10.1/first").add("_rankingScore", 0.9))
                        .add(Json.createObjectBuilder().add("pid", "doi:10.1/second").add("_rankingScore", 0.8))
                        .add(Json.createObjectBuilder().add("pid", "doi:10.1/first").add("_rankingScore", 0.1)))
                .build();

        LinkedHashMap<String, Float> results = MeilisearchSearchServiceBean.parseResponse(response);

        assertEquals(List.of("doi:10.1/first", "doi:10.1/second"), results.keySet().stream().toList());
        assertEquals(0.9F, results.get("doi:10.1/first"));
    }

    @Test
    void rejectsMalformedHits() {
        JsonObject response = Json.createObjectBuilder()
                .add("hits", Json.createArrayBuilder().add(Json.createObjectBuilder().add("title", "No PID")))
                .build();

        assertThrows(SearchException.class, () -> MeilisearchSearchServiceBean.parseResponse(response));
    }

    @Test
    void hydratesWithSolrThenRanksAndPaginates() throws Exception {
        LinkedHashMap<String, Float> meilisearchResults = new LinkedHashMap<>();
        meilisearchResults.put("doi:10.1/first", 0.9F);
        meilisearchResults.put("doi:10.1/second", 0.8F);

        SolrSearchResult second = result("doi:10.1/second");
        SolrSearchResult first = result("doi:10.1/first");
        SolrQueryResponse solrResponse = response(List.of(second, first));
        RecordingSearchService solr = new RecordingSearchService(solrResponse);
        MeilisearchSearchServiceBean service = serviceReturning(meilisearchResults);
        service.setSolrSearchService(solr);

        SolrQueryResponse response = service.search(null, null, "finches", List.of("publicationStatus:Published"),
                SearchFields.RELEVANCE, SortBy.DESCENDING, 1, false, 1, false, null, null, true, true, false);

        assertEquals(List.of("doi:10.1/second"), response.getSolrSearchResults().stream()
                .map(SolrSearchResult::getIdentifier).toList());
        assertEquals(0.8F, response.getSolrSearchResults().get(0).getScore());
        assertEquals(2L, response.getNumResultsFound());
        assertEquals(1L, response.getResultsStart());
        assertTrue(solr.query.contains("\"doi:10.1/first\""));
        assertEquals(0, solr.paginationStart);
        assertEquals(4, solr.numResultsPerPage);
        assertFalse(solr.addHighlights);
        assertTrue(response.getSolrSearchResults().get(0).getHighlightsMap().isEmpty());
        assertEquals(0, response.getSolrSearchResults().get(0).getRelevance().build().size());
    }

    @Test
    void reversesMeilisearchRankingForAscendingRelevance() throws Exception {
        LinkedHashMap<String, Float> meilisearchResults = new LinkedHashMap<>();
        meilisearchResults.put("doi:10.1/first", 0.9F);
        meilisearchResults.put("doi:10.1/second", 0.8F);
        RecordingSearchService solr = new RecordingSearchService(
                response(List.of(result("doi:10.1/first"), result("doi:10.1/second"))));
        MeilisearchSearchServiceBean service = serviceReturning(meilisearchResults);
        service.setSolrSearchService(solr);

        SolrQueryResponse response = service.search(null, null, "finches", List.of(), SearchFields.RELEVANCE,
                SortBy.ASCENDING, 0, false, 2, false, null, null, false, false, false);

        assertEquals(List.of("doi:10.1/second", "doi:10.1/first"), response.getSolrSearchResults().stream()
                .map(SolrSearchResult::getIdentifier).toList());
    }

    @Test
    void usesSolrToBuildACompleteEmptyResponse() throws Exception {
        SolrQueryResponse solrResponse = response(List.of());
        RecordingSearchService solr = new RecordingSearchService(solrResponse);
        MeilisearchSearchServiceBean service = serviceReturning(new LinkedHashMap<>());
        service.setSolrSearchService(solr);

        SolrQueryResponse response = service.search(null, null, "nothing", List.of(), SearchFields.RELEVANCE,
                SortBy.DESCENDING, 5, false, 10, false, null, null, true, true, false);

        assertSame(solrResponse, response);
        assertEquals("id:\"__meilisearch_no_results__\"", solr.query);
        assertEquals(0, solr.paginationStart);
        assertEquals(5L, response.getResultsStart());
        assertFalse(solr.addHighlights);
    }

    @Test
    void delegatesAdvancedQueriesDirectlyToSolr() throws Exception {
        SolrQueryResponse solrResponse = response(List.of());
        RecordingSearchService solr = new RecordingSearchService(solrResponse);
        MeilisearchSearchServiceBean service = serviceReturning(new LinkedHashMap<>());
        service.setSolrSearchService(solr);

        SolrQueryResponse response = service.search(null, null, "title:finches", List.of(), SearchFields.RELEVANCE,
                SortBy.DESCENDING, 3, false, 10, false, null, null, true, true, false);

        assertSame(solrResponse, response);
        assertEquals("title:finches", solr.query);
        assertEquals(3, solr.paginationStart);
        assertEquals(10, solr.numResultsPerPage);
        assertTrue(solr.addHighlights);
    }

    private static MeilisearchSearchServiceBean serviceReturning(LinkedHashMap<String, Float> results) {
        return new MeilisearchSearchServiceBean() {
            @Override
            protected LinkedHashMap<String, Float> queryMeilisearch(String query, int candidateLimit) {
                return results;
            }
        };
    }

    private static SolrSearchResult result(String pid) {
        SolrSearchResult result = new SolrSearchResult("finches", pid);
        result.setIdentifier(pid);
        return result;
    }

    private static SolrQueryResponse response(List<SolrSearchResult> results) {
        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery("test"));
        response.setSolrSearchResults(results);
        response.setNumResultsFound((long) results.size());
        response.setResultsStart(0L);
        response.setSpellingSuggestionsByToken(Map.of());
        response.setFacetCategoryList(List.of());
        response.setTypeFacetCategories(List.of());
        return response;
    }

    private static class RecordingSearchService implements SearchService {
        private final SolrQueryResponse response;
        private String query;
        private int paginationStart;
        private int numResultsPerPage;
        private boolean addHighlights;

        private RecordingSearchService(SolrQueryResponse response) {
            this.response = response;
        }

        @Override
        public String getServiceName() {
            return "recording";
        }

        @Override
        public String getDisplayName() {
            return "Recording";
        }

        @Override
        public SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
                List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
                boolean onlyDataRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
                String geoRadius, boolean addFacets, boolean addHighlights, boolean addCollections) {
            this.query = query;
            this.paginationStart = paginationStart;
            this.numResultsPerPage = numResultsPerPage;
            this.addHighlights = addHighlights;
            return response;
        }
    }
}
