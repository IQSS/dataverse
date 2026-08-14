package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Uses Meilisearch for dataset ranking and Solr for permissions and result hydration.
 */
public class MeilisearchSearchServiceBean implements SearchService {

    static final String SERVICE_NAME = "meilisearch";
    static final String DEFAULT_INDEX = "datasets";
    static final int DEFAULT_CANDIDATE_LIMIT = 1000;
    static final int MAX_CANDIDATE_LIMIT = 1000;
    static final String PID_ATTRIBUTE = "pid";

    private static final Pattern BOOLEAN_OPERATOR = Pattern.compile("(^|\\s)(AND|OR|NOT)(\\s|$)");
    private static final Pattern UNSUPPORTED_SYNTAX = Pattern.compile("[:()\\[\\]{}~^?*\\\\&|!+/]");
    private static final int SOLR_DOCUMENTS_PER_DATASET = 2;
    private static final String NO_RESULTS_QUERY = SearchFields.ID + ":\"__meilisearch_no_results__\"";

    private SearchService solrSearchService;

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String getDisplayName() {
        return "Meilisearch";
    }

    @Override
    public void setSolrSearchService(SearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    @Override
    public SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
            List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
            boolean onlyDataRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
            String geoRadius, boolean addFacets, boolean addHighlights, boolean addCollections) throws SearchException {
        validateRequest(paginationStart, numResultsPerPage);

        List<String> effectiveFilterQueries = filterQueries == null ? List.of() : filterQueries;
        if (!supportsQuery(query) || onlyDataRelatedToMe || geoPoint != null || geoRadius != null
                || requiresNonDatasetResults(effectiveFilterQueries)) {
            return searchSolr(dataverseRequest, dataverses, query, effectiveFilterQueries, sortField, sortOrder,
                    paginationStart, onlyDataRelatedToMe, numResultsPerPage, retrieveEntities, geoPoint, geoRadius,
                    addFacets, addHighlights, addCollections);
        }

        int candidateLimit = getCandidateLimit();
        LinkedHashMap<String, Float> rankingByPid = queryMeilisearch(query, candidateLimit);
        if (rankingByPid.isEmpty()) {
            SolrQueryResponse response = searchSolr(dataverseRequest, dataverses, NO_RESULTS_QUERY,
                    effectiveFilterQueries, null, sortOrder, 0, onlyDataRelatedToMe, 1, retrieveEntities, null, null,
                    addFacets, false, addCollections);
            response.setResultsStart((long) paginationStart);
            response.setSpellingSuggestionsByToken(Map.of());
            return response;
        }

        String candidateQuery = buildCandidateQuery(rankingByPid.keySet().stream().toList());
        boolean useMeilisearchRanking = sortField == null || SearchFields.RELEVANCE.equals(sortField);
        int solrResultLimit = Math.max(rankingByPid.size(), rankingByPid.size() * SOLR_DOCUMENTS_PER_DATASET);
        SolrQueryResponse response = searchSolr(dataverseRequest, dataverses, candidateQuery, effectiveFilterQueries,
                useMeilisearchRanking ? null : sortField, sortOrder, 0, onlyDataRelatedToMe, solrResultLimit,
                retrieveEntities, null, null, addFacets, false, addCollections);

        List<SolrSearchResult> accessibleResults = new ArrayList<>(response.getSolrSearchResults());
        Map<String, Integer> rankByPid = new HashMap<>();
        int rank = 0;
        for (Map.Entry<String, Float> entry : rankingByPid.entrySet()) {
            rankByPid.put(entry.getKey(), rank++);
        }
        for (SolrSearchResult result : accessibleResults) {
            Float score = rankingByPid.get(result.getIdentifier());
            if (score != null) {
                result.setScore(score);
            }
            result.setHighlightsAsList(List.of());
            result.setHighlightsMap(Map.of());
            result.setHighlightsAsMap(Map.of());
        }
        if (useMeilisearchRanking) {
            Comparator<SolrSearchResult> ranking = Comparator.comparingInt(
                    result -> rankByPid.getOrDefault(result.getIdentifier(), Integer.MAX_VALUE));
            if (SortBy.ASCENDING.equals(sortOrder)) {
                ranking = ranking.reversed();
            }
            accessibleResults.sort(ranking);
        }

        long end = Math.min((long) paginationStart + numResultsPerPage, accessibleResults.size());
        List<SolrSearchResult> page = paginationStart >= accessibleResults.size()
                ? List.of()
                : new ArrayList<>(accessibleResults.subList(paginationStart, (int) end));
        response.setSolrSearchResults(page);
        response.setNumResultsFound((long) accessibleResults.size());
        response.setResultsStart((long) paginationStart);
        response.setSpellingSuggestionsByToken(Map.of());
        return response;
    }

    protected LinkedHashMap<String, Float> queryMeilisearch(String query, int candidateLimit) throws SearchException {
        String baseUrl = JvmSettings.MEILISEARCH_URL.lookupOptional()
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new SearchException(
                        JvmSettings.MEILISEARCH_URL.getScopedKey() + " is not configured", null));
        String index = JvmSettings.MEILISEARCH_INDEX.lookupOptional().filter(value -> !value.isBlank())
                .orElse(DEFAULT_INDEX);
        String apiKey = JvmSettings.MEILISEARCH_API_KEY.lookupOptional().orElse(null);
        JsonObject requestBody = buildRequest(query, candidateLimit);

        try (Client client = ClientBuilder.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()) {
            var request = client.target(baseUrl).path("indexes").path(index).path("search")
                    .request(MediaType.APPLICATION_JSON_TYPE);
            if (apiKey != null && !apiKey.isBlank()) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            try (Response response = request.post(Entity.entity(requestBody.toString(), MediaType.APPLICATION_JSON_TYPE))) {
                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    String message = response.hasEntity() ? response.readEntity(String.class) : "";
                    throw new SearchException("Meilisearch returned HTTP " + response.getStatus()
                            + (message.isBlank() ? "" : ": " + message), null);
                }
                try (var reader = Json.createReader(new StringReader(response.readEntity(String.class)))) {
                    return parseResponse(reader.readObject());
                }
            }
        } catch (ProcessingException | IllegalArgumentException | JsonException ex) {
            throw new SearchException("Unable to query Meilisearch", ex);
        }
    }

    static JsonObject buildRequest(String query, int candidateLimit) {
        String meilisearchQuery = query == null || query.isBlank() || "*".equals(query.trim()) ? "" : query;
        JsonArrayBuilder attributes = Json.createArrayBuilder().add(PID_ATTRIBUTE);
        return Json.createObjectBuilder()
                .add("q", meilisearchQuery)
                .add("offset", 0)
                .add("limit", candidateLimit)
                .add("attributesToRetrieve", attributes)
                .add("showRankingScore", true)
                .build();
    }

    static LinkedHashMap<String, Float> parseResponse(JsonObject response) throws SearchException {
        JsonArray hits = response.getJsonArray("hits");
        if (hits == null) {
            throw new SearchException("Meilisearch response does not contain a hits array", null);
        }

        LinkedHashMap<String, Float> rankingByPid = new LinkedHashMap<>();
        for (JsonValue value : hits) {
            if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new SearchException("Meilisearch returned a non-object hit", null);
            }
            JsonObject hit = value.asJsonObject();
            String pid = hit.getString(PID_ATTRIBUTE, null);
            if (pid == null || pid.isBlank()) {
                throw new SearchException("Meilisearch hit does not contain a non-empty " + PID_ATTRIBUTE, null);
            }
            float score = hit.containsKey("_rankingScore")
                    ? hit.getJsonNumber("_rankingScore").bigDecimalValue().floatValue()
                    : 0F;
            rankingByPid.putIfAbsent(pid, score);
        }
        return rankingByPid;
    }

    static boolean supportsQuery(String query) {
        if (query == null || query.isBlank() || "*".equals(query.trim())) {
            return true;
        }

        StringBuilder unquotedSyntax = new StringBuilder(query.length());
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < query.length(); i++) {
            char current = query.charAt(i);
            if (escaped) {
                escaped = false;
                unquotedSyntax.append(quoted ? ' ' : current);
            } else if (current == '\\') {
                escaped = true;
                unquotedSyntax.append(quoted ? ' ' : current);
            } else if (current == '"') {
                quoted = !quoted;
                unquotedSyntax.append(' ');
            } else {
                unquotedSyntax.append(quoted ? ' ' : current);
            }
        }
        if (quoted) {
            return false;
        }
        String syntax = unquotedSyntax.toString();
        return !UNSUPPORTED_SYNTAX.matcher(syntax).find() && !BOOLEAN_OPERATOR.matcher(syntax).find();
    }

    static String buildCandidateQuery(List<String> pids) {
        return SearchFields.IDENTIFIER + ":("
                + String.join(" OR ", pids.stream().map(MeilisearchSearchServiceBean::quote).toList()) + ")";
    }

    static boolean requiresNonDatasetResults(List<String> filterQueries) {
        for (String filterQuery : filterQueries) {
            String normalized = filterQuery.replaceAll("\\s", "").replace("\"", "");
            if (normalized.startsWith(SearchFields.TYPE + ":")
                    && !normalized.contains(SearchConstants.DATASETS)) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private int getCandidateLimit() throws SearchException {
        int candidateLimit = JvmSettings.MEILISEARCH_CANDIDATE_LIMIT.lookupOptional(Integer.class)
                .orElse(DEFAULT_CANDIDATE_LIMIT);
        if (candidateLimit < 1) {
            throw new SearchException(JvmSettings.MEILISEARCH_CANDIDATE_LIMIT.getScopedKey()
                    + " must be greater than zero", null);
        }
        if (candidateLimit > MAX_CANDIDATE_LIMIT) {
            throw new SearchException(JvmSettings.MEILISEARCH_CANDIDATE_LIMIT.getScopedKey()
                    + " must not be greater than " + MAX_CANDIDATE_LIMIT, null);
        }
        return candidateLimit;
    }

    private SolrQueryResponse searchSolr(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query,
            List<String> filterQueries, String sortField, String sortOrder, int paginationStart,
            boolean onlyDataRelatedToMe, int numResultsPerPage, boolean retrieveEntities, String geoPoint,
            String geoRadius, boolean addFacets, boolean addHighlights, boolean addCollections) throws SearchException {
        if (solrSearchService == null) {
            throw new SearchException("Solr search service is not configured", null);
        }
        return solrSearchService.search(dataverseRequest, dataverses, query, filterQueries, sortField, sortOrder,
                paginationStart, onlyDataRelatedToMe, numResultsPerPage, retrieveEntities, geoPoint, geoRadius,
                addFacets, addHighlights, addCollections);
    }

    private static void validateRequest(int paginationStart, int numResultsPerPage) {
        if (paginationStart < 0) {
            throw new IllegalArgumentException("paginationStart must be 0 or greater");
        }
        if (numResultsPerPage < 1) {
            throw new IllegalArgumentException("numResultsPerPage must be 1 or greater");
        }
    }
}
