package edu.harvard.iq.dataverse;

import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;

public class SolrSearchResult {

    private String id;
    private Long entityid;
    private String type;
    private String query;
    private String name;
    private List<String> highlightSnippets;

    SolrSearchResult(String queryFromUser, List<String> highlightSnippets, String name) {
        this.query = queryFromUser;
        this.name = name;
        this.highlightSnippets = highlightSnippets;
    }

    @Override
    public String toString() {
        return this.id + ":" + this.name + ":" + this.entityid;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("id", this.id)
                .add("name", this.name)
                .add("entityid", this.entityid)
                .build();
        return jsonObject;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getEntityid() {
        return entityid;
    }

    public void setEntityid(Long entityid) {
        this.entityid = entityid;
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

    public List<String> getHighlightSnippets() {
        return highlightSnippets;
    }

    public void setHighlightSnippets(List<String> highlightSnippets) {
        this.highlightSnippets = highlightSnippets;
    }

}
