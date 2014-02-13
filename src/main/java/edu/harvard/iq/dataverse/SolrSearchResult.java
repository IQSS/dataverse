package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class SolrSearchResult {

    private String id;
    private Long entityId;
    private String type;
    private String query;
    private String name;
    /**
     * @todo: how important is it to differentiate between name and title?
     */
    private String title;
    private String descriptionNoSnippet;
    private List<String> highlightSnippets;
    // parent can be dataverse or dataset, store the name and id
    private Map<String, String> parent;
    // used on the SearchPage but not the search API
    private List<Dataset> datasets;
    private String affiliation;
    private String citation;
    private String filetype;

    /**
     * @todo: remove name?
     */
    SolrSearchResult(String queryFromUser, List<String> highlightSnippets, String name) {
        this.query = queryFromUser;
//        this.name = name;
        this.highlightSnippets = highlightSnippets;
    }

    @Override
    public String toString() {
        return this.id + ":" + this.name + ":" + this.entityId;
    }

    public JsonObject toJsonObject() {
        JsonObjectBuilder parentBuilder = Json.createObjectBuilder();
        for (String key : parent.keySet()) {
            parentBuilder.add(key, parent.get(key) != null ? parent.get(key) : "NONE-ROOT-DATAVERSE-OR-EMPTY-STRING-DATASET-TITLE");
        }
        JsonObjectBuilder typeSpecificFields = Json.createObjectBuilder();
        if (this.type.equals("dataverses")) {
            typeSpecificFields.add(SearchFields.NAME, this.name);
//            typeSpecificFields.add(SearchFields.AFFILIATION, affiliation);
        } else if (this.type.equals("datasets")) {
            typeSpecificFields.add(SearchFields.TITLE, this.title);
        } else if (this.type.equals("files")) {
            typeSpecificFields.add(SearchFields.NAME, this.name);
            typeSpecificFields.add(SearchFields.FILE_TYPE, this.filetype);
        }
        JsonObject jsonObject = Json.createObjectBuilder()
                .add(SearchFields.ID, this.id)
                .add(SearchFields.ENTITY_ID, this.entityId)
                .add(SearchFields.TYPE, this.type)
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

    public List<String> getHighlightSnippets() {
        return highlightSnippets;
    }

    public void setHighlightSnippets(List<String> highlightSnippets) {
        this.highlightSnippets = highlightSnippets;
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

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
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

}
