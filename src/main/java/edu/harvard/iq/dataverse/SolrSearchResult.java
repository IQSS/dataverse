package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.search.Highlight;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class SolrSearchResult {

    private static final Logger logger = Logger.getLogger(SolrSearchResult.class.getCanonicalName());

    private String id;
    private Long entityId;
    private String type;
    private String query;
    private String name;
    private String nameSort;
    private String status;
    private Date releaseOrCreateDate;

    /**
     * @todo: how important is it to differentiate between name and title?
     */
    private String title;
    private String descriptionNoSnippet;
    private List<Highlight> highlightsAsList = new ArrayList<>();
    private Map<SolrField, Highlight> highlightsMap;
    private Map<String, Highlight> highlightsAsMap;

    // parent can be dataverse or dataset, store the name and id
    private Map<String, String> parent;
    // used on the SearchPage but not the search API
    private List<Dataset> datasets;
    private String dataverseAffiliation;
    private String citation;
    private String filetype;
    /**
     * @todo: used? remove
     */
    private List<String> matchedFields;

    /**
     * @todo: remove name?
     */
    SolrSearchResult(String queryFromUser, String name) {
        this.query = queryFromUser;
//        this.name = name;
    }

    public Map<String, Highlight> getHighlightsAsMap() {
        return highlightsAsMap;
    }

    public void setHighlightsAsMap(Map<String, Highlight> highlightsAsMap) {
        this.highlightsAsMap = highlightsAsMap;
    }

    public String getNameHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.NAME);
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public String getDataverseAffiliationHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.AFFILIATION);
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public String getFileTypeHighlightSnippet() {
        Highlight highlight = highlightsAsMap.get(SearchFields.FILE_TYPE_MIME);
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public String getTitleHighlightSnippet() {
        /**
         * @todo: don't hard-code title, look it up properly... or start
         * indexing titles as names:
         * https://redmine.hmdc.harvard.edu/issues/3798#note-2
         */
        Highlight highlight = highlightsAsMap.get("title");
        if (highlight != null) {
            String firstSnippet = highlight.getSnippets().get(0);
            if (firstSnippet != null) {
                return firstSnippet;
            }
        }
        return null;
    }

    public List<String> getDescriptionSnippets() {
        for (Map.Entry<SolrField, Highlight> entry : highlightsMap.entrySet()) {
            SolrField solrField = entry.getKey();
            Highlight highlight = entry.getValue();
            logger.info("SolrSearchResult class: " + solrField.getNameSearchable() + ":" + highlight.getSnippets());
        }

        /**
         * @todo: use SearchFields.DESCRIPTION
         */
        Highlight highlight = highlightsAsMap.get("description");
        if (highlight != null) {
            return highlight.getSnippets();
        } else {
            return new ArrayList<>();
        }
    }

    public Map<SolrField, Highlight> getHighlightsMap() {
        return highlightsMap;
    }

    public void setHighlightsMap(Map<SolrField, Highlight> highlightsMap) {
        this.highlightsMap = highlightsMap;
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }

    @Override
    public String toString() {
        if (this.name != null) {
            return this.id + ":" + this.name + ":" + this.entityId;
        } else {
            return this.id + ":" + this.title + ":" + this.entityId;
        }
    }

    public JsonObject getRelevance() {
        JsonArrayBuilder detailsArrayBuilder = Json.createArrayBuilder();
        for (Highlight highlight : highlightsAsList) {
            JsonArrayBuilder snippetArrayBuilder = Json.createArrayBuilder();
            for (String snippet : highlight.getSnippets()) {
                snippetArrayBuilder.add(snippet);
            }
            JsonObjectBuilder detailsObjectBuilder = Json.createObjectBuilder();
            detailsObjectBuilder.add(highlight.getSolrField().getNameSearchable(), snippetArrayBuilder);
            detailsArrayBuilder.add(detailsObjectBuilder);
        }

        JsonObjectBuilder detailsObjectBuilder = Json.createObjectBuilder();
        for (Map.Entry<SolrField, Highlight> entry : highlightsMap.entrySet()) {
            SolrField solrField = entry.getKey();
            Highlight snippets = entry.getValue();
            JsonArrayBuilder snippetArrayBuilder = Json.createArrayBuilder();
            for (String highlight : snippets.getSnippets()) {
                snippetArrayBuilder.add(highlight);
                detailsObjectBuilder.add(solrField.getNameSearchable(), snippetArrayBuilder);
            }
        }
        JsonObject jsonObject = Json.createObjectBuilder()
                .add(SearchFields.ID, this.id)
                .add("matched_fields", this.matchedFields.toString())
                .add("detailsArray", detailsArrayBuilder)
                //                .add("detailsObject", detailsObjectBuilder)
                .build();
        return jsonObject;
    }

    public JsonObject toJsonObject() {
        JsonObjectBuilder parentBuilder = Json.createObjectBuilder();
        for (String key : parent.keySet()) {
            parentBuilder.add(key, parent.get(key) != null ? parent.get(key) : "NONE-ROOT-DATAVERSE-OR-EMPTY-STRING-DATASET-TITLE-OR-GROUP");
        }
        JsonObjectBuilder typeSpecificFields = Json.createObjectBuilder();
        if (this.type.equals("dataverses")) {
            typeSpecificFields.add(SearchFields.NAME, this.name);
//            typeSpecificFields.add(SearchFields.AFFILIATION, dataverseAffiliation);
        } else if (this.type.equals("datasets")) {
//            typeSpecificFields.add(SearchFields.TITLE, this.title);
            /**
             * @todo: don't hard code this
             */
            typeSpecificFields.add("title_s", this.title);
        } else if (this.type.equals("files")) {
            typeSpecificFields.add(SearchFields.NAME, this.name);
            typeSpecificFields.add(SearchFields.FILE_TYPE_MIME, this.filetype);
        }
        JsonObject jsonObject = Json.createObjectBuilder()
                .add(SearchFields.ID, this.id)
                .add(SearchFields.ENTITY_ID, this.entityId)
                .add(SearchFields.TYPE, this.type)
                .add(SearchFields.NAME_SORT, this.nameSort)
                .add("matched_fields", this.matchedFields.toString())
                .add("parent", parentBuilder)
                .add("type_specific", typeSpecificFields)
                .build();
        return jsonObject;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptionNoSnippet() {
        return descriptionNoSnippet;
    }

    public void setDescriptionNoSnippet(String descriptionNoSnippet) {
        this.descriptionNoSnippet = descriptionNoSnippet;
    }

    public List<Highlight> getHighlightsAsListOrig() {
        return highlightsAsList;
    }

    public List<Highlight> getHighlightsAsList() {
        List<Highlight> filtered = new ArrayList<>();
        for (Highlight highlight : highlightsAsList) {
            String field = highlight.getSolrField().getNameSearchable();
            /**
             * @todo don't hard code "title" here. And should we collapse name
             * and title together anyway?
             */
            if (!field.equals(SearchFields.NAME)
                    && !field.equals(SearchFields.DESCRIPTION)
                    && !field.equals(SearchFields.AFFILIATION)
                    && !field.equals("title")) {
                filtered.add(highlight);
            }
        }
        return filtered;
    }

    public void setHighlightsAsList(List<Highlight> highlightsAsList) {
        this.highlightsAsList = highlightsAsList;
    }

    public Map<String, String> getParent() {
        return parent;
    }

    public void setParent(Map<String, String> parent) {
        this.parent = parent;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public String getDataverseAffiliation() {
        return dataverseAffiliation;
    }

    public void setDataverseAffiliation(String dataverseAffiliation) {
        this.dataverseAffiliation = dataverseAffiliation;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public String getNameSort() {
        return nameSort;
    }

    public void setNameSort(String nameSort) {
        this.nameSort = nameSort;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    public Date getReleaseOrCreateDate() {
        return releaseOrCreateDate;
    }

    public void setReleaseOrCreateDate(Date releaseOrCreateDate) {
        this.releaseOrCreateDate = releaseOrCreateDate;
    }
}
