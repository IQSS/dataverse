package edu.harvard.iq.dataverse.search.dataverselookup;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.query.PermissionFilterQueryBuilder;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.HighlightParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Stateless
public class DataverseLookupService {
    private static final Logger logger = LoggerFactory.getLogger(DataverseLookupService.class);

    private static final String HIGHLIGHT_PRE = "<b>";
    private static final String HIGHLIGHT_POST = "</b>";

    private SolrClient solrClient;
    private PermissionFilterQueryBuilder permissionFilterQueryBuilder;
    private SolrQuerySanitizer querySanitizer;
    private DataverseDao dataverseDao;
    private PermissionServiceBean permissionService;

    private LookupData rootData;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseLookupService() { }

    @Inject
    public DataverseLookupService(SolrClient solrClient, PermissionFilterQueryBuilder permissionFilterQueryBuilder,
                                  SolrQuerySanitizer querySanitizer, DataverseDao dataverseDao,
                                  PermissionServiceBean permissionService) {
        this.solrClient = solrClient;
        this.permissionFilterQueryBuilder = permissionFilterQueryBuilder;
        this.querySanitizer = querySanitizer;
        this.dataverseDao = dataverseDao;
        this.permissionService = permissionService;
    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void init() {
        this.rootData = createRootData();
    }

    public List<LookupData> fetchLookupData(String query, LookupPermissions lookupPermissions) {
        String processedQuery = processQuery(query);
        SolrQuery solrQuery = createSolrQuery(processedQuery, lookupPermissions);
        QueryResponse response = querySolr(solrQuery);
        if (response == null) {
            return Collections.emptyList();
        }
        List<SolrDocument> rawResults = new ArrayList<>(response.getResults());
        Map<String, HighlightedData> highlighting = processHighlighting(response);

        // Fetch the rest of solr results (if they exist)
        int allFound = (int) response.getResults().getNumFound();
        int currentlyFound = rawResults.size();
        int rest = allFound - currentlyFound;
        if (rest > 0) {
            solrQuery.setRows(rest)
                    .setStart(currentlyFound);
            response = querySolr(solrQuery);
            if (response == null) {
                return Collections.emptyList();
            }
            rawResults.addAll(response.getResults());
            highlighting.putAll(processHighlighting(response));
        }
        List<EntryData> processedData = processRawData(rawResults, highlighting);

        // Find out, which second-level parents (ie. parents of parents) data
        // we need to fetch (some is probably already available)
        Set<Long> existingDataForParents = new HashSet<>();
        Set<Long> dataForParentsToFetch = new HashSet<>();
        for (EntryData entry : processedData) {
            existingDataForParents.add(entry.id);
            dataForParentsToFetch.add(entry.parentId);
        }
        dataForParentsToFetch.removeAll(existingDataForParents);
        List<EntryData> additionalDataForParents = new ArrayList<>();
        if (!dataForParentsToFetch.isEmpty()) {
            QueryResponse dataForParentsResponse = querySolr(createSolrQueryForParents(processedQuery, dataForParentsToFetch));
            if (dataForParentsResponse == null) {
                return Collections.emptyList();
            }
            highlighting.putAll(processHighlighting(dataForParentsResponse));
            additionalDataForParents.addAll(processRawDataForParents(dataForParentsResponse.getResults(), highlighting));
        }

        // Use all the data we have for creating parents index
        Map<Long, EntryData> indexForParents = Stream.concat(processedData.stream(), additionalDataForParents.stream())
                .collect(Collectors.toMap(e -> e.id, e -> e, (prev, next) -> next));
        return createResults(processedData, indexForParents, isRootQueried(query) && lookupPermissions.isRootPermitted());
    }

    public LookupPermissions createLookupPermissions(DataverseRequest dataverseRequest) {
        String permissionFilterQuery = permissionFilterQueryBuilder.buildPermissionFilterQueryForAddDataset(dataverseRequest);
        Dataverse rootDataverse = dataverseDao.findRootDataverse();
        boolean rootPermitted = permissionService.requestOn(dataverseRequest, rootDataverse).has(Permission.AddDataset);
        return new LookupPermissions(permissionFilterQuery, rootPermitted);
    }

    // -------------------- PRIVATE --------------------

    private LookupData createRootData() {
        Dataverse rootDataverse = dataverseDao.findRootDataverse();
        return new LookupData(rootDataverse.getId(), addHighlightTags("root"),
                addHighlightTags("Root"), StringUtils.EMPTY, StringUtils.EMPTY);
    }

    private String processQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return "*";
        }
        String[] toProcess = querySanitizer.removeSolrSpecialChars(query)
                .split("\\s+");
        String processed = Arrays.stream(toProcess)
                .filter(StringUtils::isNotBlank)
                .map(s -> s + "*")
                .collect(Collectors.joining(" OR "));
        return toProcess.length > 1 ? String.format("(%s)", processed) : processed;
    }

    private SolrQuery createSolrQuery(String queryToSolr, LookupPermissions lookupPermissions) {
        String typeQuery = String.format(" AND %s:%s", SearchFields.TYPE, SearchObjectType.DATAVERSES.getSolrValue());
        return new SolrQuery(queryToSolr + typeQuery)
                .addFilterQuery(lookupPermissions.getPermissionFilterQuery())
                .setFields(SearchFields.ID, SearchFields.ENTITY_ID, SearchFields.IDENTIFIER, SearchFields.NAME,
                        SearchFields.PARENT_ID, SearchFields.PARENT_NAME)
                .addHighlightField(SearchFields.IDENTIFIER)
                .addHighlightField(SearchFields.NAME)
                .addHighlightField(SearchFields.PARENT_NAME)
                .setHighlightSimplePre(HIGHLIGHT_PRE).setHighlightSimplePost(HIGHLIGHT_POST);
    }

    private QueryResponse querySolr(SolrQuery solrQuery) {
        try {
            return solrClient.query(solrQuery);
        } catch (SolrServerException | IOException ex) {
            logger.warn("Exception during solr query: ", ex);
            return null;
        }
    }

    private SolrQuery createSolrQueryForParents(String queryToSolr, Set<Long> parentIds) {
        String typeQuery = String.format(" AND %s:%s", SearchFields.TYPE, SearchObjectType.DATAVERSES.getSolrValue());
        String ids = parentIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" OR "));
        return new SolrQuery(String.format("%s:(%s)", SearchFields.ENTITY_ID, ids) + typeQuery)
                .setFields(SearchFields.ID, SearchFields.ENTITY_ID, SearchFields.PARENT_NAME)
                .setParam(HighlightParams.Q, queryToSolr)
                .addHighlightField(SearchFields.PARENT_NAME)
                .setHighlightSimplePre(HIGHLIGHT_PRE).setHighlightSimplePost(HIGHLIGHT_POST);
    }

    private List<LookupData> createResults(List<EntryData> entries, Map<Long, EntryData> indexForParents, boolean includeRoot) {
        List<LookupData> lookupData = new ArrayList<>(entries.size() + (includeRoot ? 1 : 0));
        // We have to manually include root dataverse, as it's not present in solr
        if (includeRoot) {
            lookupData.add(rootData);
        }
        for (EntryData entryData : entries) {
            EntryData upperParent = indexForParents.get(entryData.parentId);
            lookupData.add(new LookupData(entryData.id, entryData.identifier, entryData.name, entryData.parentName,
                    upperParent != null ? upperParent.parentName : StringUtils.EMPTY));
        }
        return lookupData;
    }

    private boolean isRootQueried(String query) {
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(StringUtils::isNotBlank)
                .anyMatch(q -> q.matches("ro|roo|root"));
    }

    private String addHighlightTags(String value) {
        return HIGHLIGHT_PRE + value + HIGHLIGHT_POST;
    }

    private List<EntryData> processRawData(List<SolrDocument> rawData, Map<String, HighlightedData> highlightedDataMap) {
        List<EntryData> processedData = new ArrayList<>();
        HighlightedData emptyHighlighting = new HighlightedData(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        for (SolrDocument document : rawData) {
            Long id = (Long) document.getFieldValue(SearchFields.ENTITY_ID);
            String solrId = (String) document.getFieldValue(SearchFields.ID);
            String identifier = (String) document.getFieldValue(SearchFields.IDENTIFIER);
            String name = (String) document.getFieldValue(SearchFields.NAME);
            String parentIdString = (String) document.getFieldValue(SearchFields.PARENT_ID);
            String parentName = (String) document.getFieldValue(SearchFields.PARENT_NAME);
            HighlightedData highlightedData = highlightedDataMap.get(solrId);
            highlightedData = highlightedData == null ? emptyHighlighting : highlightedData;
            EntryData entryData = new EntryData(id, solrId,
                    StringUtils.isNotBlank(highlightedData.identifier) ? highlightedData.identifier : identifier,
                    StringUtils.isNotBlank(highlightedData.name) ? highlightedData.name : name,
                    StringUtils.isNotBlank(parentIdString) ? Long.parseLong(parentIdString) : -1L,
                    StringUtils.isNotBlank(highlightedData.parentName) ? highlightedData.parentName : parentName);
            processedData.add(entryData);
        }
        return processedData;
    }

    private List<EntryData> processRawDataForParents(List<SolrDocument> rawData, Map<String, HighlightedData> highlightedDataMap) {
        List<EntryData> processedData = new ArrayList<>();
        HighlightedData emptyHighlighting = new HighlightedData(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        for (SolrDocument document : rawData) {
            Long id = (Long) document.getFieldValue(SearchFields.ENTITY_ID);
            String solrId = (String) document.getFieldValue(SearchFields.ID);
            String parentName = (String) document.getFieldValue(SearchFields.PARENT_NAME);
            HighlightedData highlightedData = highlightedDataMap.get(solrId);
            highlightedData = highlightedData == null ? emptyHighlighting : highlightedData;
            EntryData entryData = new EntryData(id, solrId, StringUtils.EMPTY, StringUtils.EMPTY, -1L,
                    StringUtils.isNotBlank(highlightedData.parentName) ? highlightedData.parentName : parentName);
            processedData.add(entryData);
        }
        return processedData;
    }

    private Map<String, HighlightedData> processHighlighting(QueryResponse response) {
        Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();
        if (highlighting == null || highlighting.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, HighlightedData> processedHighlighting = new HashMap<>();
        for (Map.Entry<String, Map<String, List<String>>> entry : highlighting.entrySet()) {
            Map<String, List<String>> valuesMap = entry.getValue();
            processedHighlighting.put(entry.getKey(), new HighlightedData(
                            getValue(SearchFields.IDENTIFIER, valuesMap),
                            getValue(SearchFields.NAME, valuesMap),
                            getValue(SearchFields.PARENT_NAME, valuesMap)));
        }
        return processedHighlighting;
    }

    private String getValue(String fieldName, Map<String, List<String>> data) {
        List<String> values = data.get(fieldName);
        return values == null || values.isEmpty() ? StringUtils.EMPTY : values.get(0);
    }

    // -------------------- INNER CLASSES --------------------

    private static class HighlightedData {
        public final String identifier;
        public final String name;
        public final String parentName;

        public HighlightedData(String identifier, String name, String parentName) {
            this.identifier = identifier;
            this.name = name;
            this.parentName = parentName;
        }
    }

    private static class EntryData {
        public final Long id;
        public final String solrId;
        public final String identifier;
        public final String name;
        public final Long parentId;
        public final String parentName;

        public EntryData(Long id, String solrId, String identifier, String name, Long parentId, String parentName) {
            this.id = id;
            this.solrId = solrId;
            this.identifier = identifier;
            this.name = name;
            this.parentId = parentId;
            this.parentName = parentName;
        }
    }

    public static class LookupPermissions {
        private final String permissionFilterQuery;
        private final boolean rootPermitted;

        // -------------------- CONSTRUCTORS --------------------

        public LookupPermissions(String permissionFilterQuery, boolean rootPermitted) {
            this.permissionFilterQuery = permissionFilterQuery;
            this.rootPermitted = rootPermitted;
        }

        // -------------------- GETTERS --------------------

        public String getPermissionFilterQuery() {
            return permissionFilterQuery;
        }

        public boolean isRootPermitted() {
            return rootPermitted;
        }
    }
}