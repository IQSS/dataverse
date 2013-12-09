package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;

public class SolrSearchResult {

    private String id;
    private Long entityId;
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
        return this.id + ":" + this.name + ":" + this.entityId;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add(SearchFields.ID, this.id)
                /**
                 * @todo datasets have titles not names
                 */
                .add(SearchFields.NAME, this.name)
                .add(SearchFields.ENTITY_ID, this.entityId)
                .add(SearchFields.TYPE, this.type)
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

    public List<String> getHighlightSnippets() {
        return highlightSnippets;
    }

    public void setHighlightSnippets(List<String> highlightSnippets) {
        this.highlightSnippets = highlightSnippets;
    }

}
