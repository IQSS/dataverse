package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.ArrayList;
import java.util.Date;
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
    private String nameSort;
    private String status;
    private Date releaseOrCreateDate;

    /**
     * @todo: how important is it to differentiate between name and title?
     */
    private String title;
    private String descriptionNoSnippet;
    private String highlightField01;
    private String highlightField02;

    public String getHighlightField01() {
        return highlightField01;
    }

    public void setHighlightField01(String highlightField01) {
        this.highlightField01 = highlightField01;
    }

    public String getHighlightField02() {
        return highlightField02;
    }

    public void setHighlightField02(String highlightField02) {
        this.highlightField02 = highlightField02;
    }

    private List<String> highlightSnippets01;

    public List<String> getHighlightSnippets02() {
        return highlightSnippets02;
    }

    public void setHighlightSnippets02(List<String> highlightSnippets02) {
        this.highlightSnippets02 = highlightSnippets02;
    }
    private List<String> highlightSnippets02;
    // parent can be dataverse or dataset, store the name and id
    private Map<String, String> parent;
    // used on the SearchPage but not the search API
    private List<Dataset> datasets;
    private String dataverseAffiliation;
    private String citation;
    private String filetype;
    private List<String> matchedFields;

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }

    /**
     * @todo: remove name?
     */
    SolrSearchResult(String queryFromUser, List<String> highlightSnippets01, String name) {
        this.query = queryFromUser;
//        this.name = name;
        this.highlightSnippets01 = highlightSnippets01;
    }

    @Override
    public String toString() {
        if (this.name != null) {
            return this.id + ":" + this.name + ":" + this.entityId;
        } else {
            return this.id + ":" + this.title + ":" + this.entityId;
        }
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

    public List<String> getHighlightSnippets01() {
        return highlightSnippets01;
    }

    public void setHighlightSnippets01(List<String> highlightSnippets01) {
        this.highlightSnippets01 = highlightSnippets01;
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
